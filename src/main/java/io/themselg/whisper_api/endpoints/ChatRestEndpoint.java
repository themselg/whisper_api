package io.themselg.whisper_api.endpoints;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.themselg.whisper_api.models.ChatMessage;
import io.themselg.whisper_api.models.MessageType; 
import io.themselg.whisper_api.models.User;
import io.themselg.whisper_api.payload.response.ConversationSummary;
import io.themselg.whisper_api.repository.ChatMessageRepository;
import io.themselg.whisper_api.repository.UserRepository;
import io.themselg.whisper_api.security.services.UserDetailsImpl;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ChatRestEndpoint {

    @Autowired
    ChatMessageRepository chatMessageRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    MongoTemplate mongoTemplate;

    // A. Enviar mensaje
    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@RequestBody ChatMessage message) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl sender = (UserDetailsImpl) auth.getPrincipal(); // Usuario autenticado
        
        // 1. Configurar datos del mensaje
        message.setSenderId(sender.getId());  // Usuario que lo envia
        message.setTimestamp(new Date());          // Fecha actual
        message.setRead(false);              // Inicialmente no leído
        
        // 2. Guardar en BD
        ChatMessage savedMessage = chatMessageRepository.save(message);
        
        // Se construye el payload de notificación
        // Se obtiene el username de quien envía
        String nameToDisplay = sender.getUsername(); 
        
        // Si tiene displayName, usarlo
        Optional<User> senderOpt = userRepository.findById(sender.getId());
        if (senderOpt.isPresent()) {
            User senderDetails = senderOpt.get();
            if (senderDetails.getDisplayName() != null && !senderDetails.getDisplayName().isEmpty()) {
                nameToDisplay = senderDetails.getDisplayName();
            }
        }

        // 3. Preparar la notificación SSE
        NotificationPayload payload = new NotificationPayload(
            sender.getId(),  // Remitente
            nameToDisplay,        // Nombre a mostrar
            message.getContent(), // Contenido
            message.getType()     // Tipo de mensaje
        );

        // 4. Enviamos el evento SSE
        SseEndpoint.sendEventToUser(
            message.getRecipientId(), // Destinatario
            "NEW_MESSAGE",       // Tipo de evento
            payload                   // Datos del mensaje
        );

        //System.out.println("[ChatRest] Mensaje enviado de " + sender.getId() + " a " + message.getRecipientId());
        
        return ResponseEntity.ok(savedMessage);
    }

    // B. Historial del chat
    @GetMapping("/history/{id}")
    public ResponseEntity<List<ChatMessage>> getChatHistory(@PathVariable String id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl sender = (UserDetailsImpl) auth.getPrincipal(); // Usuario autenticado

        // Recupera el historial completo entre los dos usuarios
        List<ChatMessage> history = chatMessageRepository
            .findBySenderIdAndRecipientIdOrSenderIdAndRecipientIdOrderByTimestampAsc(
                sender.getId(), id, 
                id, sender.getId()
            );

        //System.out.println("[ChatRest] Historial cargado entre " + sender.getUsername() + " y " + id + ".\nMensajes encontrados: " + history.size());
        return ResponseEntity.ok(history);
    }

    // C. Señal de escribiendo
    @PostMapping("/typing")
    public ResponseEntity<?> sendTypingSignal(@RequestBody TypingRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl sender = (UserDetailsImpl) auth.getPrincipal(); // Usuario autenticado

        TypingPayload payload = new TypingPayload(
            sender.getId(),                  // Remitente
            request.getIsTyping()  // Estado de escribiendo
        );


        SseEndpoint.sendEventToUser(
            request.getRecipientId(),  // Destinatario
            "TYPING",            // Tipo de evento
            payload                   // Payload ^^^^^
        );

        //System.out.println("[ChatRest] TYPING " + request.getRecipientId() + "por " + sender.getId() + ": " + request.getIsTyping());

        return ResponseEntity.ok().build();
    }

    // D. Marcar mensajes como leídos
    @PostMapping("/read/{contactId}")
    public ResponseEntity<?> markMessagesAsRead(@PathVariable String contactId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl sender = (UserDetailsImpl) auth.getPrincipal(); // Usuario autenticado

        Query query = new Query(Criteria
                .where("senderId").is(contactId) // Mensajes enviados por el contacto
                .and("recipientId").is(sender.getId())     // Mensajes recibidos por mí
                .and("isRead").is(false)); // Que no estén leídos
        
        Update update = new Update().set("isRead", true); // Marcar como leídos
        
        mongoTemplate.updateMulti(query, update, ChatMessage.class); // Actualiza múltiples documentos

        SseEndpoint.sendEventToUser(contactId, "READ_RECEIPT", sender.getId()); // Notificar al contacto

        //System.out.println("[ChatRest] Mensajes de " + contactId + " marcados como leídos por " + sender.getUsername());

        return ResponseEntity.ok().build();
    }

    // E. Obtener lista de conversaciones
    @GetMapping("/conversations")
    public ResponseEntity<?> getUserConversations() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            UserDetailsImpl sender = (UserDetailsImpl) auth.getPrincipal(); // Usuario autenticado

            //System.out.println("Cargando conversaciones para usuario: " + sender.getUsername());

            Aggregation aggregation = Aggregation.newAggregation(
                // A. Filtrar
                Aggregation.match(new Criteria().orOperator(
                    Criteria.where("senderId").is(sender.getId()),
                    Criteria.where("recipientId").is(sender.getId())
                )),
                // B. Ordenar
                Aggregation.sort(Sort.Direction.DESC, "timestamp"),
                
                // C. Proyección
                Aggregation.project("content", "timestamp", "senderId", "isRead")
                    .andExpression("cond(ifNull(type, false), type, ifNull(Type, 'TEXT'))").as("type") 
                    .andExpression("cond(eq(senderId, '" + sender.getId() + "'), recipientId, senderId)").as("partnerId")
                    .andExpression("cond(and(eq(recipientId, '" + sender.getId() + "'), eq(isRead, false)), 1, 0)").as("isUnread"),
                
                // D. Agrupar
                Aggregation.group("partnerId")
                    .first("content").as("lastMessage")
                    .first("timestamp").as("timestamp")
                    .first("senderId").as("lastSenderId")
                    .first("type").as("lastMessageType")
                    .sum("isUnread").as("unreadCount")
            );

            AggregationResults<Map> results = mongoTemplate.aggregate(aggregation, "messages", Map.class);
            List<Map> rawConversations = results.getMappedResults();
            
            //System.out.println("Conversaciones encontradas: " + rawConversations.size());

            // E. Obtener usuarios
            List<String> partnerIds = rawConversations.stream()
                    .map(c -> (String) c.get("_id"))
                    .collect(Collectors.toList());

            List<User> partners = userRepository.findAllById(partnerIds);
            Map<String, User> userMap = partners.stream()
                    .collect(Collectors.toMap(User::getId, user -> user));

            // F. Construir respuesta
            List<ConversationSummary> summaryList = new ArrayList<>();
            
            for (Map raw : rawConversations) {
                try {
                    String partnerId = (String) raw.get("_id");
                    User partner = userMap.get(partnerId);
                    
                    if (partner != null) {
                        Object typeObj = raw.get("lastMessageType");
                        String msgType = (typeObj != null) ? String.valueOf(typeObj) : "TEXT";
                        if ("null".equals(msgType)) msgType = "TEXT";
                        // -------------------------------

                        summaryList.add(new ConversationSummary(
                            partner.getId(),
                            partner.getUsername(),
                            partner.getEmail(),
                            (String) raw.get("lastMessage"),
                            (Date) raw.get("timestamp"),
                            (String) raw.get("lastSenderId"),
                            ((Number) raw.get("unreadCount")).longValue(),
                            partner.getDisplayName(),
                            partner.getProfilePicture(),
                            msgType
                        ));
                    }
                } catch (Exception innerEx) {
                    System.err.println("Error procesando chat ID " + raw.get("_id") + ": " + innerEx.getMessage());
                    innerEx.printStackTrace();
                }
            }
            
            summaryList.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
            return ResponseEntity.ok(summaryList);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    // DTOs internos`

    public static class TypingRequest {
        private String recipientId;
        private boolean isTyping;
        public String getRecipientId() { return recipientId; }
        public void setRecipientId(String recipientId) { this.recipientId = recipientId; }
        public boolean getIsTyping() { return isTyping; }
        public void setTyping(boolean isTyping) { this.isTyping = isTyping; }
    }

    public static class TypingPayload {
        private String senderId;
        private boolean isTyping;
        public TypingPayload(String senderId, boolean isTyping) {
            this.senderId = senderId;
            this.isTyping = isTyping;
        }
        public String getSenderId() { return senderId; }
        @JsonProperty("typing")
        public boolean isTyping() { return isTyping; }
    }

    public static class NotificationPayload {
        public String senderId;
        public String username;
        public String content;
        public MessageType type; 

        public NotificationPayload(String senderId, String username, String content, MessageType type) {
            this.senderId = senderId;
            this.username = username;
            this.content = content;
            this.type = type;
        }
    }
}