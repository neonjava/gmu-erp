package in.neonjava.erp.repo;

import in.neonjava.erp.model.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    List<Notice> findAllByOrderByPostedOnDescIdDesc();
    List<Notice> findTop5ByOrderByPostedOnDescIdDesc();
}
