package br.com.fiap.soat15.tc_oficina.adapter.out.persistence.repository;

import br.com.fiap.soat15.tc_oficina.domain.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsername(String username);
    Optional<Usuario> findByCpf(String cpf);
}
