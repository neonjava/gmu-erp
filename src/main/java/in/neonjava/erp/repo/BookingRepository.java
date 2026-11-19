package in.neonjava.erp.repo;

import in.neonjava.erp.model.AppUser;
import in.neonjava.erp.model.Booking;
import in.neonjava.erp.model.CampusResource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByResourceAndDate(CampusResource resource, LocalDate date);
    Optional<Booking> findByResourceAndDateAndPeriod(CampusResource resource, LocalDate date, int period);
    List<Booking> findByBookedByAndDateGreaterThanEqualOrderByDateAscPeriodAsc(AppUser user, LocalDate from);
    List<Booking> findByDateGreaterThanEqualOrderByDateAscPeriodAsc(LocalDate from);
    long countByBookedByAndDateGreaterThanEqual(AppUser user, LocalDate from);
    long countByDate(LocalDate date);
    void deleteByResource(CampusResource resource);
    void deleteByBookedBy(AppUser user);
}
