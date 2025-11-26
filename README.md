# Spring Boot User Authentication Service

This is a simple Spring Boot application that implements a user authentication service using Spring Security and JWT (JSON Web Tokens). This application includes full CRUD functionality, custom query filters, input validation for username, emails, and XSS/SQL injection, pagination, API spring security, rate limiting, and more. This porject is intended to demonstrate competencies in key areas for entry level Java developer roles.

## Getting Started

To ensure you have the required build tools, please install the following packages:

**OpenJDK Java 25 Linux**
- x64 Compressed Archive: https://download.oracle.com/java/25/latest/jdk-25_linux-x64_bin.tar.gz (SHA256)
- x64 Debian Package: https://download.oracle.com/java/25/latest/jdk-25_linux-x64_bin.deb (SHA256)

**OpenJDK Java 25 MacOS**
- x64 Compressed Archive: https://download.oracle.com/java/25/latest/jdk-25_macos-x64_bin.tar.gz (SHA256)
- x64 DMG Installer: https://download.oracle.com/java/25/latest/jdk-25_macos-x64_bin.dmg (SHA256)

**OpenJDK Java 25 Windows 10/11**
- x64 Installer: https://download.oracle.com/java/25/latest/jdk-25_windows-x64_bin.exe (SHA256)
- x64 MSI Installer: https://download.oracle.com/java/25/latest/jdk-25_windows-x64_bin.msi (SHA256)

**Apache Maven Project Linux**
- https://dlcdn.apache.org/maven/mvnd/1.0.3/maven-mvnd-1.0.3-linux-amd64.tar.gz (SHA512, asc)

**Apache Maven Project MacOS**
- https://dlcdn.apache.org/maven/mvnd/1.0.3/maven-mvnd-1.0.3-darwin-amd64.zip (SHA512, asc)

**Apache Maven Project Windows 10/11**
- https://dlcdn.apache.org/maven/mvnd/1.0.3/maven-mvnd-1.0.3-windows-amd64.zip (SHA512, asc)

### Installation Guide

1. Clone the repository:

```bash
git clone https://github.com/nexustech101/spring-boot-user-auth.git
```

2. Navigate into the project directory:

```bash
cd spring-boot-user-auth
```

3. Build the project using Maven:

```bash
mvn clean install -DskipTests
```

### Running the Application

You can run the application using Maven:

```bash
mvn spring-boot:run
```

The application will start at `http://localhost:8080`.

## Usage

Once the application is running, you can interact with it using any HTTP client such as cURL or Postman. Below are some example requests:

---

## API Endpoints

### Authentication & Session

- **POST `/api/v1/users/signup`** — Register a new user
  - Request body: `{ "username": "alice", "password": "Passw0rd!", "email": "alice@example.com" }`
  - Validates unique username/email, password length, and input safety.
  - Returns: Created user (no password).

- **POST `/api/v1/users/signin`** — Authenticate user
  - Request body: `{ "username": "alice", "password": "Passw0rd!" }`
  - Enforces rate limit (3 attempts/10min per user).
  - On success: Sets JSESSIONID cookie, returns user info.
  - On failure: `401 Unauthorized` or `429 Too Many Requests`.

- **POST `/api/v1/users/signout`** — Logout (invalidate session)
  - Requires active session (JSESSIONID cookie).
  - Returns: `204 No Content`.

- **GET `/api/v1/users/me`** — Get current authenticated user
  - Requires active session.
  - Returns: User info or `401 Unauthorized`.

### User Management

- **GET `/api/v1/users`** — List users (paginated)
  - Query params: `page`, `size`, `sortBy` (default: `id`)
  - Returns: Page of users (no passwords).

- **GET `/api/v1/users/{id}`** — Get user by ID
  - Returns: User info or `404 Not Found`.

- **GET `/api/v1/users/username/{username}`** — Get user by username
  - Returns: User info or `404 Not Found`.

- **GET `/api/v1/users/email/{email}`** — Get user by email
  - Returns: User info or `404 Not Found`.

