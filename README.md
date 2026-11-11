# GMU ERP (Java)

A rebuild of the GM University ERP in Java with Spring Boot. Version 0.1 covers phase 1: logins, students, faculty, courses, timetable, attendance, results and notices.

## Run it

You need **Java 21** and **Maven 3.9+**.

```bash
mvn spring-boot:run
```

Open http://localhost:8080. On first start the app creates a database in `./data` and fills it with demo data.

| Role    | Username   | Password    |
|---------|------------|-------------|
| Admin   | `admin`    | `admin123`  |
| Faculty | `F001`     | `faculty123`|
| Student | `U25CS001` | `student123`|

To start again from scratch, stop the app and delete the `data/` folder.

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

## Deploy (free, Render)

[![Deploy to Render](https://render.com/images/deploy-to-render-button.svg)](https://render.com/deploy?repo=https://github.com/neonjava/gmu-erp)

Render builds the `Dockerfile` and starts the app. The free plan sleeps after 15 minutes idle (the first visit then takes about a minute), and the H2 data resets on each redeploy; the demo data is recreated automatically. Use the `mysql` profile with a hosted database to keep data.

## What each role can do

**Admin**
- Add, search, edit and delete students and faculty. Each one gets a login automatically (username is the USN or employee ID, password `student123` / `faculty123`); passwords can be reset.
- Manage departments and courses, and assign a faculty member to each course.
- Build the timetable for each semester and section.
- Post notices.

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

Everyone can see notices and change their password. The dashboard mirrors the app's sections; features not built yet show as "Coming soon".

## Code layout

```
in.neonjava.erp
├── model      JPA entities. Person (abstract) → Student, Faculty
├── repo       Spring Data repositories
├── service    Business rules: accounts, attendance %, grading/SGPA, timetable, demo data
├── security   Spring Security config, login lookup, current user
└── web        Controllers (student, faculty, admin/*)
templates/     Thymeleaf pages
static/        CSS and JS
```

## Grading rules (edit in `Mark.java`)

CIE 1 and CIE 2 are out of 25 each and SEE is out of 50, for a total out of 100. Grades: O ≥90, A+ ≥80, A ≥70, B+ ≥60, B ≥55, C ≥50, P ≥40, F below 40. A grade appears only after SEE marks are entered.

## Next phases

1. Exams: hall ticket PDF (eligible with 75% attendance and fees paid), exam seating, re-apply
2. Finance: fee structure, online payment, receipts
3. Quiz engine, calendar, events, grievances, feedback
4. Community, bus routes, lab QR scanner, library link, REST API for the mobile app
