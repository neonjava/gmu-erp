package in.neonjava.erp.service;

import in.neonjava.erp.model.CalendarEntry;
import in.neonjava.erp.model.CalendarEntry.Type;
import in.neonjava.erp.repo.CalendarRepository;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.List;

/** The college calendar. Starts with the usual public holidays; the admin adds the rest. */
@Service
public class CalendarService implements SmartInitializingSingleton {

    private final CalendarRepository entries;

    public CalendarService(CalendarRepository entries) {
        this.entries = entries;
    }

    public List<CalendarEntry> month(YearMonth ym) {
        return entries.findByDateBetweenOrderByDate(ym.atDay(1), ym.atEndOfMonth());
    }

    @Transactional
    public CalendarEntry add(LocalDate date, String title, Type type) {
        if (date == null) throw new IllegalArgumentException("Pick a date");
        if (title == null || title.isBlank()) throw new IllegalArgumentException("Give it a name, like Diwali holiday");
        return entries.save(new CalendarEntry(date, title.trim(), type == null ? Type.HOLIDAY : type));
    }

    @Transactional
    public void delete(Long id) {
        entries.deleteById(id);
    }

    /** First start only: public holidays that fall on the same date every year. The admin can add the moving ones. */
    @Override
    @Transactional
    public void afterSingletonsInstantiated() {
        if (entries.count() > 0) return;
        int y = LocalDate.now().getYear();
        for (int year = y; year <= y + 1; year++) {
            add(year, Month.JANUARY, 1, "New Year's Day");
            add(year, Month.JANUARY, 26, "Republic Day");
            add(year, Month.APRIL, 14, "Dr. B. R. Ambedkar Jayanti");
            add(year, Month.MAY, 1, "Labour Day");
            add(year, Month.AUGUST, 15, "Independence Day");
            add(year, Month.OCTOBER, 2, "Gandhi Jayanti");
            add(year, Month.NOVEMBER, 1, "Kannada Rajyotsava");
            add(year, Month.DECEMBER, 25, "Christmas");
        }
        // A few academic dates relative to today so the calendar shows every kind of entry on a fresh demo.
        LocalDate monday = LocalDate.now().plusDays(8 - LocalDate.now().getDayOfWeek().getValue());
        entries.save(new CalendarEntry(monday, "CIE 2 begins", Type.EXAM));
        entries.save(new CalendarEntry(monday.plusDays(4), "CIE 2 ends", Type.EXAM));
        entries.save(new CalendarEntry(monday.plusDays(19), "OOPS PBL proposal review", Type.ACADEMIC));
    }

    private void add(int year, Month month, int day, String title) {
        entries.save(new CalendarEntry(LocalDate.of(year, month, day), title, Type.HOLIDAY));
    }
}
