package in.neonjava.erp.repo;

import in.neonjava.erp.model.Faculty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FacultyRepository extends JpaRepository<Faculty, Long> {
    boolean existsByEmpId(String empId);
    List<Faculty> findAllByOrderByEmpId();
    List<Faculty> findByNameContainingIgnoreCaseOrEmpIdContainingIgnoreCaseOrderByEmpId(String name, String empId);
}
