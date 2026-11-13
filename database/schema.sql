-- SCASP relational schema (H2 / standard SQL). Safe to run repeatedly.
-- Timestamps are stored as epoch milliseconds (BIGINT) so every database treats them the same way.

CREATE TABLE IF NOT EXISTS users (
    user_id       VARCHAR(20)  PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL CHECK (role IN ('STUDENT','ACADEMIC_STAFF','ADMIN')),
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    password_hash VARCHAR(200) NOT NULL
);

CREATE TABLE IF NOT EXISTS students (
    student_id    VARCHAR(20)  PRIMARY KEY,
    programme     VARCHAR(100) NOT NULL,
    year_of_study INT          NOT NULL CHECK (year_of_study BETWEEN 1 AND 8),
    FOREIGN KEY (student_id) REFERENCES users(user_id)
);

CREATE TABLE IF NOT EXISTS courses (
    code     VARCHAR(20)  PRIMARY KEY,
    title    VARCHAR(150) NOT NULL,
    capacity INT          NOT NULL CHECK (capacity BETWEEN 1 AND 1000),
    status   VARCHAR(12)  NOT NULL CHECK (status IN ('OPEN','CLOSED','CANCELLED'))
);

CREATE TABLE IF NOT EXISTS registrations (
    student_id  VARCHAR(20) NOT NULL,
    course_code VARCHAR(20) NOT NULL,
    created_at  BIGINT      NOT NULL,
    PRIMARY KEY (student_id, course_code),            -- a student can register for a course once
    FOREIGN KEY (student_id)  REFERENCES students(student_id),
    FOREIGN KEY (course_code) REFERENCES courses(code)
);
CREATE INDEX IF NOT EXISTS idx_reg_course ON registrations(course_code);

CREATE TABLE IF NOT EXISTS waitlist (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,    -- queue order
    student_id  VARCHAR(20) NOT NULL,
    course_code VARCHAR(20) NOT NULL,
    created_at  BIGINT      NOT NULL,
    UNIQUE (student_id, course_code),
    FOREIGN KEY (student_id)  REFERENCES students(student_id),
    FOREIGN KEY (course_code) REFERENCES courses(code)
);
CREATE INDEX IF NOT EXISTS idx_wait_course ON waitlist(course_code);

CREATE TABLE IF NOT EXISTS service_requests (
    request_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id  VARCHAR(20)  NOT NULL,
    category    VARCHAR(20)  NOT NULL,
    priority    VARCHAR(10)  NOT NULL CHECK (priority IN ('LOW','NORMAL','HIGH','URGENT')),
    status      VARCHAR(12)  NOT NULL CHECK (status IN ('SUBMITTED','ASSIGNED','IN_PROGRESS','RESOLVED','CLOSED')),
    assigned_to VARCHAR(20),
    details     VARCHAR(500) NOT NULL,
    created_at  BIGINT       NOT NULL,
    updated_at  BIGINT       NOT NULL,
    FOREIGN KEY (student_id)  REFERENCES students(student_id),
    FOREIGN KEY (assigned_to) REFERENCES users(user_id)
);
CREATE INDEX IF NOT EXISTS idx_req_status ON service_requests(status);

CREATE TABLE IF NOT EXISTS announcements (
    announcement_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title           VARCHAR(150)  NOT NULL,
    body            VARCHAR(2000) NOT NULL,
    audience        VARCHAR(10)   NOT NULL CHECK (audience IN ('ALL','STUDENTS','STAFF')),
    published_at    BIGINT        NOT NULL
);

CREATE TABLE IF NOT EXISTS audit_events (
    event_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor       VARCHAR(20) NOT NULL,
    action      VARCHAR(40) NOT NULL,
    entity_type VARCHAR(30) NOT NULL,
    entity_id   VARCHAR(60) NOT NULL,
    occurred_at BIGINT      NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_audit_time ON audit_events(occurred_at);

CREATE TABLE IF NOT EXISTS campus_services (
    service_id  BIGINT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(300) NOT NULL,
    latitude    DOUBLE       NOT NULL,
    longitude   DOUBLE       NOT NULL
);
