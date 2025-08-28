package org.example.ssj3pj.config.runner;

import lombok.RequiredArgsConstructor;
import org.example.ssj3pj.entity.Job;
import org.example.ssj3pj.entity.JobResult;
import org.example.ssj3pj.entity.User.Users;
import org.example.ssj3pj.repository.JobRepository;
import org.example.ssj3pj.repository.JobResultRepository;
import org.example.ssj3pj.repository.UsersRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class InitUserConfig {

    private final UsersRepository usersRepository;
    private final JobRepository jobRepository;
    private final JobResultRepository jobResultRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initUserAndSampleVideo() {
        return args -> {
            // 1) 유저 생성 (없으면 추가)
            Users user = usersRepository.findByUsername("testuser")
                    .orElseGet(() -> usersRepository.save(
                            Users.builder()
                                    .username("testuser")
                                    .passwordHash(passwordEncoder.encode("test1234"))
                                    .build()
                    ));

        };
    }
}
