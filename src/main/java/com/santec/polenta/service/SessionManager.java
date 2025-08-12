package com.santec.polenta.service;

import org.springframework.stereotype.Component;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles the management of initialized sessions.
 */
@Component
public class SessionManager {
    private final Set<String> initializedSessions = ConcurrentHashMap.newKeySet();

    public void addSession(String sessionId) {
        initializedSessions.add(sessionId);
    }

    public boolean isSessionInitialized(String sessionId) {
        return initializedSessions.contains(sessionId);
    }

    public void clearSession(String sessionId) {
        initializedSessions.remove(sessionId);
    }
}
