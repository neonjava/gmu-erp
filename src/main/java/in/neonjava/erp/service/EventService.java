package in.neonjava.erp.service;

import in.neonjava.erp.model.*;
import in.neonjava.erp.repo.EventRegistrationRepository;
import in.neonjava.erp.repo.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/** Campus events: create, list, register and cancel. */
@Service
public class EventService {

    private final EventRepository events;
    private final EventRegistrationRepository registrations;

    public EventService(EventRepository events, EventRegistrationRepository registrations) {
        this.events = events;
        this.registrations = registrations;
    }

    public List<Event> upcoming() {
        return events.findByDateGreaterThanEqualOrderByDateAscStartTimeAsc(LocalDate.now());
    }

    public List<Event> nextFew() {
        return events.findTop3ByDateGreaterThanEqualOrderByDateAscStartTimeAsc(LocalDate.now());
    }

    public List<Event> recentPast() {
        return events.findTop10ByDateLessThanOrderByDateDesc(LocalDate.now());
    }

    public Event get(Long id) {
        return events.findById(id).orElseThrow(() -> new IllegalArgumentException("Event not found"));
    }

    /** Registrations per event id, for showing seats taken. */
    public Map<Long, Long> seatsTaken(Collection<Event> list) {
        Map<Long, Long> m = new HashMap<>();
        for (Event e : list) m.put(e.getId(), registrations.countByEvent(e));
        return m;
    }

    public Set<Long> registeredEventIds(Student s) {
        return registrations.findByStudent(s).stream()
                .map(r -> r.getEvent().getId())
                .collect(Collectors.toSet());
    }

    public List<EventRegistration> participants(Event e) {
        return registrations.findByEventOrderByStudent_UsnAsc(e);
    }

    /** Admins manage every event; faculty manage the events they created. */
    public boolean canManage(Event e, AppUser u) {
        return u != null && (u.getRole() == Role.ADMIN || u.getUsername().equalsIgnoreCase(e.getCreatedBy()));
    }

    @Transactional
    public Event create(Event e, AppUser by) {
        if (e.getTitle() == null || e.getTitle().isBlank()) throw new IllegalArgumentException("Title is required");
        if (e.getVenue() == null || e.getVenue().isBlank()) throw new IllegalArgumentException("Venue is required");
        if (e.getDate() == null) throw new IllegalArgumentException("Date is required");
        if (e.getDate().isBefore(LocalDate.now())) throw new IllegalArgumentException("Event date cannot be in the past");
        if (e.getCapacity() < 0) throw new IllegalArgumentException("Seats cannot be negative");
        e.setTitle(e.getTitle().trim());
        e.setVenue(e.getVenue().trim());
        e.setOrganizer(by.getDisplayName());
        e.setCreatedBy(by.getUsername());
        return events.save(e);
    }

    @Transactional
    public void register(Student s, Event e) {
        if (e.isOver()) throw new IllegalArgumentException("Registration is closed: the event is over");
        if (registrations.existsByEventAndStudent(e, s)) throw new IllegalArgumentException("You are already registered");
        if (!e.isUnlimited() && registrations.countByEvent(e) >= e.getCapacity()) {
            throw new IllegalArgumentException("Sorry, this event is full");
        }
        registrations.save(new EventRegistration(e, s));
    }

    @Transactional
    public void cancel(Student s, Event e) {
        if (e.isOver()) throw new IllegalArgumentException("The event is over");
        EventRegistration r = registrations.findByEventAndStudent(e, s)
                .orElseThrow(() -> new IllegalArgumentException("You are not registered for this event"));
        registrations.delete(r);
    }

    @Transactional
    public void delete(Event e, AppUser by) {
        if (!canManage(e, by)) throw new IllegalArgumentException("Only the organiser or an admin can delete this event");
        registrations.deleteByEvent(e);
        events.delete(e);
    }
}
