# AI-Powered Clinic Portal — Backend

Backend cho hệ thống quản lý phòng khám, xây dựng bằng **Spring Boot**, cung cấp REST API cho quản lý bệnh nhân, bác sĩ, lịch hẹn, hồ sơ khám và thanh toán. Hệ thống tích hợp trợ lý AI để hỏi đáp thông tin phòng khám, phân tích triệu chứng và tóm tắt hồ sơ bệnh án, cùng kênh chat thời gian thực giữa người dùng.

Repository này chứa API backend. Frontend Next.js được phát triển trong repository riêng và kết nối qua HTTP API, Server-Sent Events (SSE) và WebSocket.

## Chức năng chính

| Nhóm chức năng | Nội dung |
| --- | --- |
| Tài khoản và xác thực | Đăng ký, đăng nhập bằng username/email, JWT access token, làm mới và thu hồi refresh token, mã hóa mật khẩu BCrypt. |
| Người dùng và phân quyền | Quản lý người dùng; các vai trò `ADMIN`, `DOCTOR`, `RECEPTIONIST`, `NURSE`, `PATIENT` (trong mã nguồn có tiền tố `ROLE_`). Quyền cụ thể phụ thuộc từng API. |
| Bệnh nhân và định danh | Quản lý thông tin cá nhân, hồ sơ bệnh nhân, tra cứu theo căn cước; mô hình `Person` dùng chung cho thông tin định danh. |
| Bác sĩ | Quản lý hồ sơ, chuyên khoa, lịch làm việc, ngày nghỉ, khung giờ còn trống và đánh giá bác sĩ. |
| Lịch hẹn | Tạo, tìm kiếm, lọc, cập nhật trạng thái và hủy lịch hẹn; kiểm soát trùng khung giờ bác sĩ. |
| Hồ sơ khám | Quản lý bệnh án, đơn thuốc và chi tiết thuốc; tiếp nhận lượt khám (`Visit`), ghi nhận phiên khám (`Encounter`) và dữ liệu lâm sàng (`Obs`). |
| Từ điển lâm sàng | Quản lý dữ liệu khám theo `Concept`: kiểu dữ liệu, đơn vị, giới hạn và danh sách lựa chọn; xem lịch sử khám và diễn biến chỉ số bệnh nhân. |
| Thanh toán | Tạo giao dịch VNPay, xử lý kết quả trả về và thông báo IPN. |
| Trợ lý AI | Chat và SSE, tìm kiếm tri thức phòng khám bằng pgvector (RAG), phân tích triệu chứng, tóm tắt bệnh án; agent hỗ trợ tìm bác sĩ, tạo và hủy lịch hẹn. |
| Chat thời gian thực | Phòng chat, lịch sử tin nhắn phân trang, đánh dấu đã đọc, gửi qua STOMP/WebSocket hoặc REST. |
| Dược phẩm | Tra cứu sản phẩm/dược phẩm, tạo và cập nhật danh mục. |

## Công nghệ

| Thành phần | Công nghệ trong repository |
| --- | --- |
| Ngôn ngữ và build | Java, Maven Wrapper; JDK 25 trong Docker và CI, compiler target Java 21. |
| Backend | Spring Boot 3.5.16, Spring Web, Spring Validation. |
| Truy cập dữ liệu | Spring Data JPA, Hibernate, PostgreSQL 14 + pgvector. |
| Migration | Flyway; Hibernate dùng `ddl-auto=validate`. |
| Bảo mật | Spring Security, JJWT, BCrypt, kiểm tra quyền ở tầng API và giới hạn tần suất yêu cầu. |
| Cache | Spring Cache, Redis 7. |
| Thời gian thực | WebSocket, STOMP, SockJS, PostgreSQL LISTEN/NOTIFY cho chat; SSE cho phản hồi AI. |
| AI | Google Gemini, OpenAI; phương án dự phòng dựa trên luật và embedding TF-IDF. |
| Tài liệu và giám sát | SpringDoc OpenAPI/Swagger UI, Spring Boot Actuator health. |
| Kiểm thử và triển khai | JUnit 5, Mockito, Spring Boot Test, Docker Compose, GitHub Actions, GHCR, EC2. |

## Kiến trúc và cấu trúc mã nguồn

Ứng dụng tổ chức theo module nghiệp vụ. Luồng xử lý thông thường đi từ `Controller → Service → Repository → PostgreSQL`; DTO và mapper dùng để chuyển đổi dữ liệu ở biên API.