- **GET `/api/v1/users/search/{name}`** — Search users by username (paginated)
  - Query params: `page`, `size`, `sortBy`
  - Returns: Page of users matching pattern.

- **PUT `/api/v1/users/{id}/email?newEmail=...`** — Update user email
  - Requires valid email, unique.
  - Returns: Updated user info.

- **PUT `/api/v1/users/{id}/password?newPassword=...`** — Update user password
  - Requires password (min 8 chars).
  - Returns: Updated user info.

- **DELETE `/api/v1/users/{id}`** — Delete user
  - Returns: `204 No Content`.

---

## Security & Validation

- **Session Management**: All protected endpoints require a valid JSESSIONID cookie.
- **Rate Limiting**: Signin attempts limited to 3 per 10 minutes per user (see `RateLimiterService`).
- **Input Sanitization**: All user input is sanitized (see `InputSanitizer`).
- **Custom Validation**: Unique username/email enforced at DTO level (`@UniqueUsername`, `@UniqueEmail`).
- **Password Policy**: Minimum 8 characters enforced.

## Error Handling

All errors return a structured JSON response:

```json
{
  "timestamp": "2024-06-01T12:34:56",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid username format",
  "path": "/api/v1/users/signup"
}
```

Common error codes:
- `400 Bad Request`: Invalid input, validation errors
- `401 Unauthorized`: Invalid credentials or missing session
- `404 Not Found`: Resource not found
- `409 Conflict`: Duplicate username/email
- `429 Too Many Requests`: Rate limit exceeded
- `500 Internal Server Error`: Unexpected error

## Swagger / OpenAPI Documentation

- Interactive API docs: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- OpenAPI spec: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
- All endpoints and models are documented and testable via Swagger UI.

## Database Configuration

- **Development**: Uses H2 in-memory database by default.
- **Production**: Configure PostgreSQL in `application.properties`.

## Testing

- Unit and integration tests in `src/test/java/com/example/auth/`
- Run tests: `mvn test`

## Built With

- Spring Boot - Framework for creating Spring applications
- Spring Web - Build web, including RESTful, applications using Spring MVC
- Spring Data JPA - Simplifies data access for relational databases
- Spring Security - Authentication and access control framework
- PostgreSQL - Open source object-relational database system
- Project Lombok - Java library that helps to reduce boilerplate code
- Auth0 Java JWT - Library for working with JSON Web Tokens (JWT) in Java
- Flyway - Database migration tool for java
- Springdoc OpenAPI - Swagger UI and OpenAPI documentation
- Google Guava - Rate limiting and caching

## Contributing

Pull requests are welcome! For major changes, please open an issue first to discuss what you would like to change.

## License

This project is licensed under the Apache 2.0 License.

### Register a User

Send a POST request to `/api/v1/users/signup` with a JSON body containing the user details:

```json
{
  "username": "alice",
  "password": "Passw0rd!",
  "email": "alice@example.com"
}
```

### Authenticate User

Send a POST request to `/api/v1/users/signin` with the user credentials:

```json
{
  "username": "alice",
  "password": "Passw0rd!"
}
```

### Check Cuurent User Session

Send a GET request to `/api/v1/users/me` to fetch current user session: no body is required if session token is stored.


### Access Protected Resource

To access a protected resource, include the JWT token in the `Authorization` header of your requests:

```makefile
Authorization: Bearer <JWT token>
```
### Built With

- Spring Boot - Framework for creating Spring applications
- Spring Web - Build web, including RESTful, applications using Spring MVC
- Spring Data JPA - Simplifies data access for relational databases
- Spring Security - Authentication and access control framework
- PostgreSQL - Open source object-relational database system
- Project Lombok - Java library that helps to reduce boilerplate code
- Auth0 Java JWT - Library for working with JSON Web Tokens (JWT) in Java
- Flyway - Database migration tool for java

### Author

- cdefre3@wgu.edu
