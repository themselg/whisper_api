package io.themselg.whisper_api.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import io.themselg.whisper_api.models.User;


public interface UserRepository extends MongoRepository<User, String> {
  Optional<User> findByUsername(String username);

  Optional<User> findById(String id);

  Boolean existsByUsername(String username);

  Boolean existsByEmail(String email);
}
