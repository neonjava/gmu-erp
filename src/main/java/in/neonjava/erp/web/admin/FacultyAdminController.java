package in.neonjava.erp.web.admin;

import in.neonjava.erp.model.Faculty;
import in.neonjava.erp.repo.DepartmentRepository;
import in.neonjava.erp.repo.FacultyRepository;
import in.neonjava.erp.repo.UserRepository;
import in.neonjava.erp.service.AccountService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/faculty")
public class FacultyAdminController {

    private final FacultyRepository faculty;
    private final DepartmentRepository departments;
    private final UserRepository users;
    private final AccountService accounts;

    public FacultyAdminController(FacultyRepository faculty, DepartmentRepository departments,
                                  UserRepository users, AccountService accounts) {
        this.faculty = faculty;
        this.departments = departments;
        this.users = users;
        this.accounts = accounts;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("q", q);
        model.addAttribute("faculty", q == null || q.isBlank()
                ? faculty.findAllByOrderByEmpId()
                : faculty.findByNameContainingIgnoreCaseOrEmpIdContainingIgnoreCaseOrderByEmpId(q.trim(), q.trim()));
        return "admin/faculty";
    }

    @GetMapping("/new")
    public String create(Model model) {
        model.addAttribute("f", new Faculty());
        model.addAttribute("departments", departments.findAllByOrderByCode());
        return "admin/faculty-form";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("f", faculty.findById(id).orElseThrow());
        model.addAttribute("departments", departments.findAllByOrderByCode());
        return "admin/faculty-form";
    }

    @PostMapping
    public String save(@RequestParam(required = false) Long id,
                       @RequestParam(required = false) String empId,
                       @RequestParam String name,
                       @RequestParam(required = false) String designation,
                       @RequestParam(required = false) String email,
                       @RequestParam(required = false) String phone,
                       @RequestParam(required = false) Long departmentId,
                       RedirectAttributes ra) {
        Faculty f = new Faculty();
        f.setId(id);
        f.setEmpId(empId);
        f.setName(name.trim());
        f.setDesignation(designation);
        f.setEmail(email);
        f.setPhone(phone);
        f.setDepartment(departmentId == null ? null : departments.findById(departmentId).orElse(null));
        try {
            Faculty saved = accounts.saveFaculty(f);
            ra.addFlashAttribute("success", id == null
                    ? "Added " + saved.getEmpId() + ". Login: " + saved.getEmpId() + " / " + AccountService.DEFAULT_FACULTY_PASSWORD
                    : "Updated " + saved.getEmpId());
            return "redirect:/admin/faculty";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return id == null ? "redirect:/admin/faculty/new" : "redirect:/admin/faculty/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        accounts.deleteFaculty(id);
        ra.addFlashAttribute("success", "Faculty deleted; their courses are now unassigned");
        return "redirect:/admin/faculty";
    }

    @PostMapping("/{id}/reset-password")
    public String resetPassword(@PathVariable Long id, RedirectAttributes ra) {
        Faculty f = faculty.findById(id).orElseThrow();
        users.findByUsernameIgnoreCase(f.getEmpId()).ifPresent(accounts::resetPassword);
        ra.addFlashAttribute("success", "Password for " + f.getEmpId() + " reset to " + AccountService.DEFAULT_FACULTY_PASSWORD);
        return "redirect:/admin/faculty";
    }
}
