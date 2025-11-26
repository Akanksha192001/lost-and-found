package neiu.lostfound.service;

import neiu.lostfound.dto.HandoffQueueRequest;
import neiu.lostfound.dto.HandoffQueueResponse;
import neiu.lostfound.model.*;
import neiu.lostfound.repository.*;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class HandoffQueueService {
    private final HandoffQueueRepository handoffQueueRepo;
    private final LostItemRepository lostItemRepo;
    private final FoundItemRepository foundItemRepo;
    private final ItemMatchRepository itemMatchRepo;
    private final EmailNotificationService emailNotifications;

    public HandoffQueueService(HandoffQueueRepository handoffQueueRepo,
                               LostItemRepository lostItemRepo,
                               FoundItemRepository foundItemRepo,
                               ItemMatchRepository itemMatchRepo,
                               EmailNotificationService emailNotifications) {
        this.handoffQueueRepo = handoffQueueRepo;
        this.lostItemRepo = lostItemRepo;
        this.foundItemRepo = foundItemRepo;
        this.itemMatchRepo = itemMatchRepo;
        this.emailNotifications = emailNotifications;
    }

    public List<HandoffQueueResponse> getAllHandoffs() {
        return handoffQueueRepo.findAllByOrderByInitiatedAtDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<HandoffQueueResponse> getHandoffsByStatus(HandoffQueue.HandoffStatus status) {
        return handoffQueueRepo.findByStatusOrderByInitiatedAtDesc(status).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<HandoffQueueResponse> getHandoffsByAssignedUser(String assignedTo) {
        return handoffQueueRepo.findByAssignedToOrderByInitiatedAtDesc(assignedTo).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public Optional<HandoffQueueResponse> getHandoffById(Long id) {
        return handoffQueueRepo.findById(id).map(this::toResponse);
    }
    
    private HandoffQueueResponse toResponse(HandoffQueue handoff) {
        Optional<ItemMatch> matchOpt = itemMatchRepo.findById(handoff.getMatchId());
        if (matchOpt.isEmpty()) {
            return new HandoffQueueResponse(handoff, null, null, null);
        }
        
        ItemMatch match = matchOpt.get();
        LostItem lostItem = lostItemRepo.findById(match.getLostItemId()).orElse(null);
        FoundItem foundItem = foundItemRepo.findById(match.getFoundItemId()).orElse(null);
        
        return new HandoffQueueResponse(handoff, match, lostItem, foundItem);
    }

    public HandoffQueue createHandoff(HandoffQueueRequest request, String initiatedBy) {
        Optional<LostItem> lostItemOpt = lostItemRepo.findById(request.getLostItemId());
        Optional<FoundItem> foundItemOpt = foundItemRepo.findById(request.getFoundItemId());

        if (lostItemOpt.isEmpty() || foundItemOpt.isEmpty()) {
            throw new IllegalArgumentException("Lost or Found item not found");
        }

        HandoffQueue handoff = new HandoffQueue();
        
        // Match must be provided - handoff cannot exist without a match
        if (request.getMatchId() == null) {
            throw new IllegalArgumentException("Match ID is required to create a handoff");
        }
        
        // Verify match exists
        if (itemMatchRepo.findById(request.getMatchId()).isEmpty()) {
            throw new IllegalArgumentException("Match not found with ID: " + request.getMatchId());
        }
        
        handoff.setMatchId(request.getMatchId());

        handoff.setInitiatedBy(initiatedBy);
        handoff.setInitiatedAt(new Date());
        handoff.setStatus(HandoffQueue.HandoffStatus.PENDING);

        if (request.getAssignedTo() != null) {
            handoff.setAssignedTo(request.getAssignedTo());
        }
        if (request.getNotes() != null) {
            handoff.setNotes(request.getNotes());
        }

        HandoffQueue saved = handoffQueueRepo.save(handoff);
        emailNotifications.notifyHandoffCreated(saved);
        return saved;
    }

    public HandoffQueue updateHandoff(Long id, HandoffQueueRequest request, String updatedBy) {
        Optional<HandoffQueue> handoffOpt = handoffQueueRepo.findById(id);
        if (handoffOpt.isEmpty()) {
            throw new IllegalArgumentException("Handoff not found");
        }

        HandoffQueue handoff = handoffOpt.get();
        HandoffQueue.HandoffStatus previousStatus = handoff.getStatus();

        // Update status if provided
        if (request.getStatus() != null) {
            HandoffQueue.HandoffStatus newStatus = HandoffQueue.HandoffStatus.valueOf(request.getStatus());
            handoff.setStatus(newStatus);

            // If completing, set completion details
            if (newStatus == HandoffQueue.HandoffStatus.COMPLETED) {
                handoff.setCompletedBy(updatedBy);
                handoff.setCompletedAt(new Date());
                emailNotifications.notifyHandoffStatusChange(handoff);
                
                // Mark items as RETURNED
                markAsReturned(handoff);
                return handoff;
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

        HandoffQueue saved = handoffQueueRepo.save(handoff);

        if (request.getStatus() != null && previousStatus != saved.getStatus()) {
            emailNotifications.notifyHandoffStatusChange(saved);
        }

        return saved;
    }

    public void deleteHandoff(Long id) {
        Optional<HandoffQueue> handoffOpt = handoffQueueRepo.findById(id);
        if (handoffOpt.isPresent()) {
            HandoffQueue handoff = handoffOpt.get();
            
            // Fetch the match by ID
            Long matchId = handoff.getMatchId();
            Optional<ItemMatch> matchOpt = itemMatchRepo.findById(matchId);
            
            if (matchOpt.isPresent()) {
                ItemMatch itemMatch = matchOpt.get();
                
                // Fetch and reset lost and found item statuses
                Optional<LostItem> lostOpt = lostItemRepo.findById(itemMatch.getLostItemId());
                Optional<FoundItem> foundOpt = foundItemRepo.findById(itemMatch.getFoundItemId());
                
                lostOpt.ifPresent(lost -> {
                    lost.setStatus(LostItem.Status.OPEN);
                    lostItemRepo.save(lost);
                });
                
                foundOpt.ifPresent(found -> {
                    found.setStatus(FoundItem.Status.UNCLAIMED);
                    foundItemRepo.save(found);
                });
                
                // Delete the handoff first (no FK constraints!)
                handoffQueueRepo.deleteById(id);
                
                // Delete the match (no FK constraints!)
                itemMatchRepo.delete(itemMatch);
            } else {
                // If match doesn't exist, just delete the handoff
                handoffQueueRepo.deleteById(id);
            }
        }
    }

    private void markAsReturned(HandoffQueue handoff) {
        // Fetch match by ID
        Optional<ItemMatch> matchOpt = itemMatchRepo.findById(handoff.getMatchId());
        if (matchOpt.isEmpty()) return;
        
        ItemMatch match = matchOpt.get();
        
        // Fetch items by ID
        Optional<LostItem> lostOpt = lostItemRepo.findById(match.getLostItemId());
        Optional<FoundItem> foundOpt = foundItemRepo.findById(match.getFoundItemId());
        
        if (lostOpt.isEmpty() || foundOpt.isEmpty()) return;
        
        LostItem lost = lostOpt.get();
        FoundItem found = foundOpt.get();

        // Simply update status to RETURNED - no need for separate table!
        lost.setStatus(LostItem.Status.RETURNED);
        found.setStatus(FoundItem.Status.RETURNED);
        
        lostItemRepo.save(lost);
        foundItemRepo.save(found);
        
        // Keep the handoff queue entry with COMPLETED status for audit trail
        // Don't delete it - admins should be able to view completed handoffs
        // The handoff is already saved with COMPLETED status in updateHandoff()
        
        // Keep the match record for history/audit trail
        // Items stay in their respective tables with RETURNED status
    }
}
