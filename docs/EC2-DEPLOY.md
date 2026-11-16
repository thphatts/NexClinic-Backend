# Backend CI/CD và EC2

Workflow nằm trong **repo NexClinic-Backend**, các đường dẫn tính từ root backend (không có prefix `be/`). Push mọi nhánh và PR chạy CI; chỉ push `main` được publish/deploy; `workflow_dispatch` chạy kiểm tra, không deploy thủ công một nhánh bất kỳ.

## Các jobs và điều kiện chặn

| Job | Nội dung |
| --- | --- |
| `pipeline-checks` | actionlint, ShellCheck, cú pháp Bash, 3 test script deploy/rollback, bảo vệ migration đã có trên base commit |
| `source-validation` | Maven validate/compile, conflict markers và whitespace |
| `tests` | Toàn bộ JUnit/Mockito/security tests; cung cấp PostgreSQL pgvector + Redis cho test cần DB; tải báo cáo Surefire kể cả khi lỗi |
| `package` | Đóng gói JAR khi test xanh, lưu artifact |
| `container-smoke` | Build Docker, DB rỗng riêng biệt, Flyway strict + Hibernate validate lúc startup, health, public/protected routes, register/login/profile với dữ liệu giả; restart cùng DB để kiểm tra startup lần 2 |
| `image-scan` | Trivy CLI quét OS/JAR trong chính image vừa test, chặn HIGH/CRITICAL có bản vá; lưu báo cáo artifact |
| `codeql` | Phân tích bảo mật Java; là gate trước publish |
| `publish` | Push chính image đã test và scan lên `ghcr.io/<owner>/<repo>:<full-commit-sha>`, xuất digest |
| `deploy` | SSH xác thực host key, dùng digest, khóa chống deploy đồng thời, Docker Compose chờ healthy, rollback image khi lỗi |

Không dùng `continue-on-error` để bỏ qua test. Không cần CI secrets cho PR fork: JWT key CI sinh ngẫu nhiên theo job và chỉ dùng trong DB tạm. Các test đang `@Disabled` không được tự động coi là integration coverage; smoke container kiểm tra startup/migration thật nhưng không thay thế test đầy đủ mọi nghiệp vụ.

CodeQL trên repo private cần GitHub Code Security được bật; nếu chưa có, bật tính năng hoặc chọn scanner tương đương trước khi đưa pipeline vào dùng. CI runner và EC2 mặc định x86_64/amd64; chưa cấu hình Graviton/arm64.

Trivy CLI được tải từ GitHub release chính thức, ghim version và kiểm SHA-256 trước khi chạy. Không dùng composite action `trivy-action`/`setup-trivy`. Khi nâng cấp, cập nhật cùng lúc `TRIVY_VERSION` và `TRIVY_SHA256` trong workflow theo checksum chính thức.

## Chuẩn bị EC2 một lần

Dùng Ubuntu 24.04 x86_64, có dung lượng cho PostgreSQL, image cũ/mới và bản backup. Docker và Compose v2 phải có trên EC2. Cài Docker Engine + Compose plugin theo tài liệu chính thức https://docs.docker.com/engine/install/ubuntu/ (gói `docker.io` từ Ubuntu không luôn đi kèm `docker-compose-plugin`). Script cần `bash`, `flock` (util-linux), `docker compose` và `curl`.

Kiểm tra:

```sh
docker --version
sudo docker compose version
command -v flock
sudo install -d -m 0750 /opt/nexclinic
```

Đặt bản sao của `deploy/production.env.example` tại `/opt/nexclinic/.env`, sửa mọi giá trị mẫu. File này là định dạng Docker Compose env; script **không source/execute** nội dung của nó. JWT key phải là Base64, có thể tạo bằng `openssl rand -base64 48 | tr -d '\n'`.

```sh
sudo chmod 600 /opt/nexclinic/.env
```

