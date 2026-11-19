package in.neonjava.erp;

import in.neonjava.erp.model.*;
import in.neonjava.erp.repo.*;
import in.neonjava.erp.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.DayOfWeek;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(BookingService.class)
class BookingServiceTest {

    @Autowired BookingService service;
    @Autowired CampusResourceRepository resources;
    @Autowired UserRepository users;
    @Autowired CourseRepository courses;
    @Autowired TimetableSlotRepository timetable;

    CampusResource lab, hall;
    AppUser faculty, student;
    LocalDate day;

    @BeforeEach
    void setUp() {
        lab = resources.save(new CampusResource("LAB-9", ResourceType.LAB, "Block A", 60, null));
        hall = resources.save(new CampusResource("Hall-9", ResourceType.HALL, "Block B", 200, null));
        faculty = users.save(new AppUser("F900", "x", Role.FACULTY, "Prof. Test"));
        student = users.save(new AppUser("U900", "x", Role.STUDENT, "Test Student"));
        day = LocalDate.now().plusDays(1);
        if (day.getDayOfWeek() == DayOfWeek.SUNDAY) day = day.plusDays(1);
    }

    @Test
    void sameSlotCannotBeBookedTwice() {
        service.book(lab, day, 3, faculty, "Lab session");
        var ex = assertThrows(IllegalArgumentException.class, () -> service.book(lab, day, 3, student, "Project"));
        assertTrue(ex.getMessage().contains("Already booked by Prof. Test"));
        assertDoesNotThrow(() -> service.book(lab, day, 4, student, "Project"));
    }

    @Test
    void studentsCannotBookHalls() {
        var ex = assertThrows(IllegalArgumentException.class, () -> service.book(hall, day, 1, student, "Club meet"));
        assertTrue(ex.getMessage().startsWith("Students can book only labs and equipment"));
        assertDoesNotThrow(() -> service.book(hall, day, 1, faculty, "Guest talk"));
    }

    @Test
    void studentsHoldAtMostTwoBookings() {
        service.book(lab, day, 1, student, "a");
        service.book(lab, day, 2, student, "b");
        var ex = assertThrows(IllegalArgumentException.class, () -> service.book(lab, day, 5, student, "c"));
        assertTrue(ex.getMessage().contains("at most 2"));
    }

    @Test
    void timetableClassBlocksTheRoom() {
        Course c = new Course();
        c.setCode("UE25CS9001");
        c.setName("Test Course");
        c.setSemester(3);
        courses.save(c);
        TimetableSlot t = new TimetableSlot();
        t.setSemester(3);
        t.setSection("A");
        t.setDayOfWeek(day.getDayOfWeek());
        t.setPeriod(4);
        t.setCourse(c);
        t.setRoom("lab-9");
        timetable.save(t);

        var ex = assertThrows(IllegalArgumentException.class, () -> service.book(lab, day, 4, faculty, "Extra"));
        assertTrue(ex.getMessage().contains("reserved for class UE25CS9001"));
        assertFalse(service.dayView(lab, day).get(3).free());
        assertTrue(service.dayView(lab, day).get(2).free());
    }

    @Test
    void pastSundayAndFarDatesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.book(lab, LocalDate.now().minusDays(1), 1, faculty, "x"));
        assertThrows(IllegalArgumentException.class, () -> service.book(lab, LocalDate.now().plusDays(45), 1, faculty, "x"));
        LocalDate sunday = LocalDate.now().plusDays(1);
        while (sunday.getDayOfWeek() != DayOfWeek.SUNDAY) sunday = sunday.plusDays(1);
        LocalDate s = sunday;
        assertThrows(IllegalArgumentException.class, () -> service.book(lab, s, 1, faculty, "x"));
    }

    @Test
    void onlyOwnerOrAdminCanCancel() {
        Booking b = service.book(lab, day, 2, faculty, "Lab");
        assertThrows(IllegalArgumentException.class, () -> service.cancel(b.getId(), student));
        AppUser admin = users.save(new AppUser("adm9", "x", Role.ADMIN, "Admin"));
        assertDoesNotThrow(() -> service.cancel(b.getId(), admin));
    }
}
