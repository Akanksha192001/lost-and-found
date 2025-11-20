package neiu.lostfound.repository;

import neiu.lostfound.model.LostItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LostItemRepository extends JpaRepository<LostItem, Long> {
    List<LostItem> findByTitleContainingIgnoreCase(String title);
    List<LostItem> findByLocationContainingIgnoreCase(String location);
    List<LostItem> findByOwnerNameContainingIgnoreCase(String ownerName);
    List<LostItem> findByOwnerEmailContainingIgnoreCase(String ownerEmail);
    @Query("SELECT l FROM LostItem l WHERE (:q IS NULL OR LOWER(l.title) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(l.location) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(l.ownerName) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(l.ownerEmail) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(l.description) LIKE LOWER(CONCAT('%',:q,'%')))")
    List<LostItem> searchLostItems(@Param("q") String q);
    
    @Query("SELECT l FROM LostItem l WHERE l.reportedBy.email = :email ORDER BY l.id DESC")
    List<LostItem> findByReportedByEmail(@Param("email") String email);
}
