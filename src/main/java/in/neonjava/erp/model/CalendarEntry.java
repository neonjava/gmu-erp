package in.neonjava.erp.model;

import jakarta.persistence.*;

import java.time.LocalDate;

/** A date on the college calendar: a holiday, an exam period, or another academic date. */
@Entity
@Table(name = "calendar_entries")
public class CalendarEntry {

    public enum Type { HOLIDAY, EXAM, ACADEMIC }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entry_date", nullable = false)
    private LocalDate date;

    @Column(nullable = false, length = 120)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Type type;

    public CalendarEntry() {}

    public CalendarEntry(LocalDate date, String title, Type type) {
        this.date = date;
        this.title = title;
        this.type = type;
    }

    public Long getId() { return id; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
}
