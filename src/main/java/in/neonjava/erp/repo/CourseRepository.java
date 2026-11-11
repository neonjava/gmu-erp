package in.neonjava.erp.repo;

import in.neonjava.erp.model.Course;
import in.neonjava.erp.model.Department;
import in.neonjava.erp.model.Faculty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {
    boolean existsByCode(String code);
    List<Course> findAllByOrderByCode();
    List<Course> findByFacultyOrderByCode(Faculty faculty);
    List<Course> findByDepartmentAndSemesterOrderByCode(Department department, int semester);
}
