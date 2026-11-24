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
import java.time.LocalTime;
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
    private final EventRepository events;
    private final EventRegistrationRepository eventRegs;
    private final CampusResourceRepository resources;
    private final BookingRepository bookings;
    private final AccountService accounts;
    private final TimetableService timetable;
    private final PasswordEncoder encoder;

    public DemoDataSeeder(@Value("${erp.seed-demo-data:true}") boolean enabled,
                          UserRepository users, DepartmentRepository departments, CourseRepository courses,
                          RegistrationRepository registrations, AttendanceRepository attendance,
                          MarkRepository marks, NoticeRepository notices, AccountService accounts,
                          TimetableService timetable, PasswordEncoder encoder,
                          EventRepository events, EventRegistrationRepository eventRegs,
                          CampusResourceRepository resources, BookingRepository bookings) {
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
        this.events = events;
        this.eventRegs = eventRegs;
        this.resources = resources;
        this.bookings = bookings;
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

        seedEvents(sts);
        seedResources();
        seedLoginStatus();

        log.info("Demo data ready. Logins: admin/admin123, F001/faculty123, U25CS001/student123");
    }

    /** Demo accounts can sign in straight away; two students show the other states in the admin list. */
    private void seedLoginStatus() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        int i = 0;
        for (AppUser u : users.findAll()) {
            u.setMustChangePassword(false);
            if (u.getRole() != Role.ADMIN) u.setLastLogin(now.minusHours(3 + 7L * i++));
            switch (u.getUsername()) {
                case "U25CS009" -> { u.setMustChangePassword(true); u.setLastLogin(null); }   // new: must set a password
                case "U25CS010" -> u.setEnabled(false);                                      // turned off by the admin
                default -> { }
            }
            users.save(u);
        }
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

    private void seedEvents(List<Student> sts) {
        Event hack = event("CodeSprint 24-hour Hackathon", EventCategory.TECHNICAL, "Main Auditorium", 6, "09:00", 120,
                "Build a working prototype in 24 hours in teams of up to 4. Food and Wi-Fi provided. Prizes for the top 3 teams.",
                "Dr. Ramesh K", "F001");
        Event workshop = event("Spring Boot Hands-on Workshop", EventCategory.WORKSHOP, "LAB-1", 3, "14:00", 40,
                "Build and deploy a REST API with Spring Boot. Bring your laptop with JDK 21 installed.",
                "Dr. Ramesh K", "F001");
        Event resume = event("Resume Building Session", EventCategory.SEMINAR, "Seminar Hall", 2, "11:15", 5,
                "Training and Placement cell session on writing a resume and preparing for internships.",
                "ERP Administrator", "admin");
        Event cricket = event("Inter-department Cricket League", EventCategory.SPORTS, "University Ground", 9, "08:00", 88,
                "Eight teams, one trophy. Register as a player; captains will be contacted by the sports department.",
                "Prof. Kiran M", "F003");
        Event fest = event("Utsav Cultural Fest", EventCategory.CULTURAL, "Open Air Theatre", 12, "17:00", 0,
                "Music, dance, drama and food stalls. Open to all students.",
                "ERP Administrator", "admin");
        Event lecture = event("AI in Healthcare: Guest Lecture", EventCategory.SEMINAR, "Seminar Hall", -5, "11:15", 150,
                "Guest lecture on applications of machine learning in diagnostics.",
                "Prof. Anitha S", "F002");

        // Hemanth (sts[0]) is registered for the hackathon and the workshop; the resume session is full.
        for (int i = 0; i < sts.size(); i++) {
            if (i < 7) eventRegs.save(new EventRegistration(hack, sts.get(i)));
            if (i < 4) eventRegs.save(new EventRegistration(workshop, sts.get(i)));
            if (i >= 1 && i <= 5) eventRegs.save(new EventRegistration(resume, sts.get(i)));
            if (i % 2 == 1) eventRegs.save(new EventRegistration(cricket, sts.get(i)));
            if (i % 3 == 0) eventRegs.save(new EventRegistration(fest, sts.get(i)));
            eventRegs.save(new EventRegistration(lecture, sts.get(i)));
        }
    }

    private Event event(String title, EventCategory cat, String venue, int inDays, String time, int capacity,
                        String description, String organizer, String createdBy) {
        Event e = new Event(title, cat, venue, LocalDate.now().plusDays(inDays), LocalTime.parse(time), capacity);
        e.setDescription(description);
        e.setOrganizer(organizer);
        e.setCreatedBy(createdBy);
        return events.save(e);
    }

    private void seedResources() {
        CampusResource lab1 = resources.save(new CampusResource("LAB-1", ResourceType.LAB, "CSE Block, 1st floor", 60,
                "60 systems, Java 21, MySQL, Eclipse and VS Code"));
        resources.save(new CampusResource("LAB-2", ResourceType.LAB, "CSE Block, 2nd floor", 60,
                "60 systems, used for 3rd-semester labs"));
        resources.save(new CampusResource("CR-201", ResourceType.CLASSROOM, "CSE Block, 2nd floor", 70,
                "Smart board and projector"));
        CampusResource hall = resources.save(new CampusResource("Seminar Hall", ResourceType.HALL, "Admin Block, ground floor", 200,
                "Audio system, projector, stage"));
        CampusResource audi = resources.save(new CampusResource("Main Auditorium", ResourceType.HALL, "Main Block", 600,
                "Large events and convocation"));
        CampusResource arduino = resources.save(new CampusResource("Arduino Kit Set", ResourceType.EQUIPMENT, "IDEA Lab store", 10,
                "Arduino Uno kits with sensors, for PBL prototypes"));
        resources.save(new CampusResource("Projector Kit 1", ResourceType.EQUIPMENT, "CSE department office", 1,
                "Portable projector with HDMI cable"));
        resources.save(new CampusResource("DSLR Camera", ResourceType.EQUIPMENT, "Media club room", 1,
                "Canon DSLR with tripod, for event coverage"));

        AppUser f1 = users.findByUsernameIgnoreCase("F001").orElseThrow();
        AppUser f2 = users.findByUsernameIgnoreCase("F002").orElseThrow();
        AppUser admin = users.findByUsernameIgnoreCase("admin").orElseThrow();
        AppUser hemanth = users.findByUsernameIgnoreCase("U25CS001").orElseThrow();
        bookings.save(new Booking(hall, workingDay(1), 2, f1, "OOPS guest talk for 3rd semester"));
        bookings.save(new Booking(lab1, workingDay(1), 5, f2, "Data Structures extra lab session"));
        bookings.save(new Booking(lab1, workingDay(2), 6, f1, "PBL review: Smart Campus project"));
        bookings.save(new Booking(arduino, workingDay(2), 5, hemanth, "PBL prototype testing"));
        bookings.save(new Booking(audi, workingDay(6), 1, admin, "CodeSprint Hackathon opening"));
        bookings.save(new Booking(audi, workingDay(6), 2, admin, "CodeSprint Hackathon opening"));
    }

    /** The day n days from now, moved to Monday if it falls on a Sunday. */
    private LocalDate workingDay(int n) {
        LocalDate d = LocalDate.now().plusDays(n);
        return d.getDayOfWeek() == DayOfWeek.SUNDAY ? d.plusDays(1) : d;
    }

    private Notice dated(Notice n, int daysAgo) {
        n.setPostedOn(LocalDate.now().minusDays(daysAgo));
        return n;
    }
}
