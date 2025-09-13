package com.example.demo1.entity;

import jakarta.persistence.*;
import lombok.*;


@Entity(name = "users")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false)
    private Long chatId;
    private String username;
    private String phone;

    @Enumerated(EnumType.STRING)
    private Role role;
}
