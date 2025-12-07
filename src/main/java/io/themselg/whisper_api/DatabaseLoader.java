package io.themselg.whisper_api;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import io.themselg.whisper_api.models.ERole;
import io.themselg.whisper_api.models.Role;
import io.themselg.whisper_api.repository.RoleRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DatabaseLoader implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) throws Exception {
        if (roleRepository.count() == 0) {
            roleRepository.save(new Role(ERole.ROLE_USER));
            roleRepository.save(new Role(ERole.ROLE_MODERATOR));
            roleRepository.save(new Role(ERole.ROLE_ADMIN));
        }
    }
}
