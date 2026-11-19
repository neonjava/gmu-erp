package in.neonjava.erp.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalTime;

/** A campus event that students can register for. */
@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventCategory category = EventCategory.TECHNICAL;

    private String venue;

    @Column(name = "event_date", nullable = false)
    private LocalDate date;

    @Column(name = "start_time")
    private LocalTime startTime;

    /** Maximum registrations; 0 means no limit. */
    private int capacity;

    /** Display name of the organiser. */
    private String organizer;

    /** Username of the account that created the event (may delete it). */
    private String createdBy;

    public Event() {}

    public Event(String title, EventCategory category, String venue, LocalDate date, LocalTime startTime, int capacity) {
        this.title = title;
        this.category = category;
        this.venue = venue;
        this.date = date;
        this.startTime = startTime;
        this.capacity = capacity;
    }

    public boolean isUnlimited() { return capacity <= 0; }

    public boolean isOver() { return date.isBefore(LocalDate.now()); }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public EventCategory getCategory() { return category; }
    public void setCategory(EventCategory category) { this.category = category; }
    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public String getOrganizer() { return organizer; }
    public void setOrganizer(String organizer) { this.organizer = organizer; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
