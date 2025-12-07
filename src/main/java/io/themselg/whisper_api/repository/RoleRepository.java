
package io.themselg.whisper_api.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import io.themselg.whisper_api.models.Role;
import io.themselg.whisper_api.models.ERole;

public interface RoleRepository extends MongoRepository<Role, String> {
  Optional<Role> findByName(ERole name);
}
