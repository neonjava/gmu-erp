# GMU ERP (Java)

A rebuild of the GM University ERP in Java with Spring Boot. It covers logins, students, faculty, courses, timetable, attendance, results, notices, an events portal and resource booking.

**Live preview:** https://neonjava.github.io/gmu-erp/ (the real screens on saved sample data. Saving is turned off; run the app for the working version).

## Run it

You need **Java 21** and **Maven 3.9+**.

```bash
mvn spring-boot:run
```

Open http://localhost:8080 (it opens the app at `/app`). The first build also builds the React frontend and downloads its own Node, so it takes a minute longer; after that it is quick. On first start the app creates a database in `./data` and fills it with demo data.

| Role    | Username   | Password    |
|---------|------------|-------------|
| Admin   | `admin`    | `admin123`  |
| Faculty | `F001`     | `faculty123`|
| Student | `U25CS001` | `student123`|

To start again from scratch, stop the app and delete the `data/` folder.

## Sign-in and accounts

- New accounts start on the default password and must choose their own at first sign-in.
- Five wrong passwords in a row lock the account for 15 minutes. The admin can unlock it sooner.
- The admin can reset a password, turn sign-in off or on, or delete a student (Students page, with a filter by sign-in status).
- "Keep me signed in" lasts a week.

## Frontend

The interface is a **React + TypeScript** app (Vite, React Router, Framer Motion, Lucide icons, react-snowfall for the falling leaves) in `frontend/`. It talks to the Spring Boot backend through JSON endpoints under `/api`. The design is frosted glass in the GMU maroon and gold, with a floating "island" bar.

Pages already in React: sign-in, home, profile and password, notices, the student pages, and the admin Students page. The other pages (faculty, admin courses/faculty/timetable, events, resources) are still server-rendered with Thymeleaf and share the same stylesheet; they move over next.

For frontend work, run the Java app and then `cd frontend && npm install && npm run dev` (http://localhost:5173, proxies `/api` to Java). `mvn package -Dskip.npm=true` skips the frontend build.

### Using MySQL instead of H2

```bash
DB_USER=root DB_PASSWORD=yourpass mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

The `gmu_erp` database is created automatically.

### Build a jar

```bash
mvn clean package
java -jar target/gmu-erp-0.1.0.jar
```

## Deploy

**Full app on Render (free).** [![Deploy to Render](https://render.com/images/deploy-to-render-button.svg)](https://render.com/deploy?repo=https://github.com/neonjava/gmu-erp)

Render builds the `Dockerfile` (Maven builds the Java app and the React frontend) and starts it. The free plan sleeps after 15 minutes idle, so the first visit takes about a minute, and the H2 data resets on each redeploy; the demo data is recreated automatically. Use the `mysql` profile with a hosted database to keep data.

**Any Docker host.**

```bash
docker build -t gmu-erp .
docker run -p 8080:8080 gmu-erp        # then open http://localhost:8080
```

**Static preview on GitHub Pages.** `.github/workflows/pages.yml` builds the frontend in preview mode and publishes it on every push to `main` that touches the frontend. There is no server: the screens read sample data saved in `frontend/demo-data/`, and saving shows a "preview only" message. To refresh the sample data, start the app on an empty database and run `cd frontend && npm run capture:demo`. To build the preview locally: `npm run build:demo` (output in `frontend/dist-demo`).

## What each role can do

**Admin**
- Add, search, edit and delete students and faculty. Each one gets a login automatically (username is the USN or employee ID, password `student123` / `faculty123`); passwords can be reset.
- Manage departments and courses, and assign a faculty member to each course.
- Build the timetable for each semester and section.
- Post notices.
- Manage bookable resources (labs, classrooms, seminar halls, equipment): add, disable or delete. See every upcoming booking and cancel any of them.

**Faculty**
- See their courses and weekly timetable.
- Mark attendance for any past date: all students start as present and you untick the absentees. Editing a day that was already taken updates it.
- Enter CIE 1, CIE 2 and SEE marks. Values are validated, and if any value is wrong nothing is saved.

**Student**
- Dashboard showing overall attendance %, number of courses with a shortage, and SGPA so far.
- Attendance per course, with a shortage flag and how many classes in a row are needed to get back to 75%.
- Weekly timetable.
- Results: CIE, SEE, total, grade and SGPA.
- Register for or drop courses offered for their semester.

**Events portal (everyone)**
- Admin and faculty create events with a category, venue, date, time and seat limit (0 means no limit).
- Students register or cancel. Registration closes when the event is full or over.
- The organiser and admins see the participant list and can delete the event.

**Resource booking (everyone)**
- Each resource has a day view with periods P1–P6, bookable up to 30 days ahead (not Sundays or past periods).
- A period can be booked only once (also enforced by a database constraint).
- Rooms whose name matches a timetable room (e.g. `LAB-2`) are blocked during scheduled classes.
- Students can book labs and equipment only, with at most 2 upcoming bookings; rooms and halls are booked by faculty.
- Users cancel their own bookings; admins can cancel any.

Everyone can see notices and change their password. The dashboard mirrors the app's sections;.

## Code layout

```
in.neonjava.erp
├── model      JPA entities. Person (abstract) → Student, Faculty
├── repo       Spring Data repositories
├── service    Business rules: accounts, attendance %, grading/SGPA, timetable, events, bookings, demo data
├── security   Spring Security config, login lockout, account checks, current user
├── api        JSON endpoints for the React app (/api/...)
└── web        Server-rendered controllers (faculty, admin/*, events, resources)
templates/     Thymeleaf pages (not yet moved to React)
static/        Shared CSS and JS
frontend/      React + TypeScript app, built into the jar under /app
```

## Grading rules (edit in `Mark.java`)

CIE 1 and CIE 2 are out of 25 each and SEE is out of 50, for a total out of 100. Grades: O ≥90, A+ ≥80, A ≥70, B+ ≥60, B ≥55, C ≥50, P ≥40, F below 40. A grade appears only after SEE marks are entered.

## Next phases

1. Exams: hall ticket PDF (eligible with 75% attendance and fees paid), exam seating, re-apply
2. Finance: fee structure, online payment, receipts
3. Quiz engine, calendar, grievances, feedback
4. Community, bus routes, lab QR scanner, library link, REST API for the mobile app
