# User guide

Open `http://localhost:8080`. Sign in with a demo account (password in the README).

## Students
* **Courses**: search by code or title, sort by code, title or most seats left. Press **Register**. If the course is full the button says
  **Join waiting list**; when someone withdraws, the first person waiting is registered automatically.
* **My registrations**: shows your courses and waiting positions; **Withdraw** or **Leave list**.
* **Requests**: choose a category and priority, describe the problem (max 500 characters) and submit. Track the status in the table.
* **Announcements** and **Campus**: read notices; **Open map** shows a service on OpenStreetMap.

## Staff
* **Courses**: add or update a course, open, close or cancel it (cancelled is final). Raising capacity admits waiting students.
* **Requests**: sees all requests, most urgent first. **Assign** a staff member, then **Move** the status one step at a time.
* **Announcements**: publish for everyone, students or staff.
* **Reports**: capacity, waiting lists, request workload, registrations per student, latest audit events, processor statistics.

## Other clients
TCP: `./scasp.sh gateway` then type commands (see `docs/protocols/protocols.md`). Admin (RMI): `./scasp.sh admin localhost 1099 capacity`.
To close a course remotely use the token printed by the server: `./scasp.sh admin localhost 1099 status <token> CS101 CLOSED`.
