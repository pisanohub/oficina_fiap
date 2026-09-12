package br.com.fiap.soat15.tc_oficina.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "TB_USUARIOS")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String cpf;

    @Column(nullable = false)
    private String password;
}
