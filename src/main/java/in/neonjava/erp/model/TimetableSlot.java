package in.neonjava.erp.model;

import jakarta.persistence.*;

import java.time.DayOfWeek;

@Entity
@Table(name = "timetable_slots",
       uniqueConstraints = @UniqueConstraint(columnNames = {"semester", "section", "day_of_week", "period"}))
public class TimetableSlot {

    public static final int PERIODS_PER_DAY = 6;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int semester;

    @Column(length = 5)
    private String section = "A";

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", length = 10)
    private DayOfWeek dayOfWeek;

    /** 1..PERIODS_PER_DAY */
    private int period;

    @ManyToOne(optional = false)
    @JoinColumn(name = "course_id")
    private Course course;

    private String room;

    /** Key used by the timetable grid in templates, e.g. "MONDAY-3". */
    public String getKey() { return dayOfWeek.name() + "-" + period; }

    public Long getId() { return id; }
    public int getSemester() { return semester; }
    public void setSemester(int semester) { this.semester = semester; }
    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }
    public DayOfWeek getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(DayOfWeek dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public int getPeriod() { return period; }
    public void setPeriod(int period) { this.period = period; }
    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }
    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }
}
