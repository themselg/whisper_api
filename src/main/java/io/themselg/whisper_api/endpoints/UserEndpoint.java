package io.themselg.whisper_api.endpoints;

import io.themselg.whisper_api.models.User;
import io.themselg.whisper_api.repository.UserRepository;
import io.themselg.whisper_api.security.services.UserDetailsImpl;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*", maxAge = 3600)
public class UserEndpoint {

    @Autowired
    UserRepository userRepository;

    @GetMapping("/search/{username}")
    // A. Buscar usuario por nombre de usuario
    public ResponseEntity<?> getUserByUsername(@PathVariable String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Retornamos un objeto simplificado (DTO) con el ID
            return ResponseEntity.ok(new UserSummary(user.getId(), user.getUsername(), user.getEmail()));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/id/{id}")
    // B. Obtener perfil de usuario por ID
    public ResponseEntity<?> getUserProfile(@PathVariable String id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            return ResponseEntity.ok(new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getProfilePicture(),
                user.getEmail()
            ));
        }
        return ResponseEntity.notFound().build();
    }

    // C. Actualizar mi perfil
    @PutMapping("/profile")
    public ResponseEntity<?> updateMyProfile(@RequestBody UpdateProfileRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String myId = ((UserDetailsImpl) auth.getPrincipal()).getId();

        return userRepository.findById(myId).map(user -> {
            if (request.getDisplayName() != null) user.setDisplayName(request.getDisplayName());
            if (request.getProfilePicture() != null) user.setProfilePicture(request.getProfilePicture());
            
            userRepository.save(user);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }

    // DTOs
    @Data @AllArgsConstructor
    static class UserProfileResponse {
        private String id;
        private String username;
        private String displayName;
        private String profilePicture;
        private String email;
    }

    @Data
    static class UpdateProfileRequest {
        private String displayName;
        private String profilePicture; // Base64 string
    }

    @Data
    @AllArgsConstructor
    static class UserSummary {
        private String id;
        private String username;
        private String email;
    }
}