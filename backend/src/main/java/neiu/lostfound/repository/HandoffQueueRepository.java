package neiu.lostfound.repository;

import neiu.lostfound.model.HandoffQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HandoffQueueRepository extends JpaRepository<HandoffQueue, Long> {
    List<HandoffQueue> findByStatusOrderByInitiatedAtDesc(HandoffQueue.HandoffStatus status);
    List<HandoffQueue> findByAssignedToOrderByInitiatedAtDesc(String assignedTo);
    List<HandoffQueue> findAllByOrderByInitiatedAtDesc();
}
