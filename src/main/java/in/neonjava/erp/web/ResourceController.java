package in.neonjava.erp.web;

import in.neonjava.erp.model.AppUser;
import in.neonjava.erp.model.CampusResource;
import in.neonjava.erp.security.CurrentUserService;
import in.neonjava.erp.service.BookingService;
import in.neonjava.erp.service.TimetableService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

/** Resource management: browse resources, check a day's availability, book and cancel. */
@Controller
@RequestMapping("/resources")
public class ResourceController {

    private final BookingService booking;
    private final CurrentUserService currentUser;

    public ResourceController(BookingService booking, CurrentUserService currentUser) {
        this.booking = booking;
        this.currentUser = currentUser;
    }

    @GetMapping
    public String list(Model model) {
        AppUser me = currentUser.get();
        model.addAttribute("resources", booking.listFor(me));
        model.addAttribute("bookings", booking.upcomingFor(me));
        model.addAttribute("today", LocalDate.now());
        return "resources/list";
    }

    @GetMapping("/{id}")
    public String day(@PathVariable Long id,
                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                      Model model) {
        AppUser me = currentUser.get();
        CampusResource r = booking.get(id);
        LocalDate today = LocalDate.now();
        LocalDate d = date != null ? date : firstBookableDay(today);
        model.addAttribute("resource", r);
        model.addAttribute("date", d);
        model.addAttribute("today", today);
        model.addAttribute("maxDate", today.plusDays(BookingService.MAX_DAYS_AHEAD));
        model.addAttribute("slots", booking.dayView(r, d));
        model.addAttribute("canBook", booking.canBook(me, r));
        return "resources/day";
    }

    /** Today, unless the last period has already started (or it is Sunday): then the next working day. */
    private static LocalDate firstBookableDay(LocalDate today) {
        LocalTime lastStart = LocalTime.parse(TimetableService.PERIOD_TIMES.get(TimetableService.PERIOD_TIMES.size() - 1));
        LocalDate d = LocalTime.now().isBefore(lastStart) ? today : today.plusDays(1);
        return d.getDayOfWeek() == DayOfWeek.SUNDAY ? d.plusDays(1) : d;
    }

    @PostMapping("/{id}/book")
    public String book(@PathVariable Long id,
                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                       @RequestParam int period, @RequestParam(required = false) String purpose, RedirectAttributes ra) {
        CampusResource r = booking.get(id);
        try {
            booking.book(r, date, period, currentUser.get(), purpose);
            ra.addFlashAttribute("success", "Booked " + r.getName() + " for P" + period + " on " + date);
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/resources/" + id + "?date=" + date;
    }

    @PostMapping("/bookings/{bookingId}/cancel")
    public String cancel(@PathVariable Long bookingId, @RequestParam(required = false) String back, RedirectAttributes ra) {
        try {
            booking.cancel(bookingId, currentUser.get());
            ra.addFlashAttribute("success", "Booking cancelled");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:" + (back != null && back.startsWith("/resources") ? back : "/resources");
    }
}
