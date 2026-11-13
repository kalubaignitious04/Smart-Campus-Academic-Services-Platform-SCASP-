-- SCASP fictional sample data. Demo password for every account: Demo#2026 (hashes are salted PBKDF2).
-- Runs only when the users table is empty.

INSERT INTO users(user_id,name,role,active,password_hash) VALUES('S1001','Chola Demo','STUDENT',TRUE,'pbkdf2$65536$QvWyo0UR1OGv9MswRRW6ew==$+Ys5/IFDO32neVM2DQpknbIOZ6J4/YVR/IPr8kh0aZ8=');
INSERT INTO users(user_id,name,role,active,password_hash) VALUES('S1002','Mwansa Demo','STUDENT',TRUE,'pbkdf2$65536$/TKURuvst5rJxGalZ5atPg==$3OV5RJdSZ8WGIB1ve/uqcZGkQVuGpyJn0/RwGvwkQJg=');
INSERT INTO users(user_id,name,role,active,password_hash) VALUES('S1003','Natasha Demo','STUDENT',TRUE,'pbkdf2$65536$shrwGn3EFxpHqBL9XiImvg==$/DFHx3Cmhmw1VujUOiKIwTItGXzMaV+PoSmDDUrarHI=');
INSERT INTO users(user_id,name,role,active,password_hash) VALUES('S1004','Kondwani Demo','STUDENT',TRUE,'pbkdf2$65536$+iawubqgTDRucD6aTgGpRA==$zq+b+lBc/SnM5xAyMrM24qUHt0VKrINsnOPF1D2GyFo=');
INSERT INTO users(user_id,name,role,active,password_hash) VALUES('S1005','Lungile Demo','STUDENT',TRUE,'pbkdf2$65536$cJmzZ+yRZS5n2DUszV8HiA==$ajmEdSUf72Z3MQ0Lp6+51+FzJcYCzcY8/wu55+m7OFc=');
INSERT INTO users(user_id,name,role,active,password_hash) VALUES('T2001','Registrar Demo','ACADEMIC_STAFF',TRUE,'pbkdf2$65536$hFHqci7cTa79zUgpdikJMA==$HshczkwwlOe5O94RuSc+St05mx8BpJyuhTr/sczF+uI=');
INSERT INTO users(user_id,name,role,active,password_hash) VALUES('A3001','Admin Demo','ADMIN',TRUE,'pbkdf2$65536$nq7qPf4KFU51cO6qTcFtvg==$zKTdOWIp3MybYMNIktbJrdNzMt0tqghIeK513SooARU=');

INSERT INTO students(student_id,programme,year_of_study) VALUES('S1001','BSc Computer Science',3),('S1002','BSc Computer Science',3),('S1003','BSc Information Systems',2),('S1004','BSc Computer Science',1),('S1005','BSc Information Systems',2);

INSERT INTO courses(code,title,capacity,status) VALUES('CS101','Introduction to Programming',2,'OPEN'),('CS210','Data Structures',40,'OPEN'),('DB201','Database Systems',3,'OPEN'),('NET220','Computer Networks',30,'OPEN'),('ENT301','Entrepreneurship',25,'OPEN');

INSERT INTO registrations(student_id,course_code,created_at) VALUES('S1001','CS101',1767225600000),('S1001','DB201',1767225600000);

INSERT INTO announcements(title,body,audience,published_at) VALUES('Welcome to SCASP','Register for courses and raise service requests here.','ALL',1767225600000),('Registry hours','The Registry Office is open 08:00-16:00, Monday to Friday.','STUDENTS',1767225600000);

INSERT INTO campus_services(service_id,name,description,latitude,longitude) VALUES(1,'Library','Study space and book loans',-15.4167,28.2833),(2,'Computer Lab','Open-access computers',-15.4172,28.2841),(3,'Registry Office','Transcripts, clearance and appeals',-15.4160,28.2825),(4,'Health Clinic','Basic student health services',-15.4179,28.2819);
