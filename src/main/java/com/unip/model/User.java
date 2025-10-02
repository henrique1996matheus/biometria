package com.unip.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
public class User {

    private Long id;
    private String name;
    private String email;
    private Role role;
    private String faceId; // diretório ou hash do rosto
}