```text
src/main/java/com/thphatts/clinicportal/
├── ClinicPortalApplication.java
├── annotation/          # Annotation và validator dùng chung
├── common/              # ApiResponse, phân trang, tiện ích thanh toán
├── config/              # Security, WebSocket, Swagger, cache, dữ liệu dev
├── exception/           # Ngoại lệ và xử lý lỗi tập trung
└── modules/
    ├── ai/              # LLM, embedding, RAG, bộ nhớ hội thoại, agent
    ├── appointment/     # Lịch hẹn và trạng thái khám
    ├── chat/            # Phòng chat và tin nhắn
    ├── concept/         # Từ điển dữ liệu lâm sàng
    ├── doctor/          # Bác sĩ, lịch làm việc, đánh giá
    ├── patient/         # Bệnh nhân, bệnh án, đơn thuốc, Visit/Encounter/Obs
    ├── payment/         # Giao dịch và tích hợp VNPay
    ├── person/          # Thông tin định danh dùng chung
    ├── product/         # Dược phẩm và danh mục
    └── user/            # Người dùng, đăng nhập, refresh token

src/main/resources/
├── application.properties
└── db/migration/        # Migration Flyway
src/test/java/           # Kiểm thử backend
.github/workflows/       # Pipeline CI/CD
deploy/                  # Cấu hình và script triển khai EC2
scripts/                 # Kiểm tra migration, smoke test và kiểm thử deploy
docs/                    # Tài liệu triển khai và bảo mật
```

## Chạy local

Thực hiện các lệnh bên dưới từ **thư mục gốc repository backend** (`be/` nếu đang ở workspace chung).

### 1. Chuẩn bị

- JDK 25, theo môi trường Docker và CI của dự án.
- Docker Engine/Docker Desktop và Docker Compose v2.
- Không cần cài Maven riêng vì repository có `mvnw`.

### 2. Khởi động PostgreSQL và Redis

```sh
cp .env.example .env
```

Sửa các giá trị mẫu trong `.env`, đặc biệt `POSTGRES_PASSWORD` và `DB_PASSWORD` phải khớp nhau khi dùng database local. Sau đó chạy:

```sh
docker compose up -d postgres_db redis
```

Theo cấu hình mặc định, PostgreSQL được mở tại `localhost:5434`, Redis tại `localhost:6379`. Compose còn khai báo MySQL nhưng ứng dụng hiện dùng PostgreSQL; hai service trên là các service cần cho hướng dẫn này.

### 3. Cấu hình và chạy API

Docker Compose tự đọc `.env`; khi chạy Maven trực tiếp, Spring Boot **không tự nạp file này**. Export biến trong terminal chạy API hoặc cấu hình Environment Variables trong IDE:

```sh
export DB_HOST=localhost
export DB_PORT=5434
export DB_NAME=my_postgres_db
export DB_USERNAME=admin
export DB_PASSWORD='your_secure_postgres_password'
export JWT_SECRET="$(openssl rand -base64 48)"
export REDIS_HOST=localhost
export REDIS_PORT=6379
export FRONTEND_URL=http://localhost:3000
export ENABLE_SWAGGER=true

./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Thay giá trị database bằng thông tin đã đặt trong `.env`. `JWT_SECRET` phải là chuỗi Base64 giải mã được ít nhất 32 byte; lệnh trên sinh khóa dùng cho local. Giữ một khóa cố định trong cấu hình môi trường nếu cần token còn hợp lệ sau khi khởi động lại.

Flyway chạy migration khi ứng dụng khởi động, sau đó Hibernate kiểm tra schema. Database phải hỗ trợ extension `vector`; image `pgvector/pgvector:pg14` trong Compose đã cung cấp extension này.

| Địa chỉ local | Mục đích |
| --- | --- |
| `http://localhost:8080` | Địa chỉ API mặc định. |
| `http://localhost:8080/actuator/health` | Kiểm tra trạng thái ứng dụng và các kết nối phụ thuộc. |
| `http://localhost:8080/swagger-ui/index.html` | Swagger UI khi `ENABLE_SWAGGER=true`. |
| `http://localhost:8080/v3/api-docs` | OpenAPI JSON khi `ENABLE_SWAGGER=true`. |

### 4. Tài khoản phát triển

