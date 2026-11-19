package in.neonjava.erp.web;

import in.neonjava.erp.model.AppUser;
import in.neonjava.erp.model.Mark;
import in.neonjava.erp.repo.*;
import in.neonjava.erp.security.CurrentUserService;
import in.neonjava.erp.service.AccountService;
import in.neonjava.erp.service.AttendanceService;
import in.neonjava.erp.service.EventService;
import in.neonjava.erp.service.ResultService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalTime;
import java.util.List;

@Controller
public class HomeController {

    private final CurrentUserService currentUser;
    private final NoticeRepository notices;
    private final StudentRepository students;
    private final FacultyRepository faculty;
    private final CourseRepository courses;
    private final AttendanceService attendanceService;
    private final ResultService resultService;
    private final AccountService accounts;
    private final EventService events;

    public HomeController(CurrentUserService currentUser, NoticeRepository notices, StudentRepository students,
                          FacultyRepository faculty, CourseRepository courses, AttendanceService attendanceService,
                          ResultService resultService, AccountService accounts,
                          EventService events) {
        this.currentUser = currentUser;
        this.notices = notices;
        this.students = students;
        this.faculty = faculty;
        this.courses = courses;
        this.attendanceService = attendanceService;
        this.resultService = resultService;
        this.accounts = accounts;
        this.events = events;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        AppUser me = currentUser.get();
        int hour = LocalTime.now().getHour();
        model.addAttribute("greeting", hour < 12 ? "Good morning" : hour < 17 ? "Good afternoon" : "Good evening");
        model.addAttribute("notices", notices.findTop5ByOrderByPostedOnDescIdDesc());
        model.addAttribute("nextEvents", events.nextFew());

        switch (me.getRole()) {
            case STUDENT -> {
                List<AttendanceService.Summary> att = attendanceService.summaryFor(me.getStudent());
                long attended = att.stream().mapToLong(AttendanceService.Summary::attended).sum();
                long total = att.stream().mapToLong(AttendanceService.Summary::total).sum();
                model.addAttribute("overallAttendance", total == 0 ? 0 : Math.round(attended * 1000.0 / total) / 10.0);
                model.addAttribute("shortageCount", att.stream().filter(AttendanceService.Summary::shortage).count());
                List<Mark> rows = resultService.marksFor(me.getStudent());
                model.addAttribute("sgpa", resultService.sgpa(rows));
                model.addAttribute("courseCount", rows.size());
            }
            case FACULTY -> model.addAttribute("myCourses", courses.findByFacultyOrderByCode(me.getFaculty()));
            case ADMIN -> {
                model.addAttribute("studentCount", students.count());
                model.addAttribute("facultyCount", faculty.count());
                model.addAttribute("courseCount", courses.count());
            }
        }
        return "dashboard";
    }

    @GetMapping("/notices")
    public String notices(Model model) {
        model.addAttribute("notices", notices.findAllByOrderByPostedOnDescIdDesc());
        return "notices";
    }

    @GetMapping("/profile")
    public String profile() {
        return "profile";
    }

    @PostMapping("/profile/password")
    public String changePassword(@RequestParam String current, @RequestParam String next,
                                 @RequestParam String confirm, RedirectAttributes ra) {
        try {
            accounts.changePassword(currentUser.get(), current, next, confirm);
            ra.addFlashAttribute("success", "Password changed");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/profile";
    }
}
