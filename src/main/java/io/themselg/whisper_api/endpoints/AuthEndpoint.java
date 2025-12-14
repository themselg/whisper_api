package io.themselg.whisper_api.endpoints;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.themselg.whisper_api.models.ERole;
import io.themselg.whisper_api.models.Role;
import io.themselg.whisper_api.models.User;
import io.themselg.whisper_api.payload.request.LoginRequest;
import io.themselg.whisper_api.payload.request.SignupRequest;
import io.themselg.whisper_api.payload.response.JwtResponse;
import io.themselg.whisper_api.payload.response.MessageResponse;
import io.themselg.whisper_api.repository.RoleRepository;
import io.themselg.whisper_api.repository.UserRepository;
import io.themselg.whisper_api.security.jwt.JwtUtils;
import io.themselg.whisper_api.security.services.UserDetailsImpl;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthEndpoint {
  @Autowired
  AuthenticationManager authenticationManager;

  @Autowired
  UserRepository userRepository;

  @Autowired
  RoleRepository roleRepository;

  @Autowired
  PasswordEncoder encoder;

  @Autowired
  JwtUtils jwtUtils;

  @PostMapping("/signin")
  // Se recive loginRequest como JSON
  public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

    // Envia las credenciales al AuthenticationManager para autenticar
    Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

    SecurityContextHolder.getContext().setAuthentication(authentication);

    // Genera JWT
    String jwt = jwtUtils.generateJwtToken(authentication);
    
    // Obtiene detalles del usuario autenticado (oid, username, email, roles)
    UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();    
    List<String> roles = userDetails.getAuthorities().stream()
        .map(item -> item.getAuthority())
        .collect(Collectors.toList());

    return ResponseEntity.ok(new JwtResponse(jwt, 
                         userDetails.getId(), 
                         userDetails.getUsername(), 
                         userDetails.getEmail(), 
                         roles));
  }

  @PostMapping("/signup")
  // Se recive signupRequest como JSON
  public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
    // Se verifica si ya existe:
    // 1. username
    if (userRepository.existsByUsername(signUpRequest.getUsername())) {
      return ResponseEntity
          .badRequest()
          .body(new MessageResponse("El nombre de usuario ya está en uso"));
    }
    // 2. email
    if (userRepository.existsByEmail(signUpRequest.getEmail())) {
      return ResponseEntity
          .badRequest()
          .body(new MessageResponse("El correo electrónico ya está en uso"));
    }

    // Crear nuevo usuario
    User user = new User(signUpRequest.getUsername(), 
                   signUpRequest.getEmail(),
                   encoder.encode(signUpRequest.getPassword()));

        // Si la peticion incluye:
        // 1. displayName
        if (signUpRequest.getDisplayName() != null) {
            user.setDisplayName(signUpRequest.getDisplayName());
        }
        // 2. profilePicture
        if (signUpRequest.getProfilePicture() != null) {
            user.setProfilePicture(signUpRequest.getProfilePicture());
        }
        // Se agregan al usuario antes de asignar roles

    // Asignación de roles
    // (En la implementación actual, solo se usa ROLE_USER)
    Set<String> strRoles = signUpRequest.getRole();
    Set<Role> roles = new HashSet<>();

    if (strRoles == null) {
      Role userRole = roleRepository.findByName(ERole.ROLE_USER)
          .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
      roles.add(userRole);
    } else {
      strRoles.forEach(role -> {
        switch (role) {
        case "admin":
          Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
              .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
          roles.add(adminRole);

          break;
        case "mod":
          Role modRole = roleRepository.findByName(ERole.ROLE_MODERATOR)
              .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
          roles.add(modRole);

          break;
        default:
          Role userRole = roleRepository.findByName(ERole.ROLE_USER)
              .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
          roles.add(userRole);
        }
      });
    }

    user.setRoles(roles);
    // Guardar en BD
    userRepository.save(user);

    return ResponseEntity.ok(new MessageResponse("Usuario registrado exitosamente"));
  }
}
