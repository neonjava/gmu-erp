package in.neonjava.erp.repo;

import in.neonjava.erp.model.CalendarEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface CalendarRepository extends JpaRepository<CalendarEntry, Long> {
    List<CalendarEntry> findByDateBetweenOrderByDate(LocalDate from, LocalDate to);
}
