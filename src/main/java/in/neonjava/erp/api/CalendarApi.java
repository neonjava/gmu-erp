package in.neonjava.erp.api;

import in.neonjava.erp.model.CalendarEntry;
import in.neonjava.erp.model.Event;
import in.neonjava.erp.service.CalendarService;
import in.neonjava.erp.service.EventService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/** The mini calendar: holidays, exam dates and campus events for one month. */
@RestController
@RequestMapping("/api")
public class CalendarApi {

    public record Entry(Long id, LocalDate date, String title, String type, String link) {}
    public record NewEntry(LocalDate date, String title, String type) {}

    private final CalendarService calendar;
    private final EventService events;

    public CalendarApi(CalendarService calendar, EventService events) {
        this.calendar = calendar;
        this.events = events;
    }

    @GetMapping("/calendar")
    public List<Entry> month(@RequestParam(required = false) String month) {
        YearMonth ym = month == null ? YearMonth.now() : YearMonth.parse(month);
        List<Entry> out = new ArrayList<>();
        for (CalendarEntry e : calendar.month(ym)) {
            out.add(new Entry(e.getId(), e.getDate(), e.getTitle(), e.getType().name(), null));
        }
        for (Event e : events.upcoming()) {
            if (YearMonth.from(e.getDate()).equals(ym)) {
                out.add(new Entry(null, e.getDate(), e.getTitle(), "EVENT", "/events/" + e.getId()));
            }
        }
        out.sort(Comparator.comparing(Entry::date));
        return out;
    }

    @PostMapping("/admin/calendar")
    public Entry add(@RequestBody NewEntry n) {
        CalendarEntry saved = calendar.add(n.date(), n.title(),
                n.type() == null ? null : CalendarEntry.Type.valueOf(n.type()));
        return new Entry(saved.getId(), saved.getDate(), saved.getTitle(), saved.getType().name(), null);
    }

    @DeleteMapping("/admin/calendar/{id}")
    public Map<String, String> delete(@PathVariable Long id) {
        calendar.delete(id);
        return Map.of("message", "Removed from the calendar");
    }
}
