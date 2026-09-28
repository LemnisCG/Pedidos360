package cl.duoc.pedidos360.usuario.repository;

import cl.duoc.pedidos360.usuario.model.OAuthSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OAuthSessionRepository extends JpaRepository<OAuthSession, String> {}
