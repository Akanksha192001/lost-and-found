package neiu.lostfound.service;

import neiu.lostfound.model.FoundItem;
import neiu.lostfound.model.HandoffQueue;
import neiu.lostfound.model.ItemMatch;
import neiu.lostfound.model.LostItem;
import neiu.lostfound.repository.FoundItemRepository;
import neiu.lostfound.repository.ItemMatchRepository;
import neiu.lostfound.repository.LostItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Service
public class EmailNotificationService {
  private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);
  private static final DateFormat DATE_TIME_FORMAT = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.US);

  private final JavaMailSender mailSender;
  private final boolean notificationsEnabled;
  private final String fromAddress;
  private final ItemMatchRepository itemMatchRepo;
  private final LostItemRepository lostItemRepo;
  private final FoundItemRepository foundItemRepo;

  public EmailNotificationService(
      JavaMailSender mailSender,
      @Value("${app.notifications.enabled:true}") boolean notificationsEnabled,
      @Value("${app.notifications.from-address:no-reply@lostfound-neiu.local}") String fromAddress,
      ItemMatchRepository itemMatchRepo,
      LostItemRepository lostItemRepo,
      FoundItemRepository foundItemRepo) {
    this.mailSender = mailSender;
    this.notificationsEnabled = notificationsEnabled;
    this.fromAddress = fromAddress;
    this.itemMatchRepo = itemMatchRepo;
    this.lostItemRepo = lostItemRepo;
    this.foundItemRepo = foundItemRepo;
  }

  @Async
  public void sendMatchConfirmation(LostItem lostItem, FoundItem foundItem) {
    if (lostItem == null || foundItem == null) {
      return;
    }
    String subject = "Lost & Found match confirmed";
    String body = """
        Hello,

        Good news! A match was confirmed between a lost item (%s) and a found item reported at %s.

        Lost item owner: %s (%s)
        Found item reporter: %s (%s)

        Our team will be in touch with the next handoff steps. You can also log back into the Lost & Found portal for more details.

        -- NEIU Lost & Found Team
        """.formatted(
        safe(lostItem.getTitle()),
        safe(foundItem.getLocation()),
        safe(lostItem.getOwnerName()),
        safe(lostItem.getOwnerEmail()),
        safe(foundItem.getReporterName()),
        safe(foundItem.getReporterEmail()));

    sendToParticipants(Arrays.asList(
        lostItem.getOwnerEmail(),
        foundItem.getReporterEmail()), subject, body);
  }

  @Async
  public void notifyHandoffCreated(HandoffQueue handoff) {
    if (handoff == null) return;
    
    // Fetch items manually from repositories
    Optional<ItemMatch> matchOpt = itemMatchRepo.findById(handoff.getMatchId());
    if (matchOpt.isEmpty()) return;
    
    ItemMatch match = matchOpt.get();
    Optional<LostItem> lostOpt = lostItemRepo.findById(match.getLostItemId());
    Optional<FoundItem> foundOpt = foundItemRepo.findById(match.getFoundItemId());
    
    if (lostOpt.isEmpty() || foundOpt.isEmpty()) return;
    
    LostItem lostItem = lostOpt.get();
    FoundItem foundItem = foundOpt.get();
    
    String itemTitle = safe(lostItem.getTitle());
    
    // Email to lost item owner
    String ownerSubject = "✓ Handoff Scheduled - Your Lost Item: " + itemTitle;
    String ownerBody = """
        Dear %s,

        Great news! We have scheduled a handoff for your lost item.

        ══════════════════════════════════════════════
        ITEM DETAILS
        ══════════════════════════════════════════════
        Item: %s
        Description: %s
        
        ══════════════════════════════════════════════
        HANDOFF INFORMATION
        ══════════════════════════════════════════════
        %s
        
        ══════════════════════════════════════════════

        WHAT TO BRING:
        • A valid photo ID
        • This email or your Lost & Found reference number
        • Any proof of ownership (if available)

        IMPORTANT NOTES:
        • Please arrive on time for the scheduled handoff
        • If you cannot make it, please contact us immediately
        • You will receive updates if there are any changes

        If you have any questions, please log into the Lost & Found portal or contact the Lost & Found desk.

        Thank you,
        NEIU Lost & Found Team
        """.formatted(
        safe(lostItem.getOwnerName()),
        itemTitle,
        safe(lostItem.getDescription()),
        buildHandoffDetails(handoff));
    
    // Email to found item reporter
    String reporterSubject = "✓ Handoff Scheduled - Found Item: " + itemTitle;
    String reporterBody = """
        Dear %s,

        Thank you for reporting a found item! We have scheduled a handoff to return it to its owner.

        ══════════════════════════════════════════════
        ITEM DETAILS
        ══════════════════════════════════════════════
        Item: %s
        Location Found: %s
        
        ══════════════════════════════════════════════
        HANDOFF INFORMATION
        ══════════════════════════════════════════════
        %s
        
        ══════════════════════════════════════════════

        Your assistance in helping reunite this item with its owner is greatly appreciated!

        You will receive a notification once the handoff is completed.

        Thank you for being part of our community,
        NEIU Lost & Found Team
        """.formatted(
        safe(foundItem.getReporterName()),
        itemTitle,
        safe(foundItem.getLocation()),
        buildHandoffDetails(handoff));
    
    sendEmail(lostItem.getOwnerEmail(), ownerSubject, ownerBody);
    sendEmail(foundItem.getReporterEmail(), reporterSubject, reporterBody);
  }

  @Async
  public void notifyHandoffStatusChange(HandoffQueue handoff) {
    if (handoff == null) return;
    
    // Fetch items manually from repositories
    Optional<ItemMatch> matchOpt = itemMatchRepo.findById(handoff.getMatchId());
    if (matchOpt.isEmpty()) return;
    
    ItemMatch match = matchOpt.get();
    Optional<LostItem> lostOpt = lostItemRepo.findById(match.getLostItemId());
    Optional<FoundItem> foundOpt = foundItemRepo.findById(match.getFoundItemId());
    
    if (lostOpt.isEmpty() || foundOpt.isEmpty()) return;
    
    LostItem lostItem = lostOpt.get();
    FoundItem foundItem = foundOpt.get();
    
    String itemTitle = safe(lostItem.getTitle());
    HandoffQueue.HandoffStatus status = handoff.getStatus();
    
    switch (status) {
      case SCHEDULED -> notifyScheduled(handoff, lostItem, foundItem, itemTitle);
      case COMPLETED -> notifyCompleted(handoff, lostItem, foundItem, itemTitle);
      case CANCELLED -> notifyCancelled(handoff, lostItem, foundItem, itemTitle);
      case PENDING -> notifyPending(handoff, lostItem, foundItem, itemTitle);
      default -> notifyGenericUpdate(handoff, lostItem, foundItem, itemTitle);
    }
  }
  
  private void notifyScheduled(HandoffQueue handoff, LostItem lostItem, FoundItem foundItem, String itemTitle) {
    String ownerSubject = "📅 Handoff Scheduled - " + itemTitle;
    String ownerBody = """
        Dear %s,

        Your handoff has been scheduled! Here are the updated details:

        ══════════════════════════════════════════════
        HANDOFF DETAILS
        ══════════════════════════════════════════════
        Item: %s
        %s
        
        ══════════════════════════════════════════════

        WHAT TO BRING:
        • Valid photo ID
        • This email confirmation
        • Proof of ownership (if available)

        Please arrive on time. If you need to reschedule, contact us as soon as possible.

        Best regards,
        NEIU Lost & Found Team
        """.formatted(
        safe(lostItem.getOwnerName()),
        itemTitle,
        buildHandoffDetails(handoff));
    
    String reporterSubject = "📅 Handoff Scheduled - " + itemTitle;
    String reporterBody = """
        Dear %s,

        The handoff for the item you reported has been scheduled.

        ══════════════════════════════════════════════
        HANDOFF DETAILS
        ══════════════════════════════════════════════
        Item: %s
        %s
        
        ══════════════════════════════════════════════

        Thank you for your contribution to our community!

        Best regards,
        NEIU Lost & Found Team
        """.formatted(
        safe(foundItem.getReporterName()),
        itemTitle,
        buildHandoffDetails(handoff));
    
    sendEmail(lostItem.getOwnerEmail(), ownerSubject, ownerBody);
    sendEmail(foundItem.getReporterEmail(), reporterSubject, reporterBody);
  }
  
  private void notifyCompleted(HandoffQueue handoff, LostItem lostItem, FoundItem foundItem, String itemTitle) {
    String ownerSubject = "✅ Handoff Completed - " + itemTitle;
    String ownerBody = """
        Dear %s,

        Great news! Your handoff has been successfully completed.

        ══════════════════════════════════════════════
        COMPLETION DETAILS
        ══════════════════════════════════════════════
        Item: %s
        Completed on: %s
        Completed by: %s
        
        ══════════════════════════════════════════════

        We're glad we could help reunite you with your item!

        If you have any feedback about our Lost & Found service, we'd love to hear from you.

        Thank you for using NEIU Lost & Found!
        
        Best regards,
        NEIU Lost & Found Team
        """.formatted(
        safe(lostItem.getOwnerName()),
        itemTitle,
        formatDate(handoff.getCompletedAt() != null ? handoff.getCompletedAt() : new Date()),
        safe(handoff.getCompletedBy()));
    
    String reporterSubject = "✅ Handoff Completed - " + itemTitle;
    String reporterBody = """
        Dear %s,

        Wonderful news! The item you reported has been successfully returned to its owner.

        ══════════════════════════════════════════════
        COMPLETION DETAILS
        ══════════════════════════════════════════════
        Item: %s
        Location Found: %s
        Completed on: %s
        
        ══════════════════════════════════════════════

        THANK YOU!
        
        Your honesty and community spirit made a real difference. Because of people like you, 
        our Lost & Found service continues to help students and staff reunite with their belongings.

        We truly appreciate your contribution to making NEIU a better place!
        
        With gratitude,
        NEIU Lost & Found Team
        """.formatted(
        safe(foundItem.getReporterName()),
        itemTitle,
        safe(foundItem.getLocation()),
        formatDate(handoff.getCompletedAt() != null ? handoff.getCompletedAt() : new Date()));
    
    sendEmail(lostItem.getOwnerEmail(), ownerSubject, ownerBody);
    sendEmail(foundItem.getReporterEmail(), reporterSubject, reporterBody);
  }
  
  private void notifyCancelled(HandoffQueue handoff, LostItem lostItem, FoundItem foundItem, String itemTitle) {
    String reason = handoff.getCancellationReason() != null && !handoff.getCancellationReason().isBlank()
        ? "\nReason: " + handoff.getCancellationReason()
        : "";
    
    String ownerSubject = "⚠️ Handoff Cancelled - " + itemTitle;
    String ownerBody = """
        Dear %s,

        We regret to inform you that the handoff for your item has been cancelled.

        ══════════════════════════════════════════════
        CANCELLATION DETAILS
        ══════════════════════════════════════════════
        Item: %s%s
        
        ══════════════════════════════════════════════

        NEXT STEPS:
        
        Please contact the Lost & Found desk for further assistance. We will work with you 
        to reschedule or arrange an alternative solution.
        
        Contact Options:
        • Visit the Lost & Found desk in person
        • Log into the Lost & Found portal for updates
        • Check your email for future notifications

        We apologize for any inconvenience.
        
        Best regards,
        NEIU Lost & Found Team
        """.formatted(
        safe(lostItem.getOwnerName()),
        itemTitle,
        reason);
    
    String reporterSubject = "⚠️ Handoff Cancelled - " + itemTitle;
    String reporterBody = """
        Dear %s,

        The handoff for the item you reported has been cancelled.

        ══════════════════════════════════════════════
        CANCELLATION DETAILS
        ══════════════════════════════════════════════
        Item: %s
        Location Found: %s%s
        
        ══════════════════════════════════════════════

        The item remains in our system and we will continue working to return it to its owner.
        
        Thank you for your patience and understanding.
        
        Best regards,
        NEIU Lost & Found Team
        """.formatted(
        safe(foundItem.getReporterName()),
        itemTitle,
        safe(foundItem.getLocation()),
        reason);
    
    sendEmail(lostItem.getOwnerEmail(), ownerSubject, ownerBody);
    sendEmail(foundItem.getReporterEmail(), reporterSubject, reporterBody);
  }
  
  private void notifyPending(HandoffQueue handoff, LostItem lostItem, FoundItem foundItem, String itemTitle) {
    String ownerSubject = "⏳ Handoff Pending - " + itemTitle;
    String ownerBody = """
        Dear %s,

        A handoff request has been initiated for your lost item. Our team is reviewing the details 
        and will schedule the handoff shortly.

        ══════════════════════════════════════════════
        STATUS UPDATE
        ══════════════════════════════════════════════
        Item: %s
        Status: Pending
        %s
        
        ══════════════════════════════════════════════

        You will receive another notification once the handoff has been scheduled with specific 
        date, time, and location details.

        Thank you for your patience!
        
        Best regards,
        NEIU Lost & Found Team
        """.formatted(
        safe(lostItem.getOwnerName()),
        itemTitle,
        buildHandoffDetails(handoff));
    
    String reporterSubject = "⏳ Handoff Pending - " + itemTitle;
    String reporterBody = """
        Dear %s,

        A handoff request has been initiated for the item you reported. Our team is working 
        on scheduling the return to its owner.

        ══════════════════════════════════════════════
        STATUS UPDATE
        ══════════════════════════════════════════════
        Item: %s
        Location Found: %s
        Status: Pending
        
        ══════════════════════════════════════════════

        You will receive a notification once the handoff has been scheduled or completed.
        
        Thank you for your contribution!
        
        Best regards,
        NEIU Lost & Found Team
        """.formatted(
        safe(foundItem.getReporterName()),
        itemTitle,
        safe(foundItem.getLocation()));
    
    sendEmail(lostItem.getOwnerEmail(), ownerSubject, ownerBody);
    sendEmail(foundItem.getReporterEmail(), reporterSubject, reporterBody);
  }
  
  private void notifyGenericUpdate(HandoffQueue handoff, LostItem lostItem, FoundItem foundItem, String itemTitle) {
    String subject = "📝 Handoff Update - " + itemTitle;
    String details = buildHandoffDetails(handoff);
    
    String ownerBody = """
        Dear %s,

        There has been an update to your handoff.

        ══════════════════════════════════════════════
        UPDATED DETAILS
        ══════════════════════════════════════════════
        Item: %s
        %s
        
        ══════════════════════════════════════════════

        Please review the updated information above.
        
        Best regards,
        NEIU Lost & Found Team
        """.formatted(
        safe(lostItem.getOwnerName()),
        itemTitle,
        details);
    
    String reporterBody = """
        Dear %s,

        There has been an update to the handoff for the item you reported.

        ══════════════════════════════════════════════
        UPDATED DETAILS
        ══════════════════════════════════════════════
        Item: %s
        %s
        
        ══════════════════════════════════════════════

        Thank you for your patience.
        
        Best regards,
        NEIU Lost & Found Team
        """.formatted(
        safe(foundItem.getReporterName()),
        itemTitle,
        details);
    
    sendEmail(lostItem.getOwnerEmail(), subject, ownerBody);
    sendEmail(foundItem.getReporterEmail(), subject, reporterBody);
  }

  private void sendToParticipants(List<String> recipients, String subject, String body) {
    recipients.stream()
        .filter(Objects::nonNull)
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .distinct()
        .forEach(to -> sendEmail(to, subject, body));
  }

  private void sendEmail(String to, String subject, String body) {
    if (!notificationsEnabled) {
      log.info("Notifications disabled. Skipping email to {} with subject '{}'", to, subject);
      return;
    }
    try {
      SimpleMailMessage message = new SimpleMailMessage();
      message.setTo(to);
      message.setFrom(fromAddress);
      message.setSubject(subject);
      message.setText(body);
      mailSender.send(message);
      log.info("Sent notification email to {}", to);
    } catch (MailException ex) {
      log.warn("Unable to send email to {}: {}", to, ex.getMessage());
    }
  }

  private String buildHandoffDetails(HandoffQueue handoff) {
    StringBuilder details = new StringBuilder();
    if (handoff.getScheduledHandoffTime() != null) {
      details.append("\nWhen: ")
          .append(formatDate(handoff.getScheduledHandoffTime()));
    }
    if (handoff.getHandoffLocation() != null && !handoff.getHandoffLocation().isBlank()) {
      details.append("\nWhere: ").append(handoff.getHandoffLocation());
    }
    if (handoff.getNotes() != null && !handoff.getNotes().isBlank()) {
      details.append("\nNotes: ").append(handoff.getNotes());
    }
    if (handoff.getAssignedTo() != null && !handoff.getAssignedTo().isBlank()) {
      details.append("\nAssigned Staff: ").append(handoff.getAssignedTo());
    }
    details.append("\nCurrent Status: ").append(handoff.getStatus());
    return details.toString();
  }

  private String formatDate(Date date) {
    return DATE_TIME_FORMAT.format(date);
  }

  private String safe(String value) {
    return value == null ? "N/A" : value;
  }
}
