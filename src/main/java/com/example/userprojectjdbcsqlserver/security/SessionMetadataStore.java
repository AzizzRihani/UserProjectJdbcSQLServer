package com.example.userprojectjdbcsqlserver.security;

import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SessionRegistry (en memoire) n'expose, pour chaque session, que son id, son principal et sa
 * derniere activite - pas le user-agent, l'IP, la date de creation, ni la duree d'inactivite. On les garde donc ici,
 * dans une table maison, alimentee au login (AuthService) et nettoyee automatiquement des qu'une
 * session se termine (timeout ou logout) grace au callback sessionDestroyed ci-dessous.
 *
 * La duree d'inactivite est celle du parametre interne security.sessionTimeoutMinutes AU MOMENT du login : modifier
 * le parametre ensuite ne change que les nouvelles sessions, et /auth/sessions affiche la bonne expiration pour chacune.
 */
@Component
public class SessionMetadataStore implements HttpSessionListener {

    public record SessionMetadata(Instant creationTime, String userAgent, String ipAddress, Duration timeout) {
    }

    private final Map<String, SessionMetadata> metadataBySessionId = new ConcurrentHashMap<>();

    public void put(String sessionId, String userAgent, String ipAddress, Duration timeout) {
        metadataBySessionId.put(sessionId, new SessionMetadata(Instant.now(), userAgent, ipAddress, timeout));
    }

    public SessionMetadata get(String sessionId) {
        return metadataBySessionId.get(sessionId);
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        metadataBySessionId.remove(event.getSession().getId());
    }
}
