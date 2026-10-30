package com.dungeoneer.auth.adapter.out.persistence;

import com.dungeoneer.auth.domain.model.Role;
import com.dungeoneer.auth.domain.model.User;

import java.util.List;
import java.util.stream.Collectors;

public final class UserMapper {

    private UserMapper() {}

    public static UserJpaEntity toJpaEntity(User domain) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.setId(domain.getId());
        entity.setEmail(domain.getEmail());
        entity.setNickname(domain.getNickname());
        entity.setBirthdate(domain.getBirthdate());
        entity.setPasswordHash(domain.getPasswordHash());
        entity.setRoles(domain.getRoles().stream().map(Enum::name).collect(Collectors.toList()));
        return entity;
    }

    public static User toDomain(UserJpaEntity entity) {
        List<Role> roles = entity.getRoles() != null
                ? entity.getRoles().stream().map(Role::valueOf).collect(Collectors.toList())
                : List.of(Role.PLAYER);

        return new User(
                entity.getId(),
                entity.getEmail(),
                entity.getNickname(),
                entity.getBirthdate(),
                entity.getPasswordHash(),
                roles
        );
    }
}
