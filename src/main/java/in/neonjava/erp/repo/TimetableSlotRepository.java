package in.neonjava.erp.repo;

import in.neonjava.erp.model.Course;
import in.neonjava.erp.model.TimetableSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TimetableSlotRepository extends JpaRepository<TimetableSlot, Long> {
    List<TimetableSlot> findBySemesterAndSection(int semester, String section);
    List<TimetableSlot> findByCourseIn(Collection<Course> courses);
    Optional<TimetableSlot> findBySemesterAndSectionAndDayOfWeekAndPeriod(int semester, String section, DayOfWeek day, int period);
    void deleteByCourse(Course course);
}
