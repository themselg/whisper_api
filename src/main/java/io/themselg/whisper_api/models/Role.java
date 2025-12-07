package io.themselg.whisper_api.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Document(collection = "roles")
@Getter
@Setter
@NoArgsConstructor
public class Role {
  @Id
  private String id;

  private ERole name;

  public Role(ERole name) {
    this.name = name;
  }
}