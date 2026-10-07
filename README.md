# QRoll — QR-Based Attendance System

> A desktop application for professors to run real-time, anti-cheat attendance sessions using rotating QR codes. Students scan a QR code on their phones  no app install, no paper, no proxies.

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [System Architecture](#system-architecture)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Usage Guide](#usage-guide)
- [Project Structure](#project-structure)
- [Security Design](#security-design)
- [Database Schema](#database-schema)
- [OOP Design Patterns](#oop-design-patterns)
- [Known Limitations](#known-limitations)

---

## Overview

QRoll solves a real classroom problem: manual attendance is slow, paper-based registers are error-prone, and digital workarounds are trivially cheatable. QRoll replaces all of that with a system where the professor displays a rotating QR code on screen. Students scan it with their phone camera, enter their student ID, and their attendance is recorded instantly  with duplicate scan protection, device fingerprinting, and time-limited tokens baked in.

The application runs entirely on the professor's machine. No external server, no cloud dependency, no account creation for students.

---

## Features

**Session Management**
- Start and end attendance sessions per module (Lecture / Lab / Tutorial)
- Live session timer
- Fullscreen QR display mode for projector use

**Anti-Cheat Mechanisms**
- TOTP-based rolling tokens: QR code content changes every 30 seconds
- Device guard: one IP address = one submission per session
- Duplicate scan detection at both application and database level
- Rate limiting: max 10 requests per minute per IP
- Session UUID binding: old QR codes from previous sessions are rejected

**Live Dashboard**
- Real-time present/absent split as students scan
- Color-coded rows: green for on-time, orange for late (>15 min after session start)
- Searchable table by name, student ID, or group

**Reports**
- Export per-session CSV: present list, absent list, scan times, late flags, summary
- Export per-student CSV: attendance rate per module, risk threshold alerts, full history

**Student Management**
- Manual student entry
- Bulk import from CSV file
- Multi-select delete

---

## System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Professor's Machine                     │
│                                                             │
│  ┌──────────────┐   observer callback    ┌───────────────┐  │
│  │  JavaFX GUI  │ ◄──────────────────── │ Spring Server │  │
│  │  (UI thread) │                        │  (port 8080)  │  │
│  └──────┬───────┘                        └───────┬───────┘  │
│         │                                        │          │
│         └──────────────┬─────────────────────────┘          │
│                        │                                     │
│                  ┌─────▼──────┐                             │
│                  │  SQLite DB │                             │
│                  │  qroll.db  │                             │
│                  └────────────┘                             │
└─────────────────────────────────────────────────────────────┘
                          ▲
                          │ HTTP POST /scan
                          │ (local WiFi)
                   ┌──────┴──────┐
                   │Student Phone│
                   │  browser    │
                   └─────────────┘
```

The JavaFX UI and the Spring Boot server run on the same machine in separate threads. They communicate through a callback (observer pattern) — never through shared mutable state. All persistence goes through SQLite via JDBC.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Desktop UI | JavaFX 21 |
| Embedded Web Server | Spring Boot 3 |
| Database | SQLite 3 (via `sqlite-jdbc`) |
| QR Generation | ZXing (Google) |
| Token Algorithm | Custom TOTP (HmacSHA1, RFC 6238) |
| Build Tool | Apache Maven |
| Java Version | Java 21 |

---

## Prerequisites

Before running QRoll, make sure you have the following installed:

- **Java 21 or later** — [Download](https://adoptium.net/)
- **Maven 3.8+** — [Download](https://maven.apache.org/download.cgi)
- A machine with a **local area network** (WiFi router) that students can connect to

To verify your setup:
```bash
java -version    # should print: openjdk 21 or later
mvn -version     # should print: Apache Maven 3.x
```

---

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/moussakh189/qroll.git
cd qroll
```

### 2. Build the project

```bash
mvn clean package -DskipTests
```

This produces a fat JAR at `target/qroll-1.0-SNAPSHOT.jar` with all dependencies bundled.

### 3. Run the application

```bash
java -jar target/qroll-1.0-SNAPSHOT.jar
```

Or from your IDE: run `com.qroll.gui.App` as the main class.

### 4. First launch

On first run, `qroll.db` is created in the working directory with all tables initialized. Three demo modules are also seeded automatically:

```
ALGO_L3   — Algorithmics L3   (semester 3)
RESEAU_L3 — Networks L3       (semester 3)
CRYPTO_L3 — Cryptography L3   (semester 3)
```

To add your own modules, insert them directly into the `modules` table using any SQLite browser, or extend `ModuleRepository` to support a management UI.

---

## Usage Guide

### For Professors

**Starting a session:**

1. Open the **Session** tab
2. Select a module from the dropdown
3. Select the session type (Lecture / Lab / Tutorial)
4. Click **Start Session**

A QR code appears immediately. Click it to enter fullscreen mode for projector display. The code refreshes automatically every 30 seconds — the dashboard shows a countdown.

The status bar displays the server address: `http://192.168.x.x:8080`. Make sure students are connected to the same WiFi network.

**Monitoring attendance:**

Switch to the **Dashboard** tab. As students scan, they move from the Absent list to the Present list in real time. The stats bar shows current present/absent/total counts and the last scan time.

**Ending a session:**

Click **End Session**. The server shuts down, the session is marked `CLOSED` in the database, and the QR code is cleared.

**Exporting reports:**

Go to the **Reports** tab. Select a session from the dropdown and click **Export Session CSV** to save the attendance sheet. For a student's full attendance history across all modules, select a student and click **Export Student CSV**.

---

### For Students

1. Connect your phone to the same WiFi network as the professor's machine
2. Use your phone camera to scan the QR code displayed on screen
3. Your browser opens automatically — enter your Student ID and tap **Submit**
4. A confirmation message appears with your name if attendance was recorded

**Common errors students may see:**

| Message | Cause |
|---|---|
| QR code expired | You scanned an old QR — ask professor to show the current one |
| Already marked your attendance | Your ID was already submitted this session |
| This device has already submitted | Another ID was submitted from the same phone |
| Student ID not found | Your ID is not in the database — contact the professor |
| Session is closed | The professor ended the session before you scanned |

---

### Importing Students from CSV

Go to the **Students** tab and click **Import CSV**. The expected format is one student per line:

```
student_id,full_name,group,email
STU001,Ali Benali,G2,ali@ens.dz
STU002,Sara Mansour,G1,sara@ens.dz
STU003,Yacine Kaci,G1,
```

- Header row is auto-detected and skipped if the first column starts with `student` (case-insensitive)
- Email is optional
- Duplicate IDs are silently skipped
- Malformed rows (fewer than 3 columns) are logged and skipped

---

## Project Structure

```
qroll/
├── src/
│   └── main/
│       ├── java/com/qroll/
│       │   ├── crypto/
│       │   │   ├── TOTPEngine.java          # HmacSHA1-based token generation
│       │   │   ├── TOTPValidator.java       # validates current + previous window
│       │   │   └── QRCodeGenerator.java     # ZXing wrapper + URL builder
│       │   ├── database/
│       │   │   ├── DatabaseManager.java     # Singleton JDBC connection + DDL
│       │   │   ├── StudentRepository.java
│       │   │   ├── SessionRepository.java
│       │   │   ├── ModuleRepository.java
│       │   │   └── AttendanceRepository.java
│       │   ├── exception/
│       │   │   ├── DuplicateScanException.java
│       │   │   ├── ExpiredQRException.java
│       │   │   ├── SessionNotActiveException.java
│       │   │   └── StudentNotFoundException.java
│       │   ├── gui/
│       │   │   ├── App.java                 # JavaFX entry point
│       │   │   ├── MainController.java      # tab pane + controller wiring
│       │   │   ├── SessionController.java   # session start/stop + QR rotation
│       │   │   ├── DashboardController.java # live attendance view
│       │   │   ├── ReportController.java    # export UI
│       │   │   └── StudentController.java   # student CRUD + CSV import
│       │   ├── model/
│       │   │   ├── Student.java
│       │   │   ├── Module.java
│       │   │   ├── Session.java
│       │   │   ├── SessionType.java         # enum: LECTURE, LAB, TUTORIAL
│       │   │   ├── SessionStatus.java       # enum: ACTIVE, CLOSED
│       │   │   └── AttendanceRecord.java
│       │   ├── report/
│       │   │   ├── Reportable.java          # interface
│       │   │   ├── ReportGenerator.java     # delegates to Reportable
│       │   │   ├── SessionReport.java       # implements Reportable
│       │   │   └── StudentReport.java       # implements Reportable
│       │   └── server/
│       │       ├── AttendanceServer.java    # Spring Boot lifecycle manager
│       │       ├── ScanController.java      # REST endpoint + validation chain
│       │       ├── ScanRequest.java         # DTO
│       │       ├── DeviceGuard.java         # per-IP scan tracking
│       │       └── RateLimiter.java         # sliding window rate limiter
│       └── resources/
│           ├── fxml/
│           │   ├── main.fxml
│           │   ├── session.fxml
│           │   ├── dashboard.fxml
│           │   ├── report.fxml
│           │   └── student.fxml
│           ├── css/
│           │   └── style.css
│           └── static/
│               └── index.html               # mobile scan page (served by Spring)
├── qroll.db                                 # created on first run
└── pom.xml
```

---

## Security Design

### TOTP Token Rotation

QRoll implements a subset of RFC 6238 (TOTP) using HmacSHA1. Each session is assigned a random seed (UUID) at creation time. The token is computed as:

```
window  = floor(currentTimeMillis / 1000 / 30)
token   = HMAC-SHA1(seed, window) → dynamic truncation → 6-digit code
```

The QR code encodes a URL containing this token as a query parameter. The URL changes every 30 seconds. A student who screenshots the QR and shares it with someone outside the room can only use it within a 60-second window (current + previous window are accepted to handle network delays).

### Device Guard

Each HTTP request carries the client's IP address. On a phone connected to WiFi, this is the phone's LAN IP. The `DeviceGuard` class stores every IP that has submitted during a session. A second submission from the same IP is rejected with HTTP 403, regardless of student ID.

**Limitation:** Students sharing a mobile hotspot from the same phone would be blocked after one submission. Students behind a NAT that maps multiple devices to one external IP would also be blocked — this is unlikely on a classroom LAN where each device gets its own IP from the router's DHCP.

### Rate Limiter

A sliding window rate limiter caps submissions at 10 requests per 60-second window per IP. This prevents scripted brute-force guessing of student IDs.

### Input Validation

All incoming scan parameters are validated by regex before any business logic runs:

```
studentId  — ^[A-Za-z0-9_\-]{1,30}$
token      — ^[0-9]{6}$
session    — UUID v4 format
```

---

## Database Schema

```sql
CREATE TABLE students (
    student_id  TEXT PRIMARY KEY,
    full_name   TEXT NOT NULL,
    group_name  TEXT,
    email       TEXT
);

CREATE TABLE modules (
    module_code TEXT PRIMARY KEY,
    module_name TEXT NOT NULL,
    semester    INTEGER
);

CREATE TABLE sessions (
    session_id   TEXT PRIMARY KEY,
    module_code  TEXT REFERENCES modules(module_code),
    session_type TEXT CHECK(session_type IN ('LECTURE','LAB','TUTORIAL')),
    start_time   TEXT,
    end_time     TEXT,
    status       TEXT CHECK(status IN ('ACTIVE','CLOSED')),
    totp_seed    TEXT NOT NULL
);

CREATE TABLE attendance_records (
    record_id  INTEGER PRIMARY KEY AUTOINCREMENT,
    student_id TEXT REFERENCES students(student_id),
    session_id TEXT REFERENCES sessions(session_id),
    scan_time  TEXT NOT NULL,
    valid      INTEGER DEFAULT 1,
    UNIQUE(student_id, session_id)
);
```

WAL journaling mode is enabled for better concurrent read performance. Foreign keys are enforced.

---

## OOP Design Patterns

| Pattern | Where | Purpose |
|---|---|---|
| **Singleton** | `DatabaseManager` | Single shared JDBC connection across all repositories |
| **Repository** | `StudentRepository`, `SessionRepository`, `AttendanceRepository`, `ModuleRepository` | Isolate all DB access behind a clean interface |
| **Observer / Callback** | `ScanController.setOnNewScan()` → `DashboardController` | Push scan events from Spring thread to JavaFX UI thread safely |
| **Interface + Polymorphism** | `Reportable` ← `SessionReport`, `StudentReport` | `ReportGenerator.export()` works identically for both report types |
| **Custom Exceptions** | `com.qroll.exception.*` | Typed error propagation through the validation chain with distinct HTTP responses |
| **DTO** | `ScanRequest` | Decouples HTTP deserialization from domain logic |

---

## Known Limitations

- **Single active session:** Only one session can be active at a time. Running two simultaneous sessions (e.g., two classrooms) is not supported.
- **Module management:** Modules are currently seeded at startup. There is no UI to add/edit/delete modules — this must be done directly in the database.
- **No authentication:** The desktop application has no login screen. Physical access to the machine running QRoll is assumed to be restricted to the professor.
- **Local network only:** QRoll requires students and the professor to be on the same LAN. It does not work over the internet without port forwarding.
- **SQLite concurrency:** SQLite with WAL handles light concurrent reads well, but is not designed for high-throughput write scenarios. For a classroom of 30–40 students scanning within a short window, it is sufficient.
- **Port 8080 conflicts:** If another service is using port 8080, the server will fail to start. The application waits up to 5 seconds for the port to be released between sessions.

---

## License

This project was built as an academic exercise for an Object-Oriented Programming course at the National School of Cyber Security (Algeria). It is shared for educational purposes.

---

*Built by Moussa Khenfri — ENCS Algeria*
