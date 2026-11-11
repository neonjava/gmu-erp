package in.neonjava.erp.repo;

import in.neonjava.erp.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByUsn(String usn);
    boolean existsByUsn(String usn);
    List<Student> findAllByOrderByUsn();
    List<Student> findByNameContainingIgnoreCaseOrUsnContainingIgnoreCaseOrderByUsn(String name, String usn);
}
