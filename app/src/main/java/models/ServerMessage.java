package models;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Builder;
import lombok.Data;
import orsc.enumerations.MessageType;

@Builder
@Data
public class ServerMessage {

  // TODO: add other fields from MessageCallback as needed

  private final Instant timestamp;

  private final MessageType messageType;

  private final String sender;

  private final String message;

  private final AtomicReference<String> messageToLowerReference = new AtomicReference<>();

  public long getChronoUnitsSinceMessage(ChronoUnit chronoUnit) {
    return chronoUnit.between(timestamp, Instant.now());
  }

  public int getSecondsSinceMessage() {
    return (int) getChronoUnitsSinceMessage(ChronoUnit.SECONDS);
  }

  public boolean isMessageTypeEqualTo(MessageType otherMessageType) {
    return this.messageType == otherMessageType;
  }

  public boolean isMessageContainsCaseInsensitive(String match) {
    String messageToLower = this.messageToLowerReference.get();
    if (messageToLower == null) {
      messageToLower = this.message.toLowerCase(Locale.ENGLISH);
      this.messageToLowerReference.set(messageToLower);
    }
    return messageToLower.contains(match.toLowerCase());
  }
}
