package in.neonjava.erp.web.admin;

import in.neonjava.erp.model.Course;
import in.neonjava.erp.repo.CourseRepository;
import in.neonjava.erp.repo.DepartmentRepository;
import in.neonjava.erp.repo.FacultyRepository;
import in.neonjava.erp.repo.RegistrationRepository;
import in.neonjava.erp.service.CourseService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/courses")
public class CourseAdminController {

    private final CourseRepository courses;
    private final DepartmentRepository departments;
    private final FacultyRepository faculty;
    private final RegistrationRepository registrations;
    private final CourseService courseService;

    public CourseAdminController(CourseRepository courses, DepartmentRepository departments, FacultyRepository faculty,
                                 RegistrationRepository registrations, CourseService courseService) {
        this.courses = courses;
        this.departments = departments;
        this.faculty = faculty;
        this.registrations = registrations;
        this.courseService = courseService;
    }

    @GetMapping
    public String list(Model model) {
        List<Course> all = courses.findAllByOrderByCode();
        Map<Long, Long> counts = new HashMap<>();
        for (Course c : all) counts.put(c.getId(), registrations.countByCourse(c));
        model.addAttribute("courses", all);
        model.addAttribute("counts", counts);
        return "admin/courses";
    }

    @GetMapping("/new")
    public String create(Model model) {
        return form(new Course(), model);
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        return form(courses.findById(id).orElseThrow(), model);
    }

    private String form(Course c, Model model) {
        model.addAttribute("course", c);
        model.addAttribute("departments", departments.findAllByOrderByCode());
        model.addAttribute("faculty", faculty.findAllByOrderByEmpId());
        return "admin/course-form";
    }

    @PostMapping
    public String save(@RequestParam(required = false) Long id,
                       @RequestParam String code,
                       @RequestParam String name,
                       @RequestParam(defaultValue = "3") int credits,
                       @RequestParam(defaultValue = "1") int semester,
                       @RequestParam(required = false) Long departmentId,
                       @RequestParam(required = false) Long facultyId,
                       RedirectAttributes ra) {
        Course c = id == null ? new Course() : courses.findById(id).orElseThrow();
        c.setCode(code);
        c.setName(name.trim());
        c.setCredits(credits);
        c.setSemester(semester);
        c.setDepartment(departmentId == null ? null : departments.findById(departmentId).orElse(null));
        c.setFaculty(facultyId == null ? null : faculty.findById(facultyId).orElse(null));
        try {
            courseService.save(c);
            ra.addFlashAttribute("success", "Saved " + c.getCode());
            return "redirect:/admin/courses";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return id == null ? "redirect:/admin/courses/new" : "redirect:/admin/courses/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        courseService.delete(id);
        ra.addFlashAttribute("success", "Course deleted with its registrations, attendance and marks");
        return "redirect:/admin/courses";
    }
}
