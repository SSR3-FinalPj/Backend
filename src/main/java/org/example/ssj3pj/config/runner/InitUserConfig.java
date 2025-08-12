package org.example.ssj3pj.config.runner;
import org.example.ssj3pj.entity.User.Users;
import org.example.ssj3pj.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class InitUserConfig {

    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initUser() {
        return args -> {
            if (usersRepository.findByUsername("testuser").isEmpty()) {
                usersRepository.save(
                        Users.builder()
                                .username("testuser")
                                .passwordHash(passwordEncoder.encode("test1234"))
                                .build()
                );
            }
        };
    }
}
