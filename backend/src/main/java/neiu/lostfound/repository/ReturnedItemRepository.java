package neiu.lostfound.repository;

import neiu.lostfound.model.ReturnedItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReturnedItemRepository extends JpaRepository<ReturnedItem, Long> {
    // Custom queries can be added here
}

