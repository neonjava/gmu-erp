package in.neonjava.erp.api;

import in.neonjava.erp.model.*;
import in.neonjava.erp.repo.CourseRepository;
import in.neonjava.erp.repo.RegistrationRepository;
import in.neonjava.erp.security.CurrentUserService;
import in.neonjava.erp.service.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/student")
public class StudentApi {

    private final CurrentUserService currentUser;
    private final TimetableService timetable;
    private final AttendanceService attendance;
    private final ResultService results;
    private final CourseService courseService;
    private final CourseRepository courses;
    private final RegistrationRepository registrations;

    public StudentApi(CurrentUserService currentUser, TimetableService timetable, AttendanceService attendance,
                      ResultService results, CourseService courseService, CourseRepository courses,
                      RegistrationRepository registrations) {
        this.currentUser = currentUser;
        this.timetable = timetable;
        this.attendance = attendance;
        this.results = results;
        this.courseService = courseService;
        this.courses = courses;
        this.registrations = registrations;
    }

    @GetMapping("/attendance")
    public Map<String, Object> attendance() {
        return Map.of("required", AttendanceService.REQUIRED_PERCENT,
                "rows", attendance.summaryFor(currentUser.requireStudent()).stream().map(Dto.AttendanceRow::of).toList());
    }

    @GetMapping("/results")
    public Map<String, Object> results() {
        List<Mark> rows = results.marksFor(currentUser.requireStudent());
        Map<String, Object> out = new HashMap<>();
        out.put("sgpa", results.sgpa(rows));
        out.put("rows", rows.stream().map(Dto.ResultRow::of).toList());
        return out;
    }

    @GetMapping("/timetable")
    public Dto.Timetable timetable() {
        Student s = currentUser.requireStudent();
        List<Dto.Slot> slots = timetable.grid(s.getSemester(), s.getSection()).values().stream()
                .map(t -> new Dto.Slot(t.getDayOfWeek().name(), t.getPeriod(), t.getCourse().getCode(),
                        t.getCourse().getName(), t.getRoom()))
                .toList();
        return new Dto.Timetable("Semester " + s.getSemester() + ", section " + s.getSection(),
                TimetableService.WORKING_DAYS.stream().map(Enum::name).toList(), TimetableService.PERIOD_TIMES, slots);
    }

    /** One entry per course offered to this student; the pop-up opens any of them by code. */
    @GetMapping("/subjects")
    public List<Dto.Subject> subjects() {
        Student s = currentUser.requireStudent();
        Set<Long> registered = registrations.findByStudent(s).stream()
                .map(Registration::getCourse).map(Course::getId).collect(Collectors.toSet());
        Map<String, AttendanceService.Summary> att = new HashMap<>();
        attendance.summaryFor(s).forEach(x -> att.put(x.course().getCode(), x));
        Map<String, Mark> marks = new HashMap<>();
        results.marksFor(s).forEach(m -> marks.put(m.getCourse().getCode(), m));

        List<String> times = TimetableService.PERIOD_TIMES;
        Map<String, List<Dto.Meeting>> meetings = new HashMap<>();
        timetable.grid(s.getSemester(), s.getSection()).values().stream()
                .sorted(Comparator.comparing((TimetableSlot t) -> t.getDayOfWeek()).thenComparing(TimetableSlot::getPeriod))
                .forEach(t -> meetings.computeIfAbsent(t.getCourse().getCode(), k -> new ArrayList<>())
                        .add(new Dto.Meeting(t.getDayOfWeek().name(), t.getPeriod(), times.get(t.getPeriod() - 1), t.getRoom())));

        return courses.findByDepartmentAndSemesterOrderByCode(s.getDepartment(), s.getSemester()).stream().map(c -> {
            var a = att.get(c.getCode());
            var m = marks.get(c.getCode());
            Faculty f = c.getFaculty();
            return new Dto.Subject(c.getId(), c.getCode(), c.getName(), c.getCredits(),
                    f == null ? null : f.getName(), f == null ? null : f.getDesignation(),
                    registered.contains(c.getId()), meetings.getOrDefault(c.getCode(), List.of()),
                    a == null ? null : a.attended(), a == null ? null : a.total(), a == null ? null : a.percent(),
                    a == null ? null : a.shortage(), a == null ? null : a.classesNeeded(),
                    m == null ? null : m.getCie1(), m == null ? null : m.getCie2(), m == null ? null : m.getCieTotal(),
                    m == null ? null : m.getSee(), m == null || m.getSee() == null ? null : m.getTotal(),
                    m == null || m.getSee() == null ? null : m.getGrade());
        }).toList();
    }

    @GetMapping("/registration")
    public List<Dto.OfferedCourse> registration() {
        Student s = currentUser.requireStudent();
        Set<Long> registered = registrations.findByStudent(s).stream()
                .map(Registration::getCourse).map(Course::getId).collect(Collectors.toSet());
        return courses.findByDepartmentAndSemesterOrderByCode(s.getDepartment(), s.getSemester()).stream()
                .map(c -> new Dto.OfferedCourse(Dto.CourseRow.of(c), registered.contains(c.getId())))
                .toList();
    }

    @PostMapping("/registration/{courseId}")
    public Map<String, String> register(@PathVariable Long courseId) {
        Course c = courses.findById(courseId).orElseThrow();
        courseService.register(currentUser.requireStudent(), c);
        return Map.of("message", "Registered for " + c.getCode());
    }

    @DeleteMapping("/registration/{courseId}")
    public Map<String, String> drop(@PathVariable Long courseId) {
        Course c = courses.findById(courseId).orElseThrow();
        courseService.drop(currentUser.requireStudent(), c);
        return Map.of("message", "Dropped " + c.getCode());
    }
}
