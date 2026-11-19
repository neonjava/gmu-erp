package in.neonjava.erp;

import in.neonjava.erp.model.*;
import in.neonjava.erp.repo.EventRepository;
import in.neonjava.erp.repo.StudentRepository;
import in.neonjava.erp.repo.UserRepository;
import in.neonjava.erp.service.EventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(EventService.class)
class EventServiceTest {

    @Autowired EventService service;
    @Autowired EventRepository events;
    @Autowired StudentRepository students;
    @Autowired UserRepository users;

    private Student student(String usn) {
        Student s = new Student();
        s.setUsn(usn);
        s.setName("Student " + usn);
        return students.save(s);
    }

    private Event event(int inDays, int capacity) {
        return events.save(new Event("Test event", EventCategory.WORKSHOP, "Hall", LocalDate.now().plusDays(inDays),
                LocalTime.NOON, capacity));
    }

    @Test
    void fullEventRejectsMoreRegistrations() {
        Event e = event(3, 2);
        service.register(student("U1"), e);
        service.register(student("U2"), e);
        var ex = assertThrows(IllegalArgumentException.class, () -> service.register(student("U3"), e));
        assertEquals("Sorry, this event is full", ex.getMessage());
    }

    @Test
    void cannotRegisterTwiceOrForPastEvents() {
        Student s = student("U4");
        Event e = event(3, 0);
        service.register(s, e);
        assertThrows(IllegalArgumentException.class, () -> service.register(s, e));
        assertThrows(IllegalArgumentException.class, () -> service.register(s, event(-1, 0)));
    }

    @Test
    void cancelFreesTheSeat() {
        Event e = event(3, 1);
        Student a = student("U5");
        service.register(a, e);
        service.cancel(a, e);
        assertDoesNotThrow(() -> service.register(student("U6"), e));
    }

    @Test
    void onlyOrganiserOrAdminCanDelete() {
        AppUser f1 = users.save(new AppUser("F801", "x", Role.FACULTY, "Prof. One"));
        AppUser f2 = users.save(new AppUser("F802", "x", Role.FACULTY, "Prof. Two"));
        Event e = service.create(new Event("Talk", EventCategory.SEMINAR, "Hall", LocalDate.now().plusDays(2), null, 0), f1);
        assertEquals("Prof. One", e.getOrganizer());
        assertThrows(IllegalArgumentException.class, () -> service.delete(e, f2));
        assertDoesNotThrow(() -> service.delete(e, f1));
    }

    @Test
    void createRejectsPastDates() {
        AppUser f = users.save(new AppUser("F803", "x", Role.FACULTY, "Prof. Three"));
        Event e = new Event("Old", EventCategory.SEMINAR, "Hall", LocalDate.now().minusDays(1), null, 0);
        assertThrows(IllegalArgumentException.class, () -> service.create(e, f));
    }
}
