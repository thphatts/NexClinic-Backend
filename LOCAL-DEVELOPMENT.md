# Local development

Start PostgreSQL and Redis from this directory:

```sh
docker compose up -d postgres_db redis
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

The `dev` profile creates missing demo accounts individually:
`admin`, `doctor`, `receptionist`, and `patient`, with initial password
`password123`. Existing account passwords are preserved. Without `dev`, no
demo accounts are seeded.

To explicitly restore demo passwords on a local database, stop the running
backend and run once:

```sh
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev -Dspring-boot.run.arguments=--app.dev.reset-demo-passwords=true
```

Only accounts whose username, email (`<username>@clinic.com`) and role match
the demo definition are reset. Other accounts are left unchanged. Use the
normal command on subsequent starts. Check the startup log for account creation,
preservation, reset or email collision messages.

These credentials describe the development seed; they do not guarantee an
existing database has the same passwords. A successful login does not guarantee
the demo user has a linked doctor/patient profile or a configured work schedule.
