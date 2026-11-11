package in.neonjava.erp.web;

import in.neonjava.erp.model.Course;
import in.neonjava.erp.model.Faculty;
import in.neonjava.erp.repo.CourseRepository;
import in.neonjava.erp.repo.RegistrationRepository;
import in.neonjava.erp.security.CurrentUserService;
import in.neonjava.erp.service.AttendanceService;
import in.neonjava.erp.service.ResultService;
import in.neonjava.erp.service.TimetableService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/faculty")
public class FacultyController {

    private final CurrentUserService currentUser;
    private final CourseRepository courses;
    private final RegistrationRepository registrations;
    private final AttendanceService attendance;
    private final ResultService results;
    private final TimetableService timetable;

    public FacultyController(CurrentUserService currentUser, CourseRepository courses,
                             RegistrationRepository registrations, AttendanceService attendance,
                             ResultService results, TimetableService timetable) {
        this.currentUser = currentUser;
        this.courses = courses;
        this.registrations = registrations;
        this.attendance = attendance;
        this.results = results;
        this.timetable = timetable;
    }

    /** Loads a course and checks that the logged-in faculty teaches it. */
    private Course myCourse(Long id) {
        Faculty me = currentUser.requireFaculty();
        Course c = courses.findById(id).orElseThrow(() -> new IllegalArgumentException("Course not found"));
        if (c.getFaculty() == null || !c.getFaculty().getId().equals(me.getId())) {
            throw new AccessDeniedException("You do not teach " + c.getCode());
        }
        return c;
    }

    @GetMapping("/courses")
    public String courses(Model model) {
        List<Course> mine = courses.findByFacultyOrderByCode(currentUser.requireFaculty());
        Map<Long, Long> counts = new LinkedHashMap<>();
        for (Course c : mine) counts.put(c.getId(), registrations.countByCourse(c));
        model.addAttribute("courses", mine);
        model.addAttribute("counts", counts);
        return "faculty/courses";
    }

    @GetMapping("/timetable")
    public String timetable(Model model) {
        List<Course> mine = courses.findByFacultyOrderByCode(currentUser.requireFaculty());
        model.addAttribute("grid", timetable.gridForCourses(mine));
        model.addAttribute("days", TimetableService.WORKING_DAYS);
        model.addAttribute("times", TimetableService.PERIOD_TIMES);
        return "faculty/timetable";
    }

    @GetMapping("/courses/{id}/attendance")
    public String attendanceForm(@PathVariable Long id,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                 Model model) {
        Course c = myCourse(id);
        LocalDate d = date == null ? LocalDate.now() : date;
        boolean taken = attendance.isTaken(c, d);
        model.addAttribute("course", c);
        model.addAttribute("date", d);
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("students", attendance.studentsIn(c));
        model.addAttribute("present", attendance.presentOn(c, d));
        model.addAttribute("taken", taken);
        return "faculty/attendance";
    }

    @PostMapping("/courses/{id}/attendance")
    public String saveAttendance(@PathVariable Long id,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                 @RequestParam(name = "present", required = false) List<Long> present,
                                 RedirectAttributes ra) {
        Course c = myCourse(id);
        try {
            int n = attendance.mark(c, date, present);
            int p = present == null ? 0 : present.size();
            ra.addFlashAttribute("success", "Saved attendance for " + n + " students (" + p + " present, " + (n - p) + " absent)");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/faculty/courses/" + id + "/attendance?date=" + date;
    }

    @GetMapping("/courses/{id}/marks")
    public String marksForm(@PathVariable Long id, Model model) {
        Course c = myCourse(id);
        model.addAttribute("course", c);
        model.addAttribute("rows", results.sheetFor(c));
        return "faculty/marks";
    }

    @PostMapping("/courses/{id}/marks")
    public String saveMarks(@PathVariable Long id, @RequestParam Map<String, String> form, RedirectAttributes ra) {
        Course c = myCourse(id);
        try {
            results.saveSheet(c, form);
            ra.addFlashAttribute("success", "Marks saved");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage() + ". Nothing was saved.");
        }
        return "redirect:/faculty/courses/" + id + "/marks";
    }
}
