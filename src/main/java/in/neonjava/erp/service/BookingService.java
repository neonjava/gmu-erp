package in.neonjava.erp.service;

import in.neonjava.erp.model.*;
import in.neonjava.erp.repo.BookingRepository;
import in.neonjava.erp.repo.CampusResourceRepository;
import in.neonjava.erp.repo.TimetableSlotRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/** Resource management: labs, rooms, halls and equipment booked by period. */
@Service
public class BookingService {

    public static final int MAX_DAYS_AHEAD = 30;
    public static final int MAX_STUDENT_BOOKINGS = 2;

    private final CampusResourceRepository resources;
    private final BookingRepository bookings;
    private final TimetableSlotRepository timetable;

    public BookingService(CampusResourceRepository resources, BookingRepository bookings, TimetableSlotRepository timetable) {
        this.resources = resources;
        this.bookings = bookings;
        this.timetable = timetable;
    }

    /** One row of the day view: free, booked, taken by a timetable class, or already started. */
    public record Slot(int period, String time, Booking booking, TimetableSlot classSlot, boolean started) {
        public boolean free() { return booking == null && classSlot == null && !started; }
    }

    public CampusResource get(Long id) {
        return resources.findById(id).orElseThrow(() -> new IllegalArgumentException("Resource not found"));
    }

    /** Labs first, then classrooms, halls and equipment (enum order), each by name. */
    public List<CampusResource> listFor(AppUser u) {
        List<CampusResource> list = u.getRole() == Role.ADMIN ? resources.findAll() : resources.findByActiveTrue();
        return list.stream()
                .sorted(Comparator.comparing(CampusResource::getType).thenComparing(CampusResource::getName))
                .toList();
    }

    public List<Slot> dayView(CampusResource r, LocalDate date) {
        Map<Integer, Booking> booked = new HashMap<>();
        for (Booking b : bookings.findByResourceAndDate(r, date)) booked.put(b.getPeriod(), b);
        Map<Integer, TimetableSlot> classes = classesIn(r, date);
        List<Slot> out = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (int p = 1; p <= TimetableSlot.PERIODS_PER_DAY; p++) {
            String time = TimetableService.PERIOD_TIMES.get(p - 1);
            boolean started = !LocalDateTime.of(date, LocalTime.parse(time)).isAfter(now);
            out.add(new Slot(p, time, booked.get(p), classes.get(p), started));
        }
        return out;
    }

    public boolean canBook(AppUser u, CampusResource r) {
        return r.isActive() && (u.getRole() != Role.STUDENT || r.getType().isStudentBookable());
    }

    @Transactional
    public Booking book(CampusResource r, LocalDate date, int period, AppUser u, String purpose) {
        if (!r.isActive()) throw new IllegalArgumentException(r.getName() + " is not available for booking");
        if (period < 1 || period > TimetableSlot.PERIODS_PER_DAY) throw new IllegalArgumentException("Invalid period");
        LocalDate today = LocalDate.now();
        if (date == null || date.isBefore(today)) throw new IllegalArgumentException("Cannot book a past date");
        if (date.isAfter(today.plusDays(MAX_DAYS_AHEAD))) {
            throw new IllegalArgumentException("Bookings open only " + MAX_DAYS_AHEAD + " days ahead");
        }
        if (date.getDayOfWeek() == DayOfWeek.SUNDAY) throw new IllegalArgumentException("The campus is closed on Sundays");
        LocalTime start = LocalTime.parse(TimetableService.PERIOD_TIMES.get(period - 1));
        if (!LocalDateTime.of(date, start).isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("That period has already started");
        }
        if (purpose == null || purpose.isBlank()) throw new IllegalArgumentException("Please enter the purpose");
        if (u.getRole() == Role.STUDENT) {
            if (!r.getType().isStudentBookable()) {
                throw new IllegalArgumentException("Students can book only labs and equipment. Ask your faculty to book "
                        + r.getType().getLabel().toLowerCase() + "s");
            }
            if (bookings.countByBookedByAndDateGreaterThanEqual(u, today) >= MAX_STUDENT_BOOKINGS) {
                throw new IllegalArgumentException("Students can hold at most " + MAX_STUDENT_BOOKINGS
                        + " upcoming bookings. Cancel one first");
            }
        }
        TimetableSlot cls = classesIn(r, date).get(period);
        if (cls != null) {
            throw new IllegalArgumentException("P" + period + " is reserved for class " + cls.getCourse().getCode()
                    + " (Sem " + cls.getSemester() + cls.getSection() + ")");
        }
        bookings.findByResourceAndDateAndPeriod(r, date, period).ifPresent(b -> {
            throw new IllegalArgumentException("Already booked by " + b.getBookedBy().getDisplayName());
        });
        try {
            return bookings.saveAndFlush(new Booking(r, date, period, u, purpose.trim()));
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Someone booked this slot a moment ago. Pick another period");
        }
    }

    @Transactional
    public void cancel(Long bookingId, AppUser u) {
        Booking b = bookings.findById(bookingId).orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        boolean own = b.getBookedBy().getId().equals(u.getId());
        if (!own && u.getRole() != Role.ADMIN) throw new IllegalArgumentException("You can cancel only your own bookings");
        if (b.getDate().isBefore(LocalDate.now())) throw new IllegalArgumentException("Past bookings cannot be cancelled");
        bookings.delete(b);
    }

    /** Admins see every upcoming booking; others see their own. */
    public List<Booking> upcomingFor(AppUser u) {
        LocalDate today = LocalDate.now();
        return u.getRole() == Role.ADMIN ? bookings.findByDateGreaterThanEqualOrderByDateAscPeriodAsc(today)
                                         : bookings.findByBookedByAndDateGreaterThanEqualOrderByDateAscPeriodAsc(u, today);
    }

    public long bookingsToday() {
        return bookings.countByDate(LocalDate.now());
    }

    // ---------- Admin ----------

    @Transactional
    public CampusResource add(CampusResource r) {
        if (r.getName() == null || r.getName().isBlank()) throw new IllegalArgumentException("Name is required");
        r.setName(r.getName().trim());
        if (resources.existsByNameIgnoreCase(r.getName())) throw new IllegalArgumentException(r.getName() + " already exists");
        if (r.getCapacity() < 0) throw new IllegalArgumentException("Capacity cannot be negative");
        return resources.save(r);
    }

    @Transactional
    public CampusResource toggleActive(Long id) {
        CampusResource r = get(id);
        r.setActive(!r.isActive());
        return resources.save(r);
    }

    @Transactional
    public void delete(Long id) {
        CampusResource r = get(id);
        bookings.deleteByResource(r);
        resources.delete(r);
    }

    /** Timetable classes held in this room on that weekday, keyed by period. Rooms match by name. */
    private Map<Integer, TimetableSlot> classesIn(CampusResource r, LocalDate date) {
        Map<Integer, TimetableSlot> m = new HashMap<>();
        if (r.getType() == ResourceType.EQUIPMENT) return m;
        for (TimetableSlot t : timetable.findByRoomIgnoreCaseAndDayOfWeek(r.getName(), date.getDayOfWeek())) {
            m.putIfAbsent(t.getPeriod(), t);
        }
        return m;
    }
}
