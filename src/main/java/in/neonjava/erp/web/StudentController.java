package in.neonjava.erp.web;

import in.neonjava.erp.model.Course;
import in.neonjava.erp.model.Mark;
import in.neonjava.erp.model.Registration;
import in.neonjava.erp.model.Student;
import in.neonjava.erp.repo.CourseRepository;
import in.neonjava.erp.repo.RegistrationRepository;
import in.neonjava.erp.security.CurrentUserService;
import in.neonjava.erp.service.AttendanceService;
import in.neonjava.erp.service.CourseService;
import in.neonjava.erp.service.ResultService;
import in.neonjava.erp.service.TimetableService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/student")
public class StudentController {

    private final CurrentUserService currentUser;
    private final TimetableService timetable;
    private final AttendanceService attendance;
    private final ResultService results;
    private final CourseService courseService;
    private final CourseRepository courses;
    private final RegistrationRepository registrations;

    public StudentController(CurrentUserService currentUser, TimetableService timetable, AttendanceService attendance,
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

    @GetMapping("/timetable")
    public String timetable(Model model) {
        Student s = currentUser.requireStudent();
        model.addAttribute("grid", timetable.grid(s.getSemester(), s.getSection()));
        model.addAttribute("days", TimetableService.WORKING_DAYS);
        model.addAttribute("times", TimetableService.PERIOD_TIMES);
        model.addAttribute("title", "Semester " + s.getSemester() + " · Section " + s.getSection());
        return "student/timetable";
    }

    @GetMapping("/attendance")
    public String attendance(Model model) {
        model.addAttribute("rows", attendance.summaryFor(currentUser.requireStudent()));
        model.addAttribute("required", AttendanceService.REQUIRED_PERCENT);
        return "student/attendance";
    }

    @GetMapping("/results")
    public String results(Model model) {
        List<Mark> rows = results.marksFor(currentUser.requireStudent());
        model.addAttribute("rows", rows);
        model.addAttribute("sgpa", results.sgpa(rows));
        return "student/results";
    }

    @GetMapping("/registration")
    public String registration(Model model) {
        Student s = currentUser.requireStudent();
        Set<Long> registered = registrations.findByStudent(s).stream()
                .map(Registration::getCourse).map(Course::getId).collect(Collectors.toSet());
        model.addAttribute("offered", courses.findByDepartmentAndSemesterOrderByCode(s.getDepartment(), s.getSemester()));
        model.addAttribute("registered", registered);
        return "student/registration";
    }

    @PostMapping("/registration/{courseId}/{action}")
    public String register(@PathVariable Long courseId, @PathVariable String action, RedirectAttributes ra) {
        Student s = currentUser.requireStudent();
        Course c = courses.findById(courseId).orElseThrow();
        try {
            if ("drop".equals(action)) {
                courseService.drop(s, c);
                ra.addFlashAttribute("success", "Dropped " + c.getCode());
            } else {
                courseService.register(s, c);
                ra.addFlashAttribute("success", "Registered for " + c.getCode());
            }
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/student/registration";
    }
}
