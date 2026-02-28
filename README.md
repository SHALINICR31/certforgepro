# 🎓 CertForge Pro — Spring Boot + MongoDB

A full-stack Certificate Generation System with login, email via Gmail, and MongoDB history.

---

## 📁 Project Structure

```
certforge/
├── pom.xml
├── README.md
└── src/main/
    ├── java/com/certforge/
    │   ├── CertForgeApplication.java
    │   ├── config/
    │   │   ├── DataInitializer.java      ← Add users here
    │   │   └── SecurityConfig.java
    │   ├── controller/
    │   │   ├── AuthController.java
    │   │   ├── CertificateController.java
    │   │   └── PageController.java
    │   ├── model/
    │   │   ├── User.java
    │   │   └── CertHistory.java
    │   ├── repository/
    │   │   ├── UserRepository.java
    │   │   └── CertHistoryRepository.java
    │   ├── security/
    │   │   ├── JwtUtils.java
    │   │   ├── JwtAuthFilter.java
    │   │   └── UserDetailsServiceImpl.java
    │   └── service/
    │       ├── EmailService.java
    │       └── HistoryService.java
    └── resources/
        ├── application.properties
        └── templates/
            ├── login.html
            └── app.html
```

---

## ⚙️ Prerequisites

| Tool | Version |
|------|---------|
| Java | 17+ |
| Maven | 3.8+ |
| MongoDB | 6.0+ (running locally) |
| Gmail account | With App Password enabled |

---

## 🚀 Setup Steps

### 1. Start MongoDB

```bash
# Linux/Mac
mongod --dbpath /data/db

# Windows
"C:\Program Files\MongoDB\Server\6.0\bin\mongod.exe"

# Or using Docker
docker run -d -p 27017:27017 --name mongo mongo:6
```

### 2. Configure Gmail

You need a **Gmail App Password** (not your regular password):

1. Go to your Google Account → Security
2. Enable **2-Step Verification** (required)
3. Go to Security → **App passwords**
4. Create a new App Password → select "Mail" → copy the 16-char password

Then edit `src/main/resources/application.properties`:
```properties
spring.mail.username=YOUR_GMAIL@gmail.com
spring.mail.password=xxxx xxxx xxxx xxxx    # 16-char App Password (spaces OK)
```

### 3. Add Users (No Registration Allowed)

Edit `src/main/java/com/certforge/config/DataInitializer.java`:

```java
@Override
public void run(String... args) {
    createUserIfNotExists("john",  "john123",  "john@company.com",  "John Doe");
    createUserIfNotExists("priya", "priya123", "priya@company.com", "Priya Kumar");
    // Add more users here ↑
}
```

> ⚠️ Users **cannot self-register**. Only users added here can log in.

### 4. Build & Run

```bash
cd certforge
mvn clean package -DskipTests
mvn spring-boot:run
```

Then open: **http://localhost:8080**

---

## 🔐 Login Credentials (Default)

| Username | Password | Full Name |
|----------|----------|-----------|
| admin    | admin123 | Admin User |
| john     | john123  | John Doe |
| priya    | priya123 | Priya Kumar |

> Change these in `DataInitializer.java` before going to production!

---

## 📧 How Email Works

- Email is sent via **Gmail SMTP** (configured server-side)
- The certificate is attached as a **PNG file** to the email
- No external email service (EmailJS etc.) needed — it uses Spring Boot's `JavaMailSender`
- Recipient email addresses come from your **Excel column** (auto-detected if named "Email" or "Mail")

---

## 🗄️ MongoDB Collections

| Collection | Purpose |
|------------|---------|
| `users` | Stores user accounts (created by DataInitializer) |
| `cert_history` | Stores all certificate generation sessions |

---

## 🔑 API Endpoints

| Method | URL | Auth | Description |
|--------|-----|------|-------------|
| POST | `/api/auth/login` | ❌ | Login, returns JWT token |
| GET | `/api/auth/me` | ✅ | Get current user info |
| POST | `/api/certificates/send-email` | ✅ | Send certificate via Gmail |
| POST | `/api/certificates/save-history` | ✅ | Save generation session |
| GET | `/api/certificates/history` | ✅ | Get user's history |
| DELETE | `/api/certificates/history/{id}` | ✅ | Delete a session |
| DELETE | `/api/certificates/history` | ✅ | Clear all history |

---

## 🏃 Quick Test

```bash
# 1. Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 2. Use the returned token in subsequent requests
TOKEN="eyJhbGciOiJIUzI1NiJ9..."

curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/auth/me
```

---

## 🔒 Security Notes

- JWT tokens expire after **24 hours**
- Passwords are hashed with **BCrypt**
- No registration endpoint exists — only admin-added users can login
- CSRF is disabled (stateless JWT API)
- Change `app.jwt.secret` in `application.properties` before production

---

## 📦 Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Spring Boot 3.2 |
| Security | Spring Security + JWT |
| Database | MongoDB |
| Email | Spring Mail + Gmail SMTP |
| Frontend | HTML5, Vanilla JS, Canvas API |
| Excel | Apache POI + SheetJS (client) |
| Build | Maven |
