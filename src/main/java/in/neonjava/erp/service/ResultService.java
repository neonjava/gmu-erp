package in.neonjava.erp.service;

import in.neonjava.erp.model.*;
import in.neonjava.erp.repo.MarkRepository;
import in.neonjava.erp.repo.RegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class ResultService {

    private final MarkRepository marks;
    private final RegistrationRepository registrations;
    private final AttendanceService attendanceService;

    public ResultService(MarkRepository marks, RegistrationRepository registrations, AttendanceService attendanceService) {
        this.marks = marks;
        this.registrations = registrations;
        this.attendanceService = attendanceService;
    }

    /** One row per registered course; empty (unsaved) rows where no marks exist yet. */
    public List<Mark> marksFor(Student student) {
        return registrations.findByStudent(student).stream()
                .map(Registration::getCourse)
                .sorted(Comparator.comparing(Course::getCode))
                .map(c -> marks.findByStudentAndCourse(student, c).orElseGet(() -> new Mark(student, c)))
                .toList();
    }

    /** SGPA over courses that have a final grade. Returns null if none are graded yet. */
    public Double sgpa(List<Mark> rows) {
        int credits = 0, points = 0;
        for (Mark m : rows) {
            if (m.getSee() == null) continue;
            credits += m.getCourse().getCredits();
            points += m.getGradePoint() * m.getCourse().getCredits();
        }
        return credits == 0 ? null : Math.round(points * 100.0 / credits) / 100.0;
    }

    public List<Mark> sheetFor(Course course) {
        return attendanceService.studentsIn(course).stream()
                .map(s -> marks.findByStudentAndCourse(s, course).orElseGet(() -> new Mark(s, course)))
                .toList();
    }

    /**
     * Saves the marks sheet. Form fields are named cie1_{studentId}, cie2_{studentId}, see_{studentId}.
     * Blank means "not entered yet".
     */
    @Transactional
    public void saveSheet(Course course, Map<String, String> form) {
        for (Student s : attendanceService.studentsIn(course)) {
            Integer cie1 = parse(form.get("cie1_" + s.getId()), Mark.CIE_MAX, s, "CIE 1");
            Integer cie2 = parse(form.get("cie2_" + s.getId()), Mark.CIE_MAX, s, "CIE 2");
            Integer see = parse(form.get("see_" + s.getId()), Mark.SEE_MAX, s, "SEE");
            Mark m = marks.findByStudentAndCourse(s, course).orElseGet(() -> new Mark(s, course));
            m.setCie1(cie1);
            m.setCie2(cie2);
            m.setSee(see);
            marks.save(m);
        }
    }

    private Integer parse(String raw, int max, Student s, String label) {
        if (raw == null || raw.isBlank()) return null;
        try {
            int v = Integer.parseInt(raw.trim());
            if (v < 0 || v > max) throw new IllegalArgumentException(
                    label + " for " + s.getUsn() + " must be between 0 and " + max);
            return v;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " for " + s.getUsn() + " is not a number: " + raw);
        }
    }
}
