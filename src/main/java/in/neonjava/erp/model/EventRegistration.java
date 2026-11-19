package in.neonjava.erp.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** A student registered for an event. */
@Entity
@Table(name = "event_registrations",
       uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "student_id"}))
public class EventRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "event_id")
    private Event event;

    @ManyToOne(optional = false)
    @JoinColumn(name = "student_id")
    private Student student;

    private LocalDateTime registeredAt = LocalDateTime.now();

    public EventRegistration() {}

    public EventRegistration(Event event, Student student) {
        this.event = event;
        this.student = student;
    }

    public Long getId() { return id; }
    public Event getEvent() { return event; }
    public Student getStudent() { return student; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }
}
