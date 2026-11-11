package in.neonjava.erp.model;

import jakarta.persistence.*;

/**
 * Marks for one student in one course.
 * CIE 1 and CIE 2 are out of 25 each, SEE is out of 50. Total is out of 100.
 */
@Entity
@Table(name = "marks",
       uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "course_id"}))
public class Mark {

    public static final int CIE_MAX = 25;
    public static final int SEE_MAX = 50;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne(optional = false)
    @JoinColumn(name = "course_id")
    private Course course;

    private Integer cie1;
    private Integer cie2;
    private Integer see;

    public Mark() {}

    public Mark(Student student, Course course) {
        this.student = student;
        this.course = course;
    }

    public int getCieTotal() {
        return (cie1 == null ? 0 : cie1) + (cie2 == null ? 0 : cie2);
    }

    public int getTotal() {
        return getCieTotal() + (see == null ? 0 : see);
    }

    /** Final grade is only available once SEE marks are entered. */
    public String getGrade() {
        if (see == null) return "-";
        int t = getTotal();
        if (t >= 90) return "O";
        if (t >= 80) return "A+";
        if (t >= 70) return "A";
        if (t >= 60) return "B+";
        if (t >= 55) return "B";
        if (t >= 50) return "C";
        if (t >= 40) return "P";
        return "F";
    }

    public int getGradePoint() {
        return switch (getGrade()) {
            case "O" -> 10;
            case "A+" -> 9;
            case "A" -> 8;
            case "B+" -> 7;
            case "B" -> 6;
            case "C" -> 5;
            case "P" -> 4;
            default -> 0;
        };
    }

    public Long getId() { return id; }
    public Student getStudent() { return student; }
    public Course getCourse() { return course; }
    public Integer getCie1() { return cie1; }
    public void setCie1(Integer cie1) { this.cie1 = cie1; }
    public Integer getCie2() { return cie2; }
    public void setCie2(Integer cie2) { this.cie2 = cie2; }
    public Integer getSee() { return see; }
    public void setSee(Integer see) { this.see = see; }
}