Nếu deploy báo `required variable ... is missing a value`, sửa file **trên EC2** bằng `sudo nano /opt/nexclinic/.env`, đối chiếu tên biến chính xác với `deploy/production.env.example`. Compose này cần `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `JWT_SECRET`, `FRONTEND_URL`, `VNPAY_TMN_CODE`, `VNPAY_SECRET_KEY`, `VNPAY_PAY_URL`, `VNPAY_RETURN_URL`. Các tên như `DB_USERNAME`/`DB_PASSWORD` không thay thế `POSTGRES_USER`/`POSTGRES_PASSWORD` trong file này. Giữ nguyên thông tin đăng nhập của database đã khởi tạo; không đổi ngẫu nhiên mật khẩu để thử sửa lỗi.

Kiểm tra mà không in giá trị secrets (sau khi script đã tạo `release.env`):

```sh
sudo docker compose --project-name nexclinic --env-file /opt/nexclinic/.env --env-file /opt/nexclinic/release.env -f /opt/nexclinic/docker-compose.ec2.yml config --quiet
```

Không dùng `source .env` hay đăng nội dung `.env` vào log/chat. `release.env` chỉ lưu `IMAGE`; các cấu hình production khác nằm trong `.env`. Workflow không tự tạo credentials hay ghi đè `.env` trên EC2.

Không đưa `.env`, PEM, database dump vào Git. Giữ DB/Redis nội bộ trong network Docker, không mở 5432/6379 trong Security Group. Cho SSH từ runner hoặc mạng quản trị được phép; GitHub hosted runner có IP thay đổi, cần giải pháp mạng phù hợp (runner tự quản/VPN nếu không muốn mở SSH công khai). App bind `127.0.0.1:8080` nên truy cập Internet phải qua reverse proxy.

Đây là deployment mới với volumes PostgreSQL/Redis tên project `nexclinic`. **Không tự động dùng hay di chuyển database từ Compose dev `postgres_data`.** Nếu đã có dữ liệu, phải backup và import/điều chỉnh storage trước. Script không cài Docker, provision EC2 hay chuyển dữ liệu.

## Quyền registry và SSH

GitHub publish dùng `GITHUB_TOKEN` quyền `packages:write`. Với GHCR private, đăng nhập trên EC2 một lần dưới cùng user chạy deployment (script được gọi bằng `sudo`, nên đăng nhập root):

```sh
# Nhập read-only PAT có read:packages khi Docker yêu cầu mật khẩu.
sudo docker login ghcr.io -u YOUR_GITHUB_USER
```

Không để PAT trong tham số CLI/workflow. Tài khoản SSH cần quyền `sudo -n` cho các lệnh install và deploy; Docker access tương đương quyền root, chỉ dùng key riêng cho deployment.

GitHub → Settings → Environments → tạo `production`, cho phép branch `main`, tùy nhu cầu bật reviewer. Tạo **environment secrets**:

| Secret | Nội dung |
| --- | --- |
| `EC2_HOST` | IP/domain EC2 |
| `EC2_USER` | Ví dụ `ubuntu` |
| `EC2_SSH_PRIVATE_KEY` | Private key deploy nhiều dòng |
| `EC2_KNOWN_HOSTS` | Dòng known_hosts có key của EC2, xác minh fingerprint qua kênh quản trị trước khi lưu |

Không dùng `StrictHostKeyChecking=no`. Không cần AWS access key vì workflow không gọi AWS API. EC2, DNS, TLS, reverse proxy và secrets chưa được workflow tự tạo.

### Khi production lỗi ở SSH/upload

`Validate production SSH secrets` thành công chỉ xác nhận secrets không rỗng. Bước `Verify SSH connection before upload` kiểm tra kết nối và đăng nhập trước SCP; giới hạn chờ kết nối 15 giây và không chờ nhập mật khẩu tương tác.

| Lỗi SSH trong log | Kiểm tra trên EC2/GitHub |
| --- | --- |
| `Connection timed out` | Public IP trong `EC2_HOST`, route Internet, Security Group/NACL và firewall phải cho TCP 22 từ **runner**. Rule `My IP` chỉ cho máy cá nhân, không cho GitHub hosted runner. Dùng runner có IP cố định, runner tự quản hoặc VPN để giới hạn nguồn truy cập. |
| `Connection refused` | SSH có đang chạy và lắng nghe cổng 22: `sudo systemctl status ssh`, `sudo ss -ltnp 'sport = :22'`. |
| `Permission denied (publickey)` | `EC2_USER` đúng với AMI (Ubuntu thường là `ubuntu`); private key tương ứng public key trong `~/.ssh/authorized_keys` của user này. |
| `Host key verification failed` | `EC2_KNOWN_HOSTS` phải chứa hostname/IP khớp `EC2_HOST` và public host key thật của EC2. Xác minh qua console trước khi cập nhật, không bỏ kiểm tra host key. |
| `Could not resolve hostname` | `EC2_HOST` chỉ chứa IP/hostname, không có `http://`, đường dẫn, cổng hay lệnh SSH. |

