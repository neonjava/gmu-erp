package in.neonjava.erp.security;

import in.neonjava.erp.model.AppUser;
import in.neonjava.erp.model.Faculty;
import in.neonjava.erp.model.Student;
import in.neonjava.erp.repo.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private final UserRepository users;

    public CurrentUserService(UserRepository users) {
        this.users = users;
    }

    /** The logged-in user, or null when nobody is logged in. */
    public AppUser get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return users.findByUsernameIgnoreCase(auth.getName()).orElse(null);
    }

    public Student requireStudent() {
        AppUser u = get();
        if (u == null || u.getStudent() == null) throw new AccessDeniedException("Not a student account");
        return u.getStudent();
    }

    public Faculty requireFaculty() {
        AppUser u = get();
        if (u == null || u.getFaculty() == null) throw new AccessDeniedException("Not a faculty account");
        return u.getFaculty();
    }
}
