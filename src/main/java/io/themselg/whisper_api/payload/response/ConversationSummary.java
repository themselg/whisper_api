package io.themselg.whisper_api.payload.response;

import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConversationSummary {
    private String contactId;      // ID del otro usuario
    private String username;       // Nombre del otro usuario
    private String email;          // Email
    private String lastMessage;    // Contenido del último mensaje
    private Date timestamp;        // Fecha del último mensaje
    private String lastSenderId;   // Para saber si fui yo quien escribió al final
    private long unreadCount;
    private String displayName;
    private String profilePicture;
    private String lastMessageType;
}