package io.themselg.whisper_api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Registra el endpoint "/ws" al que los clientes se conectarán para iniciar la comunicación WebSocket.
        // .withSockJS() provee un fallback para navegadores que no soportan WebSockets.
        registry.addEndpoint("/ws").withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Define que los mensajes cuyo destino empiece con "/app" deben ser enrutados a métodos de manejo de mensajes (en los controladores).
        registry.setApplicationDestinationPrefixes("/app");
        // Define que los mensajes cuyo destino empiece con "/topic" o "/user" deben ser enrutados al message broker.
        // El broker se encarga de transmitir los mensajes a todos los clientes suscritos a esos destinos.
        registry.enableSimpleBroker("/topic", "/user");
    }
}
