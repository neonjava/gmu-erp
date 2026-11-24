package in.neonjava.erp.repo;

import in.neonjava.erp.model.AppUser;
import in.neonjava.erp.model.Faculty;
import in.neonjava.erp.model.Role;
import in.neonjava.erp.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsernameIgnoreCase(String username);
    boolean existsByUsernameIgnoreCase(String username);
    Optional<AppUser> findByStudent(Student student);
    Optional<AppUser> findByFaculty(Faculty faculty);
    List<AppUser> findByRole(Role role);
    void deleteByStudent(Student student);
    void deleteByFaculty(Faculty faculty);
}
