package io.themselg.whisper_api.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import io.themselg.whisper_api.models.ChatMessage;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {
    // Mongo automaticatemente genera la consulta basada en el nombre del método.
    // Busca mensajes donde: (Emisor es A y Receptor es B) O (Emisor es B y Receptor es A)
    // Esto recupera el historial completo de la conversación.
    List<ChatMessage> findBySenderIdAndRecipientIdOrSenderIdAndRecipientIdOrderByTimestampAsc(
        String senderId1, String recipientId1, 
        String senderId2, String recipientId2
    );
}