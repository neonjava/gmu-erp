package in.neonjava.erp.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Serves the React app for every /app route (React Router picks the page). Files with a dot are real assets. */
@Controller
public class SpaController {

    @GetMapping({"/app", "/app/", "/app/{a:[^.]*}", "/app/{a:[^.]*}/{b:[^.]*}", "/app/{a:[^.]*}/{b:[^.]*}/{c:[^.]*}"})
    public String app() {
        return "forward:/app/index.html";
    }
}
