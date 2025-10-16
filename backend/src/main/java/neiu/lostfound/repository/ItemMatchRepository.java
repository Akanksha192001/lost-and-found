package neiu.lostfound.repository;

import neiu.lostfound.model.ItemMatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ItemMatchRepository extends JpaRepository<ItemMatch, Long> {
    Optional<ItemMatch> findByLostItemIdAndFoundItemId(Long lostItemId, Long foundItemId);
    boolean existsByLostItemIdAndFoundItemId(Long lostItemId, Long foundItemId);
}