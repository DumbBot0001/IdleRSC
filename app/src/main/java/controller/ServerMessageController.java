package controller;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;
import models.ServerMessage;

public class ServerMessageController {
  private static final int MAX_RECENT_SERVER_MESSAGES = 12;

  private ConcurrentLinkedQueue<ServerMessage> recentServerMesages = new ConcurrentLinkedQueue<>();

  public void addServerMessage(ServerMessage serverMessage) {
    System.out.println("Adding " + serverMessage);

    // Add recent messages to the queue
    recentServerMesages.add(serverMessage);

    // Trim any excess messages to prevent large memory consumption
    while (recentServerMesages.size() >= MAX_RECENT_SERVER_MESSAGES) {
      recentServerMesages.poll();
    }
  }

  public boolean recentMessagesMatchesAny(Predicate<ServerMessage> serverMessagePredicate) {
    return recentServerMesages.stream().anyMatch(serverMessagePredicate::test);
  }
}
