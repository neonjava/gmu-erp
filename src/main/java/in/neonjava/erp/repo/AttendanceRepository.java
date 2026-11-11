package in.neonjava.erp.repo;

import in.neonjava.erp.model.AttendanceRecord;
import in.neonjava.erp.model.Course;
import in.neonjava.erp.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<AttendanceRecord, Long> {
    List<AttendanceRecord> findByCourseAndDate(Course course, LocalDate date);
    Optional<AttendanceRecord> findByStudentAndCourseAndDate(Student student, Course course, LocalDate date);
    long countByStudentAndCourse(Student student, Course course);
    long countByStudentAndCourseAndPresentTrue(Student student, Course course);
    void deleteByStudent(Student student);
    void deleteByCourse(Course course);
}