Trên EC2 có thể lấy public host key bằng `sudo cat /etc/ssh/ssh_host_ed25519_key.pub`. Ghép hostname/IP phía trước để thành dòng known_hosts. Đây là **host key**, khác với key đăng nhập của `EC2_SSH_PRIVATE_KEY`; không gửi private key vào log hay chat.

Nếu server đang chạy stack `clinic-portal-deploy`, workflow này không cập nhật service `backend` của stack đó: nó tạo project `nexclinic` riêng, app bind host `127.0.0.1:8080`. Proxy đang dùng `proxy_pass http://backend:8080` vẫn phục vụ backend cũ. Cần lập kế hoạch chuyển proxy và dữ liệu trước khi chuyển production; `127.0.0.1` bên trong container proxy không phải host EC2. Không xóa volumes hoặc dừng database cũ để thử sửa lỗi SSH.

## TLS và WebSocket

Cấu hình Nginx/Caddy phục vụ `https://api.example.com`, proxy tới `127.0.0.1:8080`, giữ `Host`, `X-Forwarded-For`, `X-Forwarded-Proto`. `/ws` cần HTTP/1.1, header `Upgrade`/`Connection` và timeout phù hợp. Cấp chứng chỉ TLS trước khi frontend production gọi API; auth refresh cookie hiện cần Secure/HTTPS. `FRONTEND_URL` phải khớp origin FE, `VNPAY_RETURN_URL` trỏ về domain API thật.

## Backup, release và rollback

Trước production release, lưu backup ra ngoài EC2 và thử restore. Ví dụ khi Compose đã chạy (không truyền password qua CLI):

```sh
sudo docker compose --project-name nexclinic --env-file /opt/nexclinic/.env --env-file /opt/nexclinic/release.env -f /opt/nexclinic/docker-compose.ec2.yml exec -T postgres_db sh -c 'pg_dump -U "$POSTGRES_USER" "$POSTGRES_DB"' > clinic-backup.sql
```

Đừng để dump trong repo. Bật branch protection với các CI jobs bắt buộc. Merge/push `main` mới publish và deploy nếu mọi gate xanh. Compose thay container nên có khoảng downtime; không tuyên bố zero downtime.

Image deploy luôn theo digest. App health là `/actuator/health`, kiểm tra DB/Redis. Script rollback khi pull/start/health lỗi và trả exit code lỗi dù rollback thành công, để GitHub đánh dấu release thất bại. Release đầu không có image trước để rollback. Nếu database đã migrate, rollback container **không hoàn tác database**: migration phải tương thích phiên bản trước (expand/contract). Nếu schema không còn tương thích, cần forward fix hoặc restore backup được đánh giá trước; script không tự xóa volume hay restore DB.

Rollback thủ công về digest đã biết:

```sh
sudo /opt/nexclinic/ec2-deploy.sh ghcr.io/OWNER/REPO@sha256:DIGEST
```

OWNER/REPO phải lowercase; DIGEST là 64 ký tự hex thực. Kiểm tra Docker logs và `/actuator/health` sau mỗi release.

## Trạng thái kiểm chứng local

Workflow không thể đảm bảo bắt mọi lỗi nghiệp vụ. Unit suite local hiện chạy 93 tests, 0 failure, 0 error, 1 skipped; workflow vẫn chặn deploy nếu một test thất bại. `actionlint`, Bash/YAML parser, test rollback và migration-name/immutability check có thể chạy local. CI Docker smoke chỉ dùng synthetic data trong DB riêng.
