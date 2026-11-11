package in.neonjava.erp.web.admin;

import in.neonjava.erp.model.Student;
import in.neonjava.erp.repo.DepartmentRepository;
import in.neonjava.erp.repo.StudentRepository;
import in.neonjava.erp.repo.UserRepository;
import in.neonjava.erp.service.AccountService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/students")
public class StudentAdminController {

    private final StudentRepository students;
    private final DepartmentRepository departments;
    private final UserRepository users;
    private final AccountService accounts;

    public StudentAdminController(StudentRepository students, DepartmentRepository departments,
                                  UserRepository users, AccountService accounts) {
        this.students = students;
        this.departments = departments;
        this.users = users;
        this.accounts = accounts;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("q", q);
        model.addAttribute("students", q == null || q.isBlank()
                ? students.findAllByOrderByUsn()
                : students.findByNameContainingIgnoreCaseOrUsnContainingIgnoreCaseOrderByUsn(q.trim(), q.trim()));
        return "admin/students";
    }

    @GetMapping("/new")
    public String create(Model model) {
        Student s = new Student();
        s.setSemester(1);
        s.setSection("A");
        model.addAttribute("student", s);
        model.addAttribute("departments", departments.findAllByOrderByCode());
        return "admin/student-form";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("student", students.findById(id).orElseThrow());
        model.addAttribute("departments", departments.findAllByOrderByCode());
        return "admin/student-form";
    }

    @PostMapping
    public String save(@RequestParam(required = false) Long id,
                       @RequestParam(required = false) String usn,
                       @RequestParam String name,
                       @RequestParam(required = false) String email,
                       @RequestParam(required = false) String phone,
                       @RequestParam(required = false) Long departmentId,
                       @RequestParam(defaultValue = "1") int semester,
                       @RequestParam(defaultValue = "A") String section,
                       RedirectAttributes ra) {
        Student s = new Student();
        s.setId(id);
        s.setUsn(usn);
        s.setName(name.trim());
        s.setEmail(email);
        s.setPhone(phone);
        s.setDepartment(departmentId == null ? null : departments.findById(departmentId).orElse(null));
        s.setSemester(semester);
        s.setSection(section.trim().toUpperCase());
        try {
            Student saved = accounts.saveStudent(s);
            ra.addFlashAttribute("success", id == null
                    ? "Added " + saved.getUsn() + ". Login: " + saved.getUsn() + " / " + AccountService.DEFAULT_STUDENT_PASSWORD
                    : "Updated " + saved.getUsn());
            return "redirect:/admin/students";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return id == null ? "redirect:/admin/students/new" : "redirect:/admin/students/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        accounts.deleteStudent(id);
        ra.addFlashAttribute("success", "Student deleted");
        return "redirect:/admin/students";
    }

    @PostMapping("/{id}/reset-password")
    public String resetPassword(@PathVariable Long id, RedirectAttributes ra) {
        Student s = students.findById(id).orElseThrow();
        users.findByUsernameIgnoreCase(s.getUsn()).ifPresent(accounts::resetPassword);
        ra.addFlashAttribute("success", "Password for " + s.getUsn() + " reset to " + AccountService.DEFAULT_STUDENT_PASSWORD);
        return "redirect:/admin/students";
    }
}
