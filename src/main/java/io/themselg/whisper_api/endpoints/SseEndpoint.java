package io.themselg.whisper_api.endpoints;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import io.themselg.whisper_api.security.services.UserDetailsImpl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.io.IOException;

@RestController
@RequestMapping("/api/stream")
@CrossOrigin(origins = "*", maxAge = 3600)
public class SseEndpoint {
    public static final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    @GetMapping("/subscribe")
    public SseEmitter subscribe() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = ((UserDetailsImpl) auth.getPrincipal()).getId();

        // Timeout muy largo (ej. 1 hora o Long.MAX_VALUE)
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        emitters.put(userId, emitter);
        try {
            emitter.send(SseEmitter.event()
                    .name("INIT")
                    .data("Conexión establecida para usuario: " + userId));
        } catch (IOException e) {
            emitters.remove(userId);
        }

        return emitter;
    }

    // Enviar evento a un usuario específico (Chat, Notificaciones)
    public static void sendEventToUser(String recipientId, String type, Object data) {
        SseEmitter emitter = emitters.get(recipientId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name(type)
                        .data(data));
            } catch (IOException e) {
                emitters.remove(recipientId);
            }
        }
    }

    // DTO interno para el payload de estado
    public static class StatusPayload {
        public String userId;
        public boolean isOnline;
    }
}