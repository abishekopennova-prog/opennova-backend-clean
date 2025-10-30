package com.opennova.repository;

import com.opennova.model.User;
import com.opennova.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findByResetToken(String resetToken);
    List<User> findByRole(UserRole role);
    List<User> findByRoleIn(List<UserRole> roles);
    List<User> findByIsActiveTrue();
    long countByIsActive(boolean isActive);
}