package in.neonjava.erp.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** One period of one resource on one day. The unique constraint makes double-booking impossible. */
@Entity
@Table(name = "bookings",
       uniqueConstraints = @UniqueConstraint(columnNames = {"resource_id", "booking_date", "period"}))
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "resource_id")
    private CampusResource resource;

    @Column(name = "booking_date", nullable = false)
    private LocalDate date;

    /** 1..TimetableSlot.PERIODS_PER_DAY */
    private int period;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private AppUser bookedBy;

    private String purpose;

    private LocalDateTime createdAt = LocalDateTime.now();

    public Booking() {}

    public Booking(CampusResource resource, LocalDate date, int period, AppUser bookedBy, String purpose) {
        this.resource = resource;
        this.date = date;
        this.period = period;
        this.bookedBy = bookedBy;
        this.purpose = purpose;
    }

    public Long getId() { return id; }
    public CampusResource getResource() { return resource; }
    public LocalDate getDate() { return date; }
    public int getPeriod() { return period; }
    public AppUser getBookedBy() { return bookedBy; }
    public String getPurpose() { return purpose; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
