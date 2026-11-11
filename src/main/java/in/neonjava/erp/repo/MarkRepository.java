package in.neonjava.erp.repo;

import in.neonjava.erp.model.Course;
import in.neonjava.erp.model.Mark;
import in.neonjava.erp.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MarkRepository extends JpaRepository<Mark, Long> {
    List<Mark> findByStudent(Student student);
    List<Mark> findByCourse(Course course);
    Optional<Mark> findByStudentAndCourse(Student student, Course course);
    void deleteByStudent(Student student);
    void deleteByCourse(Course course);
}
