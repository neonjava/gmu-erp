package in.neonjava.erp.api;

import in.neonjava.erp.model.AppUser;
import in.neonjava.erp.model.Mark;
import in.neonjava.erp.repo.*;
import in.neonjava.erp.security.CurrentUserService;
import in.neonjava.erp.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Who is signed in, their dashboard, and their password. */
@RestController
@RequestMapping("/api")
public class MeApi {

    private final CurrentUserService currentUser;
    private final AccountService accounts;
    private final NoticeRepository notices;
    private final StudentRepository students;
    private final FacultyRepository faculty;
    private final CourseRepository courses;
    private final AttendanceService attendance;
    private final ResultService results;
    private final EventService events;
    private final TimetableService timetable;
    private final BookingService booking;

    public MeApi(CurrentUserService currentUser, AccountService accounts, NoticeRepository notices,
                 StudentRepository students, FacultyRepository faculty, CourseRepository courses,
                 AttendanceService attendance, ResultService results, EventService events,
                 TimetableService timetable, BookingService booking) {
        this.currentUser = currentUser;
        this.accounts = accounts;
        this.notices = notices;
        this.students = students;
        this.faculty = faculty;
        this.courses = courses;
        this.attendance = attendance;
        this.results = results;
        this.events = events;
        this.timetable = timetable;
        this.booking = booking;
    }

    /** The signed-in user, or 401. Also hands out the CSRF cookie the frontend sends back on every change. */
    @GetMapping("/me")
    public ResponseEntity<Dto.Me> me(CsrfToken csrf) {
        csrf.getToken();
        AppUser u = currentUser.get();
        return u == null ? ResponseEntity.status(401).build() : ResponseEntity.ok(Dto.Me.of(u));
    }

    public record PasswordChange(String current, String next, String confirm) {}

    @PostMapping("/me/password")
    public Map<String, String> changePassword(@RequestBody PasswordChange p) {
        AppUser me = currentUser.get();
        boolean first = me.isMustChangePassword();
        accounts.changePassword(me, p.current(), p.next(), p.confirm());
        return Map.of("message", first ? "Password set. Welcome to GMU ERP" : "Password changed");
    }

    @GetMapping("/dashboard")
    public Dto.Dashboard dashboard() {
        AppUser me = currentUser.get();
        int hour = LocalTime.now().getHour();
        String greeting = hour < 12 ? "Good morning" : hour < 17 ? "Good afternoon" : "Good evening";
        List<Dto.Stat> stats = new ArrayList<>();
        List<TimetableService.TodayClass> today = null;
        List<Dto.CourseRow> myCourses = null;

        switch (me.getRole()) {
            case STUDENT -> {
                var att = attendance.summaryFor(me.getStudent());
                long attended = att.stream().mapToLong(AttendanceService.Summary::attended).sum();
                long total = att.stream().mapToLong(AttendanceService.Summary::total).sum();
                double pct = total == 0 ? 0 : Math.round(attended * 1000.0 / total) / 10.0;
                List<Mark> rows = results.marksFor(me.getStudent());
                Double sgpa = results.sgpa(rows);
                stats.add(new Dto.Stat("Attendance", pct + "%", "/student/attendance", pct));
                stats.add(new Dto.Stat("SGPA so far", sgpa == null ? "-" : sgpa.toString(), "/student/results", null));
                stats.add(new Dto.Stat("Courses below 75%",
                        String.valueOf(att.stream().filter(AttendanceService.Summary::shortage).count()), "/student/attendance", null));
                today = timetable.todayFor(me.getStudent().getSemester(), me.getStudent().getSection());
            }
            case FACULTY -> {
                var mine = courses.findByFacultyOrderByCode(me.getFaculty());
                myCourses = mine.stream().map(Dto.CourseRow::of).toList();
                stats.add(new Dto.Stat("Courses teaching", String.valueOf(mine.size()), "/faculty/courses", null));
                stats.add(new Dto.Stat("Events coming up", String.valueOf(events.nextFew().size()), "/events", null));
                today = timetable.todayFor(mine);
            }
            case ADMIN -> {
                stats.add(new Dto.Stat("Students", String.valueOf(students.count()), "/admin/students", null));
                stats.add(new Dto.Stat("Faculty", String.valueOf(faculty.count()), "/admin/faculty", null));
                stats.add(new Dto.Stat("Courses", String.valueOf(courses.count()), "/admin/courses", null));
                stats.add(new Dto.Stat("Upcoming events", String.valueOf(events.upcoming().size()), "/events", null));
                stats.add(new Dto.Stat("Bookings today", String.valueOf(booking.bookingsToday()), "/resources", null));
            }
        }
        return new Dto.Dashboard(greeting, stats, today,
                notices.findTop5ByOrderByPostedOnDescIdDesc().stream().map(Dto.NoticeRow::of).toList(),
                events.nextFew().stream().map(Dto.EventRow::of).toList(), myCourses);
    }

    @GetMapping("/notices")
    public List<Dto.NoticeRow> notices() {
        return notices.findAllByOrderByPostedOnDescIdDesc().stream().map(Dto.NoticeRow::of).toList();
    }
}
