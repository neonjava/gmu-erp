package in.neonjava.erp.web;

import in.neonjava.erp.model.*;
import in.neonjava.erp.security.CurrentUserService;
import in.neonjava.erp.service.EventService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Event portal. Everyone can browse; admin and faculty create; students register. */
@Controller
@RequestMapping("/events")
public class EventController {

    private final EventService events;
    private final CurrentUserService currentUser;

    public EventController(EventService events, CurrentUserService currentUser) {
        this.events = events;
        this.currentUser = currentUser;
    }

    @GetMapping
    public String list(Model model) {
        AppUser me = currentUser.get();
        List<Event> upcoming = events.upcoming();
        List<Event> past = events.recentPast();
        List<Event> all = new ArrayList<>(upcoming);
        all.addAll(past);
        model.addAttribute("upcoming", upcoming);
        model.addAttribute("past", past);
        model.addAttribute("taken", events.seatsTaken(all));
        model.addAttribute("mine", me.getStudent() != null ? events.registeredEventIds(me.getStudent()) : Set.of());
        model.addAttribute("canCreate", me.getRole() != Role.STUDENT);
        return "events/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        AppUser me = currentUser.get();
        Event e = events.get(id);
        model.addAttribute("event", e);
        model.addAttribute("taken", events.seatsTaken(List.of(e)).get(e.getId()));
        model.addAttribute("registered", me.getStudent() != null && events.registeredEventIds(me.getStudent()).contains(id));
        model.addAttribute("canManage", events.canManage(e, me));
        if (me.getRole() != Role.STUDENT) model.addAttribute("participants", events.participants(e));
        return "events/detail";
    }

    @GetMapping("/new")
    public String form(Model model) {
        model.addAttribute("categories", EventCategory.values());
        model.addAttribute("today", LocalDate.now());
        return "events/form";
    }

    @PostMapping
    public String create(@RequestParam String title, @RequestParam(required = false) String description,
                         @RequestParam EventCategory category, @RequestParam String venue,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
                         @RequestParam(defaultValue = "0") int capacity, RedirectAttributes ra) {
        Event e = new Event(title, category, venue, date, startTime, capacity);
        e.setDescription(description == null ? null : description.trim());
        try {
            Event saved = events.create(e, currentUser.get());
            ra.addFlashAttribute("success", "Event created: " + saved.getTitle());
            return "redirect:/events/" + saved.getId();
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/events/new";
        }
    }

    @PostMapping("/{id}/register")
    public String register(@PathVariable Long id, RedirectAttributes ra) {
        Event e = events.get(id);
        try {
            events.register(currentUser.requireStudent(), e);
            ra.addFlashAttribute("success", "You are registered for " + e.getTitle());
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/events/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes ra) {
        Event e = events.get(id);
        try {
            events.cancel(currentUser.requireStudent(), e);
            ra.addFlashAttribute("success", "Registration cancelled for " + e.getTitle());
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/events/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        Event e = events.get(id);
        try {
            events.delete(e, currentUser.get());
            ra.addFlashAttribute("success", "Event deleted");
            return "redirect:/events";
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/events/" + id;
        }
    }
}
