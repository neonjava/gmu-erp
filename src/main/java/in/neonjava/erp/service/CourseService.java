package in.neonjava.erp.service;

import in.neonjava.erp.model.Course;
import in.neonjava.erp.model.Registration;
import in.neonjava.erp.model.Student;
import in.neonjava.erp.repo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseService {

    private final CourseRepository courses;
    private final RegistrationRepository registrations;
    private final AttendanceRepository attendance;
    private final MarkRepository marks;
    private final TimetableSlotRepository slots;

    public CourseService(CourseRepository courses, RegistrationRepository registrations,
                         AttendanceRepository attendance, MarkRepository marks, TimetableSlotRepository slots) {
        this.courses = courses;
        this.registrations = registrations;
        this.attendance = attendance;
        this.marks = marks;
        this.slots = slots;
    }

    @Transactional
    public Course save(Course c) {
        if (c.getCode() == null || c.getCode().isBlank()) throw new IllegalArgumentException("Course code is required");
        if (c.getName() == null || c.getName().isBlank()) throw new IllegalArgumentException("Course name is required");
        if (c.getId() == null && courses.existsByCode(c.getCode())) {
            throw new IllegalArgumentException("Course " + c.getCode() + " already exists");
        }
        return courses.save(c);
    }

    @Transactional
    public void delete(Long id) {
        Course c = courses.findById(id).orElseThrow(() -> new IllegalArgumentException("Course not found"));
        slots.deleteByCourse(c);
        attendance.deleteByCourse(c);
        marks.deleteByCourse(c);
        registrations.deleteByCourse(c);
        courses.delete(c);
    }

    @Transactional
    public void register(Student s, Course c) {
        if (c.getSemester() != s.getSemester()) {
            throw new IllegalArgumentException(c.getCode() + " is not offered in your semester");
        }
        if (!registrations.existsByStudentAndCourse(s, c)) {
            registrations.save(new Registration(s, c));
        }
    }

    @Transactional
    public void drop(Student s, Course c) {
        registrations.findByStudentAndCourse(s, c).ifPresent(registrations::delete);
    }
}
