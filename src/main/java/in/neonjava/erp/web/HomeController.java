package in.neonjava.erp.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Pages that moved to the React app (under /app). The old addresses redirect there, keeping the query string,
 * so bookmarks and links from the server-rendered pages still work.
 */
@Controller
public class HomeController {

    @GetMapping({"/", "/login", "/profile", "/notices",
            "/student/attendance", "/student/timetable", "/student/results", "/student/registration",
            "/admin/students"})
    public String moved(HttpServletRequest req) {
        String path = req.getRequestURI().substring(req.getContextPath().length());
        String q = req.getQueryString();
        return "redirect:/app" + path + (q == null ? "" : "?" + q);
    }
}
