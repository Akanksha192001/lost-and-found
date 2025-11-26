package neiu.lostfound.repository;

import neiu.lostfound.model.ItemMatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ItemMatchRepository extends JpaRepository<ItemMatch, Long> {
    List<ItemMatch> findByLostItemIdAndFoundItemId(Long lostItemId, Long foundItemId);
    boolean existsByLostItemIdAndFoundItemId(Long lostItemId, Long foundItemId);
    
    // Find confirmed match for a found item (should be unique)
    Optional<ItemMatch> findByFoundItemIdAndStatus(Long foundItemId, ItemMatch.Status status);
    
    // Find all matches for a lost item
    List<ItemMatch> findByLostItemId(Long lostItemId);
    
    // Find all matches for a found item
    List<ItemMatch> findByFoundItemId(Long foundItemId);
}