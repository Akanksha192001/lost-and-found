package neiu.lostfound.controller;

import neiu.lostfound.dto.HandoffQueueRequest;
import neiu.lostfound.model.HandoffQueue;
import neiu.lostfound.service.HandoffQueueService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/handoffs")
public class HandoffQueueController {
    private final HandoffQueueService handoffQueueService;

    public HandoffQueueController(HandoffQueueService handoffQueueService) {
        this.handoffQueueService = handoffQueueService;
    }

    @GetMapping
    public ResponseEntity<List<HandoffQueue>> getAllHandoffs(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "assignedTo", required = false) String assignedTo) {
        
        if (status != null && !status.isEmpty()) {
            try {
                HandoffQueue.HandoffStatus handoffStatus = HandoffQueue.HandoffStatus.valueOf(status.toUpperCase());
                return ResponseEntity.ok(handoffQueueService.getHandoffsByStatus(handoffStatus));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        }
        
        if (assignedTo != null && !assignedTo.isEmpty()) {
            return ResponseEntity.ok(handoffQueueService.getHandoffsByAssignedUser(assignedTo));
        }
        
        return ResponseEntity.ok(handoffQueueService.getAllHandoffs());
    }

    @GetMapping("/{id}")
    public ResponseEntity<HandoffQueue> getHandoffById(@PathVariable(value = "id") Long id) {
        Optional<HandoffQueue> handoff = handoffQueueService.getHandoffById(id);
        return handoff.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createHandoff(
            @RequestBody HandoffQueueRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        try {
            String username = userDetails != null ? userDetails.getUsername() : "anonymous";
            HandoffQueue handoff = handoffQueueService.createHandoff(request, username);
            return ResponseEntity.status(HttpStatus.CREATED).body(handoff);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating handoff: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateHandoff(
            @PathVariable(value = "id") Long id,
            @RequestBody HandoffQueueRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        try {
            String username = userDetails != null ? userDetails.getUsername() : "anonymous";
            HandoffQueue handoff = handoffQueueService.updateHandoff(id, request, username);
            return ResponseEntity.ok(handoff);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating handoff: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteHandoff(@PathVariable(value = "id") Long id) {
        try {
            handoffQueueService.deleteHandoff(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deleting handoff: " + e.getMessage());
        }
    }
}
