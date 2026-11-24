package in.neonjava.erp.security;

import in.neonjava.erp.model.AppUser;
import in.neonjava.erp.repo.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/** Counts wrong passwords. Five in a row locks the account for 15 minutes (the admin can unlock it sooner). */
@Service
public class LoginAttemptService {

    public static final int MAX_TRIES = 5;
    public static final Duration LOCK_TIME = Duration.ofMinutes(15);

    private final UserRepository users;

    public LoginAttemptService(UserRepository users) {
        this.users = users;
    }

    /** Records a wrong password. Returns true when this attempt locked the account. */
    @Transactional
    public boolean failed(String username) {
        AppUser u = find(username);
        if (u == null || u.isLocked()) return false;
        int tries = u.getFailedLogins() + 1;
        boolean lock = tries >= MAX_TRIES;
        u.setFailedLogins(lock ? 0 : tries);
        if (lock) u.setLockedUntil(LocalDateTime.now().plus(LOCK_TIME));
        users.save(u);
        return lock;
    }

    @Transactional
    public void succeeded(String username) {
        AppUser u = find(username);
        if (u == null) return;
        u.setFailedLogins(0);
        u.setLockedUntil(null);
        u.setLastLogin(LocalDateTime.now());
        users.save(u);
    }

    /** Tries left before a lock; unknown usernames always report the full count. */
    public int triesLeft(String username) {
        AppUser u = find(username);
        return u == null ? MAX_TRIES : MAX_TRIES - u.getFailedLogins();
    }

    private AppUser find(String username) {
        return username == null ? null : users.findByUsernameIgnoreCase(username.trim()).orElse(null);
    }
}
