package neiu.lostfound.repository;

import neiu.lostfound.model.FoundItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FoundItemRepository extends JpaRepository<FoundItem, Long> {
    List<FoundItem> findByTitleContainingIgnoreCase(String title);
    List<FoundItem> findByLocationContainingIgnoreCase(String location);
    List<FoundItem> findByReporterNameContainingIgnoreCase(String reporterName);
    List<FoundItem> findByReporterEmailContainingIgnoreCase(String reporterEmail);
    List<FoundItem> findByReporterAddressContainingIgnoreCase(String reporterAddress);

    @Query("SELECT f FROM FoundItem f WHERE (:q IS NULL OR LOWER(f.title) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(f.location) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(f.reporterName) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(f.reporterEmail) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(f.reporterAddress) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(f.description) LIKE LOWER(CONCAT('%',:q,'%')))")
    List<FoundItem> searchFoundItems(@Param("q") String q);
}
