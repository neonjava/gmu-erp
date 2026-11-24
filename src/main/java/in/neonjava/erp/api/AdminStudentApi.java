package in.neonjava.erp.api;

import in.neonjava.erp.model.AppUser;
import in.neonjava.erp.model.Role;
import in.neonjava.erp.model.Student;
import in.neonjava.erp.repo.StudentRepository;
import in.neonjava.erp.repo.UserRepository;
import in.neonjava.erp.service.AccountService;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/** Student accounts for the admin: list with sign-in status, and unlock / reset / turn off / turn on / delete. */
@RestController
@RequestMapping("/api/admin/students")
public class AdminStudentApi {

    private final StudentRepository students;
    private final UserRepository users;
    private final AccountService accounts;

    public AdminStudentApi(StudentRepository students, UserRepository users, AccountService accounts) {
        this.students = students;
        this.users = users;
        this.accounts = accounts;
    }

    @GetMapping
    public List<Dto.StudentAccount> list(@RequestParam(required = false) String q) {
        Map<Long, AppUser> logins = new HashMap<>();
        for (AppUser u : users.findByRole(Role.STUDENT)) {
            if (u.getStudent() != null) logins.put(u.getStudent().getId(), u);
        }
        List<Student> found = q == null || q.isBlank()
                ? students.findAllByOrderByUsn()
                : students.findByNameContainingIgnoreCaseOrUsnContainingIgnoreCaseOrderByUsn(q.trim(), q.trim());
        return found.stream().map(s -> {
            AppUser u = logins.get(s.getId());
            return u == null
                    ? new Dto.StudentAccount(Dto.StudentInfo.of(s), "None", null, null, false, false)
                    : new Dto.StudentAccount(Dto.StudentInfo.of(s), u.getStatus(), u.getLastLogin(),
                    u.getLockedUntil(), u.isEnabled(), u.isLocked());
        }).toList();
    }

    @PostMapping("/{id}/{action}")
    public Map<String, String> act(@PathVariable Long id, @PathVariable String action) {
        Student s = students.findById(id).orElseThrow();
        AppUser u = users.findByStudent(s).orElseThrow(() -> new IllegalArgumentException(s.getUsn() + " has no login account"));
        String msg = switch (action) {
            case "unlock" -> { accounts.unlock(u); yield "Unlocked " + s.getUsn(); }
            case "reset-password" -> {
                accounts.resetPassword(u);
                yield "Password for " + s.getUsn() + " reset to " + AccountService.DEFAULT_STUDENT_PASSWORD
                        + ". They'll pick a new one when they sign in.";
            }
            case "disable" -> { accounts.setEnabled(u, false); yield "Turned off sign-in for " + s.getUsn(); }
            case "enable" -> { accounts.setEnabled(u, true); yield "Turned on sign-in for " + s.getUsn(); }
            default -> throw new IllegalArgumentException("Unknown action " + action);
        };
        return Map.of("message", msg);
    }

    @DeleteMapping("/{id}")
    public Map<String, String> delete(@PathVariable Long id) {
        String usn = students.findById(id).orElseThrow().getUsn();
        accounts.deleteStudent(id);
        return Map.of("message", "Deleted " + usn);
    }
}
