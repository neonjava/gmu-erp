package in.neonjava.erp;

import in.neonjava.erp.model.AppUser;
import in.neonjava.erp.model.Role;
import in.neonjava.erp.model.Student;
import in.neonjava.erp.repo.StudentRepository;
import in.neonjava.erp.repo.UserRepository;
import in.neonjava.erp.security.LoginAttemptService;
import in.neonjava.erp.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import({LoginAttemptService.class, AccountService.class, BCryptPasswordEncoder.class})
class LoginManagementTest {

    @Autowired LoginAttemptService attempts;
    @Autowired AccountService accounts;
    @Autowired UserRepository users;
    @Autowired StudentRepository students;

    private AppUser newStudent(String usn) {
        Student s = new Student();
        s.setUsn(usn);
        s.setName("Student " + usn);
        accounts.saveStudent(s);
        return users.findByUsernameIgnoreCase(usn).orElseThrow();
    }

    @Test
    void newAccountMustSetItsOwnPassword() {
        AppUser u = newStudent("U99CS001");
        assertTrue(u.isMustChangePassword());
        assertEquals("New", u.getStatus());

        accounts.changePassword(u, "student123", "my-new-pass", "my-new-pass");
        assertFalse(u.isMustChangePassword());
        assertEquals("Active", u.getStatus());
    }

    @Test
    void newPasswordMustDifferFromTheOldOne() {
        AppUser u = newStudent("U99CS002");
        assertThrows(IllegalArgumentException.class,
                () -> accounts.changePassword(u, "student123", "student123", "student123"));
    }

    @Test
    void fiveWrongPasswordsLockTheAccount() {
        AppUser u = newStudent("U99CS003");
        for (int i = 1; i < LoginAttemptService.MAX_TRIES; i++) assertFalse(attempts.failed("u99cs003"));
        assertEquals(1, attempts.triesLeft("U99CS003"));
        assertTrue(attempts.failed("U99CS003"));
        assertTrue(users.findById(u.getId()).orElseThrow().isLocked());
    }

    @Test
    void successResetsTheCountAndRecordsTheTime() {
        AppUser u = newStudent("U99CS004");
        attempts.failed("U99CS004");
        attempts.failed("U99CS004");
        attempts.succeeded("U99CS004");
        AppUser after = users.findById(u.getId()).orElseThrow();
        assertEquals(0, after.getFailedLogins());
        assertNotNull(after.getLastLogin());
    }

    @Test
    void resetUnlocksAndAsksForANewPassword() {
        AppUser u = newStudent("U99CS005");
        accounts.changePassword(u, "student123", "abcdef1", "abcdef1");
        for (int i = 0; i < LoginAttemptService.MAX_TRIES; i++) attempts.failed("U99CS005");
        AppUser locked = users.findById(u.getId()).orElseThrow();
        assertEquals("Locked", locked.getStatus());

        accounts.resetPassword(locked);
        assertFalse(locked.isLocked());
        assertTrue(locked.isMustChangePassword());
    }

    @Test
    void adminCannotBeTurnedOff() {
        AppUser admin = users.save(new AppUser("root", "x", Role.ADMIN, "Admin"));
        assertThrows(IllegalArgumentException.class, () -> accounts.setEnabled(admin, false));
        AppUser s = newStudent("U99CS006");
        accounts.setEnabled(s, false);
        assertEquals("Off", s.getStatus());
    }
}
