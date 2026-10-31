package net.java21.fourcut.auth.user;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {}
