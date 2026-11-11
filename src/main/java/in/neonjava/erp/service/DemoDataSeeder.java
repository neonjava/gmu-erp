package in.neonjava.erp.service;

import in.neonjava.erp.model.*;
import in.neonjava.erp.repo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

/** Fills an empty database with sample data so every screen has something to show.
 *  Runs before the web server starts, so no request can arrive before the demo logins exist. */
@Component
public class DemoDataSeeder implements SmartInitializingSingleton {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final boolean enabled;
    private final UserRepository users;
    private final DepartmentRepository departments;
    private final CourseRepository courses;
    private final RegistrationRepository registrations;
    private final AttendanceRepository attendance;
    private final MarkRepository marks;
    private final NoticeRepository notices;
    private final AccountService accounts;
    private final TimetableService timetable;
    private final PasswordEncoder encoder;

    public DemoDataSeeder(@Value("${erp.seed-demo-data:true}") boolean enabled,
                          UserRepository users, DepartmentRepository departments, CourseRepository courses,
                          RegistrationRepository registrations, AttendanceRepository attendance,
                          MarkRepository marks, NoticeRepository notices, AccountService accounts,
                          TimetableService timetable, PasswordEncoder encoder) {
        this.enabled = enabled;
        this.users = users;
        this.departments = departments;
        this.courses = courses;
        this.registrations = registrations;
        this.attendance = attendance;
        this.marks = marks;
        this.notices = notices;
        this.accounts = accounts;
        this.timetable = timetable;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void afterSingletonsInstantiated() {
        if (users.count() > 0) return;

        // The admin account is always created on an empty database.
        users.save(new AppUser("admin", encoder.encode("admin123"), Role.ADMIN, "ERP Administrator"));
        if (!enabled) return;

        log.info("Seeding demo data...");
        Random rnd = new Random(42);

        Department cse = departments.save(new Department("CSE", "Computer Science & Engineering"));
        departments.save(new Department("ISE", "Information Science & Engineering"));
        departments.save(new Department("ECE", "Electronics & Communication Engineering"));

        Faculty f1 = faculty("F001", "Dr. Ramesh K", "Professor", cse);
        Faculty f2 = faculty("F002", "Prof. Anitha S", "Assistant Professor", cse);
        Faculty f3 = faculty("F003", "Prof. Kiran M", "Assistant Professor", cse);

        List<Course> sem3 = List.of(
                course("UE25CS2303", "Object Oriented Programming with Java", 4, cse, f1),
                course("UE25CS2301", "Data Structures", 4, cse, f2),
                course("UE25CS2302", "Database Management Systems", 3, cse, f1),
                course("UE25MA2301", "Discrete Mathematics", 3, cse, f3),
                course("UE25CS2304", "Computer Organization", 3, cse, f2));

        String[] names = {"Hemanth S Palekar", "Aishwarya R", "Bharath K", "Chandana M", "Darshan P",
                "Keerthi N", "Manoj G", "Pooja V", "Rahul B", "Sneha T"};
        List<Student> sts = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            Student s = new Student();
            s.setUsn(String.format("U25CS%03d", i + 1));
            s.setName(names[i]);
            s.setEmail(s.getUsn().toLowerCase() + "@student.gmu.ac.in");
            s.setDepartment(cse);
            s.setSemester(3);
            s.setSection("A");
            sts.add(accounts.saveStudent(s));
        }

        for (Student s : sts) for (Course c : sem3) registrations.save(new Registration(s, c));

        // Timetable: rotate the 5 courses across 6 days x 6 periods.
        int k = 0;
        for (DayOfWeek d : TimetableService.WORKING_DAYS) {
            int periods = d == DayOfWeek.SATURDAY ? 3 : 6;
            for (int p = 1; p <= periods; p++) {
                Course c = sem3.get(k++ % sem3.size());
                timetable.setSlot(3, "A", d, p, c, p <= 3 ? "CR-201" : "LAB-2");
            }
        }

        // 20 past working days of attendance. Each student has their own attendance tendency.
        LocalDate day = LocalDate.now().minusDays(1);
        int taken = 0;
        while (taken < 20) {
            if (day.getDayOfWeek() != DayOfWeek.SUNDAY) {
                for (Course c : sem3) {
                    for (int i = 0; i < sts.size(); i++) {
                        double tendency = 0.62 + (i % 5) * 0.08;   // 62% .. 94%
                        attendance.save(new AttendanceRecord(sts.get(i), c, day, rnd.nextDouble() < tendency));
                    }
                }
                taken++;
            }
            day = day.minusDays(1);
        }

        // CIE marks for all, SEE only for two courses (results "partially published").
        for (Student s : sts) {
            for (int ci = 0; ci < sem3.size(); ci++) {
                Mark m = new Mark(s, sem3.get(ci));
                m.setCie1(12 + rnd.nextInt(14));
                m.setCie2(12 + rnd.nextInt(14));
                if (ci < 2) m.setSee(18 + rnd.nextInt(33));
                marks.save(m);
            }
        }

        notices.save(dated(new Notice("CIE 2 timetable published",
                "CIE 2 for 3rd semester starts next Monday. Check the Time Table section for the schedule."), 1));
        notices.save(dated(new Notice("OOPS PBL proposal submission",
                "Groups must submit the Smart Campus PBL proposal to their course faculty by Friday."), 3));
        notices.save(dated(new Notice("Fee payment reminder",
                "Last date for odd-semester fee payment without fine is the 30th of this month."), 6));

        log.info("Demo data ready. Logins: admin/admin123, F001/faculty123, U25CS001/student123");
    }

    private Faculty faculty(String empId, String name, String designation, Department d) {
        Faculty f = new Faculty();
        f.setEmpId(empId);
        f.setName(name);
        f.setDesignation(designation);
        f.setDepartment(d);
        f.setEmail(empId.toLowerCase() + "@gmu.ac.in");
        return accounts.saveFaculty(f);
    }

    private Course course(String code, String name, int credits, Department d, Faculty f) {
        Course c = new Course();
        c.setCode(code);
        c.setName(name);
        c.setCredits(credits);
        c.setSemester(3);
        c.setDepartment(d);
        c.setFaculty(f);
        return courses.save(c);
    }

    private Notice dated(Notice n, int daysAgo) {
        n.setPostedOn(LocalDate.now().minusDays(daysAgo));
        return n;
    }
}
