package in.neonjava.erp.web.admin;

import in.neonjava.erp.model.Course;
import in.neonjava.erp.model.Department;
import in.neonjava.erp.model.Notice;
import in.neonjava.erp.repo.CourseRepository;
import in.neonjava.erp.repo.DepartmentRepository;
import in.neonjava.erp.repo.NoticeRepository;
import in.neonjava.erp.service.TimetableService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;

/** Smaller admin screens: departments, notices and the timetable editor. */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final DepartmentRepository departments;
    private final NoticeRepository notices;
    private final CourseRepository courses;
    private final TimetableService timetable;

    public AdminController(DepartmentRepository departments, NoticeRepository notices,
                           CourseRepository courses, TimetableService timetable) {
        this.departments = departments;
        this.notices = notices;
        this.courses = courses;
        this.timetable = timetable;
    }

    // ---------- Departments ----------

    @GetMapping("/departments")
    public String departments(Model model) {
        model.addAttribute("departments", departments.findAllByOrderByCode());
        return "admin/departments";
    }

    @PostMapping("/departments")
    public String addDepartment(@RequestParam String code, @RequestParam String name, RedirectAttributes ra) {
        String c = code.trim().toUpperCase();
        if (c.isEmpty() || name.isBlank()) {
            ra.addFlashAttribute("error", "Code and name are required");
        } else if (departments.findByCode(c).isPresent()) {
            ra.addFlashAttribute("error", "Department " + c + " already exists");
        } else {
            departments.save(new Department(c, name.trim()));
            ra.addFlashAttribute("success", "Added " + c);
        }
        return "redirect:/admin/departments";
    }

    // ---------- Notices ----------

    @GetMapping("/notices")
    public String notices(Model model) {
        model.addAttribute("notices", notices.findAllByOrderByPostedOnDescIdDesc());
        return "admin/notices";
    }

    @PostMapping("/notices")
    public String addNotice(@RequestParam String title, @RequestParam String body, RedirectAttributes ra) {
        if (title.isBlank()) {
            ra.addFlashAttribute("error", "Title is required");
        } else {
            notices.save(new Notice(title.trim(), body.trim()));
            ra.addFlashAttribute("success", "Notice posted");
        }
        return "redirect:/admin/notices";
    }

    @PostMapping("/notices/{id}/delete")
    public String deleteNotice(@PathVariable Long id, RedirectAttributes ra) {
        notices.deleteById(id);
        ra.addFlashAttribute("success", "Notice deleted");
        return "redirect:/admin/notices";
    }

    // ---------- Timetable ----------

    @GetMapping("/timetable")
    public String timetable(@RequestParam(defaultValue = "3") int semester,
                            @RequestParam(defaultValue = "A") String section, Model model) {
        String sec = section.trim().toUpperCase();
        model.addAttribute("semester", semester);
        model.addAttribute("section", sec);
        model.addAttribute("grid", timetable.grid(semester, sec));
        model.addAttribute("days", TimetableService.WORKING_DAYS);
        model.addAttribute("times", TimetableService.PERIOD_TIMES);
        model.addAttribute("courses", courses.findAllByOrderByCode().stream()
                .filter(c -> c.getSemester() == semester).toList());
        return "admin/timetable";
    }

    @PostMapping("/timetable")
    public String setSlot(@RequestParam int semester, @RequestParam String section,
                          @RequestParam DayOfWeek day, @RequestParam int period,
                          @RequestParam(required = false) Long courseId,
                          @RequestParam(required = false) String room, RedirectAttributes ra) {
        String sec = section.trim().toUpperCase();
        try {
            if (courseId == null) {
                timetable.clearSlot(semester, sec, day, period);
                ra.addFlashAttribute("success", "Cleared " + day + " period " + period);
            } else {
                Course c = courses.findById(courseId).orElseThrow();
                timetable.setSlot(semester, sec, day, period, c, room == null ? "" : room.trim());
                ra.addFlashAttribute("success", "Set " + day + " period " + period + " to " + c.getCode());
            }
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/timetable?semester=" + semester + "&section=" + sec;
    }
}
