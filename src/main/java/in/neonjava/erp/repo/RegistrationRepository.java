package in.neonjava.erp.repo;

import in.neonjava.erp.model.Course;
import in.neonjava.erp.model.Registration;
import in.neonjava.erp.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {
    List<Registration> findByStudent(Student student);
    List<Registration> findByCourse(Course course);
    Optional<Registration> findByStudentAndCourse(Student student, Course course);
    boolean existsByStudentAndCourse(Student student, Course course);
    long countByCourse(Course course);
    void deleteByStudent(Student student);
    void deleteByCourse(Course course);
}