Profile `dev` tạo các tài khoản mẫu còn thiếu: `admin`, `doctor`, `receptionist`, `patient`, với mật khẩu khởi tạo `password123`. Mật khẩu tài khoản đã tồn tại được giữ nguyên; các tài khoản mẫu không bảo đảm đã có hồ sơ bác sĩ/bệnh nhân liên kết hoặc lịch làm việc.

Xem [hướng dẫn phát triển local](LOCAL-DEVELOPMENT.md) để biết cách đặt lại mật khẩu mẫu. Không bật profile `dev` trên production.

## Biến môi trường

Danh sách dưới đây dựa trên `application.properties` và các provider trong mã nguồn. `.env.example` chứa cấu hình cơ bản; bổ sung các biến tích hợp khi cần dùng AI hoặc thanh toán.

| Biến | Ý nghĩa / mặc định |
| --- | --- |
| `PORT` | Cổng HTTP, mặc định `8080`. |
| `SPRING_DATASOURCE_URL` | JDBC URL đầy đủ; nếu đặt sẽ thay URL ghép từ `DB_HOST`, `DB_PORT`, `DB_NAME`. |
| `DB_HOST`, `DB_PORT`, `DB_NAME` | Database chạy trực tiếp: `localhost`, `5434`, `my_postgres_db`. |
| `DB_USERNAME`, `DB_PASSWORD` | Thông tin xác thực của ứng dụng với PostgreSQL. |
| `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DB`, `POSTGRES_PORT` | Cấu hình container PostgreSQL trong Compose local. |
| `JWT_SECRET` | Khóa ký JWT dạng Base64; cấu hình khóa riêng cho từng môi trường. |
| `JWT_EXPIRATION_MS` | Thời hạn access token, mặc định `86400000` ms (24 giờ). |
| `FRONTEND_URL` | Origin frontend cho CORS, mặc định `http://localhost:3000`. |
| `REDIS_HOST`, `REDIS_PORT` | Địa chỉ Redis, mặc định `localhost:6379`. |
| `REDIS_PASSWORD`, `REDIS_SSL` | Mật khẩu Redis và bật TLS; mặc định rỗng / `false`. |
| `CACHE_TYPE` | Loại cache của Spring, mặc định `redis`. |
| `ENABLE_SWAGGER` | Bật OpenAPI và Swagger UI, mặc định `false`. |
| `SHOW_SQL` | In SQL Hibernate, mặc định `false`. |
| `GEMINI_API_KEY` | Khóa Gemini cho LLM và embedding, mặc định rỗng. |
| `APP_AI_OPENAI_API_KEY` | Binding đến `app.ai.openai.api-key` cho OpenAI provider. |
| `VNPAY_TMN_CODE`, `VNPAY_SECRET_KEY` | Thông tin merchant VNPay; thay giá trị demo khi tích hợp thanh toán. |
| `VNPAY_PAY_URL` | URL thanh toán, mặc định sandbox VNPay. |
| `VNPAY_RETURN_URL` | Callback return, mặc định `http://localhost:8080/api/v1/payments/vnpay-return`. |

### Cách chọn AI provider

Mặc định `app.ai.active-provider=auto`: ứng dụng ưu tiên Gemini, tiếp đến OpenAI nếu có khóa cấu hình, và dùng bộ luật dự phòng khi không có provider khả dụng. Embedding ưu tiên Gemini và có TF-IDF dự phòng; tri thức được lưu trong PostgreSQL/pgvector để truy xuất nội dung liên quan khi chat.

Có thể chọn bằng Spring Boot arguments, ví dụ:

```sh
./mvnw spring-boot:run \
  -Dspring-boot.run.profiles=dev \
  -Dspring-boot.run.arguments="--app.ai.active-provider=openai --app.ai.embedding.provider=tfidf"
```

Ví dụ này cần `APP_AI_OPENAI_API_KEY` trong môi trường. Chế độ dự phòng dựa trên luật không có đầy đủ khả năng của LLM; các tích hợp online còn phụ thuộc khóa, quota và model mà nhà cung cấp hỗ trợ.

## Sử dụng API

Đa số REST API dùng tiền tố `/api/v1`.

