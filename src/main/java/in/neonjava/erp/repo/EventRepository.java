package in.neonjava.erp.repo;

import in.neonjava.erp.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByDateGreaterThanEqualOrderByDateAscStartTimeAsc(LocalDate from);
    List<Event> findTop10ByDateLessThanOrderByDateDesc(LocalDate before);
    List<Event> findTop3ByDateGreaterThanEqualOrderByDateAscStartTimeAsc(LocalDate from);
}
