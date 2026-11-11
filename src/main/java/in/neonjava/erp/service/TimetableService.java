package in.neonjava.erp.service;

import in.neonjava.erp.model.Course;
import in.neonjava.erp.model.TimetableSlot;
import in.neonjava.erp.repo.TimetableSlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TimetableService {

    public static final List<DayOfWeek> WORKING_DAYS = List.of(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY);

    public static final List<String> PERIOD_TIMES = List.of(
            "09:00", "10:00", "11:15", "12:15", "14:00", "15:00");

    private final TimetableSlotRepository slots;

    public TimetableService(TimetableSlotRepository slots) {
        this.slots = slots;
    }

    /** Grid keyed by "DAY-period", e.g. "MONDAY-1". */
    public Map<String, TimetableSlot> grid(int semester, String section) {
        return slots.findBySemesterAndSection(semester, section).stream()
                .collect(Collectors.toMap(TimetableSlot::getKey, Function.identity(), (a, b) -> a));
    }

    /** Grid limited to the given courses (used for faculty: their own classes across sections). */
    public Map<String, List<TimetableSlot>> gridForCourses(Collection<Course> courses) {
        if (courses.isEmpty()) return Map.of();
        return slots.findByCourseIn(courses).stream()
                .collect(Collectors.groupingBy(TimetableSlot::getKey));
    }

    @Transactional
    public void setSlot(int semester, String section, DayOfWeek day, int period, Course course, String room) {
        if (period < 1 || period > TimetableSlot.PERIODS_PER_DAY) throw new IllegalArgumentException("Invalid period");
        TimetableSlot slot = slots.findBySemesterAndSectionAndDayOfWeekAndPeriod(semester, section, day, period)
                .orElseGet(TimetableSlot::new);
        slot.setSemester(semester);
        slot.setSection(section);
        slot.setDayOfWeek(day);
        slot.setPeriod(period);
        slot.setCourse(course);
        slot.setRoom(room);
        slots.save(slot);
    }

    @Transactional
    public void clearSlot(int semester, String section, DayOfWeek day, int period) {
        slots.findBySemesterAndSectionAndDayOfWeekAndPeriod(semester, section, day, period)
                .ifPresent(slots::delete);
    }
}
