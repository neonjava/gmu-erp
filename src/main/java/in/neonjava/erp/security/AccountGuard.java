package in.neonjava.erp.security;

import in.neonjava.erp.model.AppUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Runs before every page and API call. Signs out accounts the admin has turned off, and blocks people
 * who are still on the default password until they set their own (the app shows them the password screen).
 */
@Component
public class AccountGuard implements HandlerInterceptor {

    private final CurrentUserService currentUser;

    public AccountGuard(CurrentUserService currentUser) {
        this.currentUser = currentUser;
    }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
        AppUser u = currentUser.get();
        if (u == null) return true;
        boolean api = SecurityConfig.isApi(req);
        if (!u.isEnabled()) {
            HttpSession s = req.getSession(false);
            if (s != null) s.invalidate();
            SecurityContextHolder.clearContext();
            if (api) res.sendError(401);
            else res.sendRedirect(req.getContextPath() + "/app/login?error=disabled");
            return false;
        }
        if (u.isMustChangePassword()) {
            if (api) {
                res.setStatus(403);
                res.setContentType("application/json");
                res.getWriter().write("{\"message\":\"Set your own password first\"}");
            } else {
                res.sendRedirect(req.getContextPath() + "/app/");
            }
            return false;
        }
        return true;
    }
}
