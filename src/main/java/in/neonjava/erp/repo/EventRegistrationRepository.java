package in.neonjava.erp.repo;

import in.neonjava.erp.model.Event;
import in.neonjava.erp.model.EventRegistration;
import in.neonjava.erp.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventRegistrationRepository extends JpaRepository<EventRegistration, Long> {
    List<EventRegistration> findByEventOrderByStudent_UsnAsc(Event event);
    List<EventRegistration> findByStudent(Student student);
    Optional<EventRegistration> findByEventAndStudent(Event event, Student student);
    boolean existsByEventAndStudent(Event event, Student student);
    long countByEvent(Event event);
    void deleteByEvent(Event event);
    void deleteByStudent(Student student);
}