| Đường dẫn | Nhóm nghiệp vụ |
| --- | --- |
| `/api/v1/auth` | `register`, `login`, `refresh`, `logout` (POST). |
| `/api/v1/users` | Quản lý người dùng. |
| `/api/v1/persons` | Tra cứu định danh, tạo hồ sơ bệnh nhân từ Person. |
| `/api/v1/patients` | Hồ sơ bệnh nhân; lịch sử lượt khám và chỉ số theo bệnh nhân. |
| `/api/v1/doctors` | Hồ sơ, chuyên khoa, lịch làm việc, ngày nghỉ và khung giờ trống. |
| `/api/v1/doctor-reviews` | Đánh giá bác sĩ. |
| `/api/v1/appointments` | Lịch hẹn và cập nhật trạng thái. |
| `/api/v1/medical-records` | Bệnh án, truy vấn theo lịch hẹn, bệnh nhân hoặc bác sĩ. |
| `/api/v1/visits` | Tiếp nhận, tạo encounter và đóng lượt khám. |
| `/api/v1/concepts/dictionary` | Từ điển lâm sàng. |
| `/api/v1/payments` | Tạo thanh toán, callback return và IPN của VNPay. |
| `/api/v1/ai` | Chat, stream, agent action, phân tích triệu chứng, tóm tắt hồ sơ. |
| `/api/v1/chat/rooms` | Phòng chat, tin nhắn và trạng thái đã đọc. |
| `/api/v1/products`, `/api/v1/categories` | Dược phẩm và danh mục. |

Xem Swagger để biết HTTP method, DTO, tham số và schema phản hồi của từng endpoint.

Ví dụ đăng nhập bằng tài khoản dev:

```sh
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"patient","password":"password123"}'
```

Access token nằm trong `result.token` của phản hồi đăng nhập. Gửi token với các API cần xác thực:

```http
Authorization: Bearer <access_token>
```

Refresh token được gửi bằng cookie `refresh_token` có `HttpOnly`, `Secure`, `SameSite=None`, giới hạn đường dẫn `/api/v1/auth`. Khi tích hợp trình duyệt, gửi kèm credentials và dùng HTTPS cho luồng refresh cookie.

Nhiều REST endpoint trả về cấu trúc chung `ApiResponse<T>`:

```json
{
  "code": 200,
  "message": "Success",
  "result": {}
}
```

Các endpoint AI có DTO phản hồi riêng; endpoint stream trả về `text/event-stream`.

### WebSocket chat

- Kết nối SockJS tại `/ws`.
- Gửi JWT trong header `Authorization: Bearer <access_token>` của STOMP `CONNECT`.
- Gửi tin nhắn đến `/app/chat.send`.
- Nhận tin nhắn phòng tại `/topic/chat.{roomId}`.
- Dùng REST `/api/v1/chat/rooms/{roomId}/messages` để đọc lịch sử hoặc gửi khi WebSocket không khả dụng.

## Kiểm thử và đóng gói

```sh
# Chạy bộ kiểm thử backend
./mvnw test

# Chạy riêng kiểm thử một service
./mvnw -Dtest=DoctorServiceTest test

# Kiểm thử và đóng gói executable JAR
./mvnw clean package

# Chạy JAR với biến môi trường đã cấu hình
java -jar target/clinicportal-0.0.1-SNAPSHOT.jar
```

Các kiểm thử hiện có bao gồm xác thực/refresh token, bác sĩ, lịch hẹn, bệnh án, thanh toán, chat, AI và security wiring. Một số kiểm thử tích hợp cần dịch vụ bên ngoài; `ClinicPortalApplicationTests` hiện được đánh dấu `@Disabled`, nên kết quả unit test không đồng nghĩa đã kiểm tra toàn bộ ứng dụng với database thật.

## CI/CD và triển khai

Workflow [backend-ci.yml](.github/workflows/backend-ci.yml) chạy kiểm tra source, compile, tests, đóng gói, smoke test container, quét image và phân tích bảo mật. Push `main` mới publish image lên GHCR và triển khai EC2 sau khi các bước kiểm tra thành công.

- [Hướng dẫn CI/CD và EC2](docs/EC2-DEPLOY.md): cấu hình secrets, TLS, health check, backup và rollback.
- [Cấu hình production mẫu](deploy/production.env.example).
- [Docker Compose production](deploy/docker-compose.ec2.yml).
- [Tài liệu quét bảo mật](docs/SECURITY-SCAN.md).

## Quy ước phát triển

- Giữ controller, service, repository, DTO và mapper trong module nghiệp vụ tương ứng.
- Thêm migration theo mẫu `V<number>__description.sql`; không sửa migration đã áp dụng.
- Không commit `.env`, khóa API, thông tin xác thực hoặc dữ liệu bệnh nhân.
- Dùng Conventional Commits, ví dụ `feat:`, `fix:`, `refactor:`, `chore:`.
