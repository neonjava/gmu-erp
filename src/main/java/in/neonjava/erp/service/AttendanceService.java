package in.neonjava.erp.service;

import in.neonjava.erp.model.*;
import in.neonjava.erp.repo.AttendanceRepository;
import in.neonjava.erp.repo.RegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AttendanceService {

    /** Minimum attendance percentage required (e.g. for exam eligibility). */
    public static final double REQUIRED_PERCENT = 75.0;

    private final AttendanceRepository attendance;
    private final RegistrationRepository registrations;

    public AttendanceService(AttendanceRepository attendance, RegistrationRepository registrations) {
        this.attendance = attendance;
        this.registrations = registrations;
    }

    public record Summary(Course course, long attended, long total) {
        public double percent() {
            return total == 0 ? 0 : Math.round(attended * 1000.0 / total) / 10.0;
        }
        public boolean shortage() {
            return total > 0 && percent() < REQUIRED_PERCENT;
        }
        /** Classes the student must attend in a row to reach the required percentage. */
        public long classesNeeded() {
            if (!shortage()) return 0;
            double r = REQUIRED_PERCENT / 100.0;
            return (long) Math.ceil((r * total - attended) / (1 - r));
        }
    }

    public List<Summary> summaryFor(Student student) {
        return registrations.findByStudent(student).stream()
                .map(Registration::getCourse)
                .sorted(Comparator.comparing(Course::getCode))
                .map(c -> new Summary(c,
                        attendance.countByStudentAndCourseAndPresentTrue(student, c),
                        attendance.countByStudentAndCourse(student, c)))
                .toList();
    }

    public List<Student> studentsIn(Course course) {
        return registrations.findByCourse(course).stream()
                .map(Registration::getStudent)
                .sorted(Comparator.comparing(Student::getUsn))
                .toList();
    }

    /** IDs of students marked present on a date. Empty set if attendance not yet taken. */
    public Set<Long> presentOn(Course course, LocalDate date) {
        return attendance.findByCourseAndDate(course, date).stream()
                .filter(AttendanceRecord::isPresent)
                .map(r -> r.getStudent().getId())
                .collect(Collectors.toSet());
    }

    public boolean isTaken(Course course, LocalDate date) {
        return !attendance.findByCourseAndDate(course, date).isEmpty();
    }

    /** Saves attendance for every registered student; anyone not in presentIds is absent. */
    @Transactional
    public int mark(Course course, LocalDate date, Collection<Long> presentIds) {
        if (date.isAfter(LocalDate.now())) throw new IllegalArgumentException("Cannot mark attendance for a future date");
        Set<Long> present = new HashSet<>(presentIds == null ? List.of() : presentIds);
        int count = 0;
        for (Student s : studentsIn(course)) {
            boolean isPresent = present.contains(s.getId());
            AttendanceRecord rec = attendance.findByStudentAndCourseAndDate(s, course, date)
                    .orElseGet(() -> new AttendanceRecord(s, course, date, isPresent));
            rec.setPresent(isPresent);
            attendance.save(rec);
            count++;
        }
        return count;
    }
}
