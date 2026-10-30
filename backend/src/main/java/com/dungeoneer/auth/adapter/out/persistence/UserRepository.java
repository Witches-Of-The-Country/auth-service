package com.dungeoneer.auth.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserJpaEntity, UUID> {
    Optional<UserJpaEntity> findByEmail(String email);
    Optional<UserJpaEntity> findByNickname(String nickname);
    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);
}
