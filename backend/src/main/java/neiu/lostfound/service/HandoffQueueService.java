package neiu.lostfound.service;

import neiu.lostfound.dto.HandoffQueueRequest;
import neiu.lostfound.model.*;
import neiu.lostfound.repository.*;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class HandoffQueueService {
    private final HandoffQueueRepository handoffQueueRepo;
    private final LostItemRepository lostItemRepo;
    private final FoundItemRepository foundItemRepo;
    private final ItemMatchRepository itemMatchRepo;
    private final ReturnedItemRepository returnedItemRepo;

    public HandoffQueueService(HandoffQueueRepository handoffQueueRepo,
                               LostItemRepository lostItemRepo,
                               FoundItemRepository foundItemRepo,
                               ItemMatchRepository itemMatchRepo,
                               ReturnedItemRepository returnedItemRepo) {
        this.handoffQueueRepo = handoffQueueRepo;
        this.lostItemRepo = lostItemRepo;
        this.foundItemRepo = foundItemRepo;
        this.itemMatchRepo = itemMatchRepo;
        this.returnedItemRepo = returnedItemRepo;
    }

    public List<HandoffQueue> getAllHandoffs() {
        return handoffQueueRepo.findAllByOrderByInitiatedAtDesc();
    }

    public List<HandoffQueue> getHandoffsByStatus(HandoffQueue.HandoffStatus status) {
        return handoffQueueRepo.findByStatusOrderByInitiatedAtDesc(status);
    }

    public List<HandoffQueue> getHandoffsByAssignedUser(String assignedTo) {
        return handoffQueueRepo.findByAssignedToOrderByInitiatedAtDesc(assignedTo);
    }

    public Optional<HandoffQueue> getHandoffById(Long id) {
        return handoffQueueRepo.findById(id);
    }

    public HandoffQueue createHandoff(HandoffQueueRequest request, String initiatedBy) {
        Optional<LostItem> lostItemOpt = lostItemRepo.findById(request.getLostItemId());
        Optional<FoundItem> foundItemOpt = foundItemRepo.findById(request.getFoundItemId());

        if (lostItemOpt.isEmpty() || foundItemOpt.isEmpty()) {
            throw new IllegalArgumentException("Lost or Found item not found");
        }

        HandoffQueue handoff = new HandoffQueue();
        handoff.setLostItem(lostItemOpt.get());
        handoff.setFoundItem(foundItemOpt.get());

        // If matchId is provided, link to existing match
        if (request.getMatchId() != null) {
            itemMatchRepo.findById(request.getMatchId()).ifPresent(handoff::setItemMatch);
        }

        handoff.setInitiatedBy(initiatedBy);
        handoff.setInitiatedAt(new Date());
        handoff.setStatus(HandoffQueue.HandoffStatus.PENDING);

        if (request.getAssignedTo() != null) {
            handoff.setAssignedTo(request.getAssignedTo());
        }
        if (request.getNotes() != null) {
            handoff.setNotes(request.getNotes());
        }

        return handoffQueueRepo.save(handoff);
    }

    public HandoffQueue updateHandoff(Long id, HandoffQueueRequest request, String updatedBy) {
        Optional<HandoffQueue> handoffOpt = handoffQueueRepo.findById(id);
        if (handoffOpt.isEmpty()) {
            throw new IllegalArgumentException("Handoff not found");
        }

        HandoffQueue handoff = handoffOpt.get();

        // Update status if provided
        if (request.getStatus() != null) {
            HandoffQueue.HandoffStatus newStatus = HandoffQueue.HandoffStatus.valueOf(request.getStatus());
            handoff.setStatus(newStatus);

            // If completing, set completion details
            if (newStatus == HandoffQueue.HandoffStatus.COMPLETED) {
                handoff.setCompletedBy(updatedBy);
                handoff.setCompletedAt(new Date());
                
                // Move items to returned_items table
                moveToReturned(handoff);
            }

            // If cancelling, save cancellation reason
            if (newStatus == HandoffQueue.HandoffStatus.CANCELLED && request.getCancellationReason() != null) {
                handoff.setCancellationReason(request.getCancellationReason());
            }
        }

        // Update other fields
        if (request.getAssignedTo() != null) {
            handoff.setAssignedTo(request.getAssignedTo());
        }
        if (request.getScheduledHandoffTime() != null) {
            handoff.setScheduledHandoffTime(request.getScheduledHandoffTime());
        }
        if (request.getHandoffLocation() != null) {
            handoff.setHandoffLocation(request.getHandoffLocation());
        }
        if (request.getNotes() != null) {
            handoff.setNotes(request.getNotes());
        }

        return handoffQueueRepo.save(handoff);
    }

    public void deleteHandoff(Long id) {
        Optional<HandoffQueue> handoffOpt = handoffQueueRepo.findById(id);
        if (handoffOpt.isPresent()) {
            HandoffQueue handoff = handoffOpt.get();
            
            // Store the match reference before deleting handoff
            ItemMatch itemMatch = handoff.getItemMatch();
            
            // Reset lost and found item statuses
            LostItem lostItem = handoff.getLostItem();
            FoundItem foundItem = handoff.getFoundItem();
            
            if (lostItem != null) {
                lostItem.setStatus(LostItem.Status.OPEN);
                lostItem.setMatchedWith(null);
                lostItemRepo.save(lostItem);
            }
            
            if (foundItem != null) {
                foundItem.setStatus(FoundItem.Status.UNCLAIMED);
                foundItem.setMatchedWith(null);
                foundItemRepo.save(foundItem);
            }
            
            // Delete the handoff first (it has FK to item_match)
            handoffQueueRepo.deleteById(id);
            
            // Then delete the confirmed match record if exists
            if (itemMatch != null) {
                itemMatchRepo.delete(itemMatch);
            }
        }
    }

    private void moveToReturned(HandoffQueue handoff) {
        LostItem lost = handoff.getLostItem();
        FoundItem found = handoff.getFoundItem();
        ItemMatch match = handoff.getItemMatch();

        // Create returned item record
        ReturnedItem returned = new ReturnedItem();
        returned.setTitle(lost.getTitle());
        returned.setDescription(lost.getDescription());
        returned.setLocation(found.getLocation());
        returned.setDateReturned(new Date());
        returned.setImageData(lost.getImageData());
        returned.setOwnerName(lost.getOwnerName());
        returned.setOwnerEmail(lost.getOwnerEmail());
        returned.setReporterName(found.getReporterName());
        returned.setReporterEmail(found.getReporterEmail());
        returnedItemRepo.save(returned);

        // Clear matchedWith references before deletion to avoid FK constraint violations
        if (lost.getMatchedWith() != null) {
            lost.setMatchedWith(null);
            lostItemRepo.save(lost);
        }
        if (found.getMatchedWith() != null) {
            found.setMatchedWith(null);
            foundItemRepo.save(found);
        }

        // Clear the handoff's references to items and match before deleting them
        handoff.setLostItem(null);
        handoff.setFoundItem(null);
        handoff.setItemMatch(null);
        handoffQueueRepo.save(handoff);

        // Now safe to delete the lost and found items
        lostItemRepo.delete(lost);
        foundItemRepo.delete(found);
        
        // Delete the match record if it exists
        if (match != null) {
            itemMatchRepo.delete(match);
        }
    }
}
