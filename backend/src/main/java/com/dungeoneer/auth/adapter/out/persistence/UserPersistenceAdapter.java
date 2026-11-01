package com.dungeoneer.auth.adapter.out.persistence;

import com.dungeoneer.auth.application.port.out.UserRepositoryPort;
import com.dungeoneer.auth.domain.model.User;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class UserPersistenceAdapter implements UserRepositoryPort {

    private final UserRepository userRepository;

    public UserPersistenceAdapter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User save(User user) {
        if (user.getId() != null) {
            Optional<UserJpaEntity> existing = userRepository.findById(user.getId());
            if (existing.isPresent()) {
                UserJpaEntity entity = existing.get();
                UserMapper.updateEntity(entity, user);
                UserJpaEntity saved = userRepository.save(entity);
                return UserMapper.toDomain(saved);
            }
        }
        UserJpaEntity entity = UserMapper.toJpaEntity(user);
        UserJpaEntity saved = userRepository.save(entity);
        return UserMapper.toDomain(saved);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return userRepository.findById(id).map(UserMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email).map(UserMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByNickname(String nickname) {
        return userRepository.existsByNickname(nickname);
    }
}
