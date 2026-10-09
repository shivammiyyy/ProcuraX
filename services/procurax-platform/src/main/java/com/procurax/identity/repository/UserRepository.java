package com.procurax.identity.repository;

import com.procurax.identity.domain.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByAuthProviderAndProviderSubject(String authProvider, String providerSubject);

    Optional<User> findByEmail(String email);
}
