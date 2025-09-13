package org.example.ssj3pj.config.runner;

import lombok.RequiredArgsConstructor;
import org.example.ssj3pj.entity.MascotImage;
import org.example.ssj3pj.entity.User.Users;
import org.example.ssj3pj.repository.MascotImageRepository;
import org.example.ssj3pj.repository.UsersRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class InitMascotImageConfig {

    private final MascotImageRepository mascotImageRepository;

    @Bean
    public CommandLineRunner initMascotImage() {
        return args -> {
            // 1) 유저 생성 (없으면 추가)
            MascotImage mascotImage1 = mascotImageRepository.findByRegionCode("POI074")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI074")
                                    .mascotImageKey("yeongdeungpo.png")
                                    .build()
                    ));
            MascotImage mascotImage2 = mascotImageRepository.findByRegionCode("POI072")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI072")
                                    .mascotImageKey("yeongdeungpo.png")
                                    .build()
                    ));
            MascotImage mascotImage3 = mascotImageRepository.findByRegionCode("POI105")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI105")
                                    .mascotImageKey("yeongdeungpo.png")
                                    .build()
                    ));
            MascotImage mascotImage4 = mascotImageRepository.findByRegionCode("POI128")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI128")
                                    .mascotImageKey("seodaemun.png")
                                    .build()
                    ));

        };
    }
}
