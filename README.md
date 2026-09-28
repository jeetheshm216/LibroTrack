# LibroTrack - College Library Management System

LibroTrack is a full-featured college library management system that replaces manual register-based book tracking. It provides automated 14-day due date calculation, overdue fine tracking (₹5/day), inventory management, student registration, and dashboard analytics.

The application is built with a **Spring Boot** backend, **Spring Data JPA**, **MySQL**, and a responsive **web frontend** served directly from Spring Boot.

---

## Features

- **Dashboard**: Real-time statistics on total books, students, total/available copies, active borrowings, overdue books, and total fines collected.
- **Books Management**: Search books by title, author, or category; register new titles; edit book information; and delete books (with safeguards against deleting currently loaned books).
- **Student Borrowing Information**: Register students and view their active borrowings.
- **14-Day Automatic Due Date**: When a book is issued, the backend automatically sets the loan period to 14 days and decreases available inventory.
- **Automated Fine Calculation**: Calculates overdue fines upon book return at a fixed rate of ₹5 per late day.
- **Transaction Safety**: All issue and return operations are wrapped in `@Transactional` to ensure data integrity.
- **REST APIs & Postman Collection**: Comprehensive REST APIs for all operations with an included Postman collection (`LibroTrack_API.postman_collection.json`).

---

## Technology Stack

- **Backend**: Java 17, Spring Boot, Spring Data JPA, Hibernate, Bean Validation
- **Database**: MySQL Server 8.0 (Database name: `librotrack`)
- **Frontend**: Modern Vanilla HTML5, CSS3, JavaScript SPA (served from `src/main/resources/static`)
- **Testing**: JUnit 5, Spring Boot Test, Mockito

---

## Prerequisites

- **Java Development Kit (JDK 17+)**
- **MySQL Server 8.0+** running locally on port `3306`
- **Maven** (included via `./mvnw` wrapper)

---

## Database Configuration

Create the MySQL database:

```sql
CREATE DATABASE IF NOT EXISTS librotrack;
```

Update your database credentials in `src/main/resources/application.properties` if needed:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/librotrack?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

---

## How to Run the Application

### 1. Start the Spring Boot Backend

Run the following command in the project root:

```bash
# On Windows PowerShell / Command Prompt:
.\mvnw.cmd spring-boot:run

# On Linux / macOS:
./mvnw spring-boot:run
```

The application will start on **`http://localhost:8080`**.

### 2. Access the Web Application

Open your browser and navigate to:

```text
http://localhost:8080/
```

Initial sample books and student records will be seeded automatically on first launch.

---

## REST API Endpoints

### Books
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/books` | Register a new book |
| `GET` | `/api/books` | Get all books |
| `GET` | `/api/books/{id}` | Get book by ID |
| `PUT` | `/api/books/{id}` | Update book details |
| `DELETE`| `/api/books/{id}` | Delete book (if not issued) |
| `GET` | `/api/books/search?query=` | Search title, author, category |

### Students
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/students` | Register a new student |
| `GET` | `/api/students` | Get all students with active issue count |
| `GET` | `/api/students/{id}` | Get student by ID |
| `GET` | `/api/students/{id}/issued-books` | Get currently issued books for student |

### Issues & Returns
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/issues` | Issue book (`{"bookId": 1, "studentId": 1}`) |
| `GET` | `/api/issues` | Get all issue records |
| `GET` | `/api/issues/active` | Get currently active borrowed books |
| `GET` | `/api/issues/{id}` | Get single issue record |
| `PUT` | `/api/issues/{id}/return` | Return book and calculate fine |

### Dashboard
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/dashboard/stats` | Get library dashboard statistics |

---

## Postman Collection

Import `LibroTrack_API.postman_collection.json` into Postman. The collection has pre-configured requests and edge case tests with `{{baseUrl}} = http://localhost:8080`.

---

## Running Automated Tests

```bash
.\mvnw.cmd test
```
