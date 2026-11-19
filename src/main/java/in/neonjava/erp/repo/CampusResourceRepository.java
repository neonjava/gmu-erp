package in.neonjava.erp.repo;

import in.neonjava.erp.model.CampusResource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CampusResourceRepository extends JpaRepository<CampusResource, Long> {
    List<CampusResource> findByActiveTrue();
    boolean existsByNameIgnoreCase(String name);
}
