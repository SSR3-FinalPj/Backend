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

            // 2) Job 생성 (테스트 용도)
            Job job = Job.builder()
                    .user(user)
                    .status("COMPLETED")
                    .purpose("테스트 영상 스트리밍")
                    .locationCode("SEOUL")
                    .sourceImageKey("images/sample.png")
                    .build();
            jobRepository.save(job);

            // 3) JobResult 생성 (S3에 올려둔 테스트 영상 key 사용)
            if (jobResultRepository.findAllByJobId(job.getId()).isEmpty()) {
                JobResult result = JobResult.builder()
                        .job(job)
                        .status("COMPLETED")
                        .resultType("video")
                        .resultKey("video/.mp4") // ⚠️ 실제 S3에 올려둔 파일 경로
                        .promptText("테스트 영상")
                        .build();
                jobResultRepository.save(result);

                System.out.println("샘플 JobResult 생성 완료: resultId = " + result.getId());
            }
        };
    }
}
