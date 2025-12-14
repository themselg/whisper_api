package io.themselg.whisper_api.models;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "messages")
public class ChatMessage {

    @Id
    private String id; // ID único del mensaje en la BD
    private String content;
    private String senderId;
    private String recipientId;
    private MessageType Type;
    private Date timestamp;

    @Field("isRead")
    @JsonProperty("isRead")
    private boolean read;

    private String replyToId;       // ID del mensaje original
    private String replyToName;     // Nombre de a quien respondes
    private String replyToText;     // El texto o resumen
    private MessageType replyToType; // El tipo del mensaje original
}