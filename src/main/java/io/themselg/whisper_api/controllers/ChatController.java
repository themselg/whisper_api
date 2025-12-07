package io.themselg.whisper_api.controllers;

import io.themselg.whisper_api.models.ChatMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessage chatMessage) {
        /*
         * Se utiliza SimpMessagingTemplate para enviar el mensaje a un destino específico por usuario.
         * Spring se encarga de resolver el destino a la sesión/conexión correcta del usuario "recipientId".
         * El cliente (React Native) deberá estar suscrito a "/user/queue/messages" para recibir estos mensajes.
         */
        messagingTemplate.convertAndSendToUser(
            chatMessage.getRecipientId(),
            "/queue/messages",
            chatMessage
        );
    }
}
