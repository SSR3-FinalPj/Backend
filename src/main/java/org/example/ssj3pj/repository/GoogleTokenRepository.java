package org.example.ssj3pj.repository;

import org.example.ssj3pj.entity.User.GoogleToken;
import org.example.ssj3pj.entity.User.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GoogleTokenRepository extends JpaRepository<GoogleToken, Long> {
    Optional<GoogleToken> findByUser(Users user);
}
