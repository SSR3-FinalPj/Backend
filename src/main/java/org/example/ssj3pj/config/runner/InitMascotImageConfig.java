package org.example.ssj3pj.config.runner;

import lombok.RequiredArgsConstructor;
import org.example.ssj3pj.entity.MascotImage;
import org.example.ssj3pj.repository.MascotImageRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class InitMascotImageConfig {

    private final MascotImageRepository mascotImageRepository;

    @Bean
    public CommandLineRunner initMascotImage() {
        return args -> {
            // 1) 이미지 생성 (없으면 추가)
            mascotImageRepository.findByRegionCode("POI001")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI001")
                                    .mascotImageKey("seoul.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI002")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI002")
                                    .mascotImageKey("dongdaemun.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI003")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI003")
                                    .mascotImageKey("jung.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI004")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI004")
                                    .mascotImageKey("seoul.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI005")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI005")
                                    .mascotImageKey("songpa.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI006")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI006")
                                    .mascotImageKey("jongno.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI007")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI007")
                                    .mascotImageKey("mapo.jpg")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI008")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI008")
                                    .mascotImageKey("jongno.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI009")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI009")
                                    .mascotImageKey("jongno.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI010")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI010")
                                    .mascotImageKey("jongno.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI011")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI011")
                                    .mascotImageKey("gangdong.jpg")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI012")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI012")
                                    .mascotImageKey("jongno.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI013")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI013")
                                    .mascotImageKey("\n" +
                                            "geumcheon.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI014")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI014")
                                    .mascotImageKey("seoul.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI015")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI015")
                                    .mascotImageKey("gwangjin.jpg")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI016")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI016")
                                    .mascotImageKey("gangdong.jpg")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI017")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI017")
                                    .mascotImageKey("seoul.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI018")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI018")
                                    .mascotImageKey("seoul.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI019")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI019")
                                    .mascotImageKey("guro.jpg")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI020")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI020")
                                    .mascotImageKey("guro.jpg")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI021")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI021")
                                    .mascotImageKey("gwangjin.jpg")
                                    .build()
                    ));
//            mascotImageRepository.findByRegionCode("POI022")
//                    .orElseGet(() -> mascotImageRepository.save(
//                            MascotImage.builder()
//                                    .regionCode("POI022")
//                                    .mascotImageKey("")
//                                    .build()
//                    ));
            mascotImageRepository.findByRegionCode("POI023")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI023")
                                    .mascotImageKey("guro.jpg")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI024")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI024")
                                    .mascotImageKey("dongdaemun.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI025")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI025")
                                    .mascotImageKey("seongdong.jpg")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI026")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI026")
                                    .mascotImageKey("gangbuk.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI027")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI027")
                                    .mascotImageKey("gangseo.png")
                                    .build()
                    ));
//            mascotImageRepository.findByRegionCode("POI028")
//                    .orElseGet(() -> mascotImageRepository.save(
//                            MascotImage.builder()
//                                    .regionCode("POI028")
//                                    .mascotImageKey("")
//                                    .build()
//                    ));
            mascotImageRepository.findByRegionCode("POI029")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI029")
                                    .mascotImageKey("dongjak.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI030")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI030")
                                    .mascotImageKey("seoul.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI031")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI031")
                                    .mascotImageKey("gwanak.jpg")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI032")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI032")
                                    .mascotImageKey("gangseo.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI033")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI033")
                                    .mascotImageKey("jung.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI034")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI034")
                                    .mascotImageKey("seoul.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI035")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI035")
                                    .mascotImageKey("seongbuk.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI036")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI036")
                                    .mascotImageKey("gangbuk.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI037")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI037")
                                    .mascotImageKey("seoul.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI038")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI038")
                                    .mascotImageKey("guro.jpg")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI039")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI039")
                                    .mascotImageKey("gwanak.jpg")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI040")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI040")
                                    .mascotImageKey("seodaemun.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI041")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI041")
                                    .mascotImageKey("seoul.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI042")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI042")
                                    .mascotImageKey("seoul.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI043")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI043")
                                    .mascotImageKey("eunpyeong.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI044")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI044")
                                    .mascotImageKey("yangcheon.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI045")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI045")
                                    .mascotImageKey("seongdong.jpg")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI046")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI046")
                                    .mascotImageKey("seoul.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI047")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI047")
                                    .mascotImageKey("seoul.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI048")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI048")
                                    .mascotImageKey("songpa.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI049")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI049")
                                    .mascotImageKey("dongdaemun.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI050")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI050")
                                    .mascotImageKey("gangdong.jpg")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI051")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI051")
                                    .mascotImageKey("dongjak.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI052")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI052")
                                    .mascotImageKey("seodaemun.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI053")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI053")
                                    .mascotImageKey("mapo.jpg")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI054")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI054")
                                    .mascotImageKey("jongno.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI055")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI055")
                                    .mascotImageKey("mapo.jpg")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI056")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI056")
                                    .mascotImageKey("dongdaemun.png")
                                    .build()
                    ));
//            mascotImageRepository.findByRegionCode("POI057")
//                    .orElseGet(() -> mascotImageRepository.save(
//                            MascotImage.builder()
//                                    .regionCode("POI057")
//                                    .mascotImageKey("")
//                                    .build()
//                    ));
            mascotImageRepository.findByRegionCode("POI058")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI058")
                                    .mascotImageKey("songpa.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI059")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI059")
                                    .mascotImageKey("seoul.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI060")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI060")
                                    .mascotImageKey("jongno.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI061")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI061")
                                    .mascotImageKey("gangseo.png")
                                    .build()
                    ));
//            mascotImageRepository.findByRegionCode("POI062")
//                    .orElseGet(() -> mascotImageRepository.save(
//                            MascotImage.builder()
//                                    .regionCode("POI062")
//                                    .mascotImageKey("")
//                                    .build()
//                    ));
            mascotImageRepository.findByRegionCode("POI063")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI063")
                                    .mascotImageKey("dongjak.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI064")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI064")
                                    .mascotImageKey("jung.png")
                                    .build()
                    ));
//            mascotImageRepository.findByRegionCode("POI065")
//                    .orElseGet(() -> mascotImageRepository.save(
//                            MascotImage.builder()
//                                    .regionCode("POI065")
//                                    .mascotImageKey("")
//                                    .build()
//                    ));
            mascotImageRepository.findByRegionCode("POI066")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI066")
                                    .mascotImageKey("jongno.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI067")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI067")
                                    .mascotImageKey("jongno.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI068")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI068")
                                    .mascotImageKey("seongdong.jpg")
                                    .build()
                    ));
//            mascotImageRepository.findByRegionCode("POI069")
//                    .orElseGet(() -> mascotImageRepository.save(
//                            MascotImage.builder()
//                                    .regionCode("POI069")
//                                    .mascotImageKey("")
//                                    .build()
//                    ));
            mascotImageRepository.findByRegionCode("POI070")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI070")
                                    .mascotImageKey("dobong.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI071")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI071")
                                    .mascotImageKey("seoul.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI072")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI072")
                                    .mascotImageKey("yeongdeungpo.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI073")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI073")
                                    .mascotImageKey("mapo.jpg")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI074")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI074")
                                    .mascotImageKey("yeongdeungpo.png")
                                    .build()
                    ));
//            mascotImageRepository.findByRegionCode("POI075")
//                    .orElseGet(() -> mascotImageRepository.save(
//                            MascotImage.builder()
//                                    .regionCode("POI075")
//                                    .mascotImageKey("")
//                                    .build()
//                    ));
            mascotImageRepository.findByRegionCode("POI076")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI076")
                                    .mascotImageKey("seoul.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI077")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI077")
                                    .mascotImageKey("seoul.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI078")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI078")
                                    .mascotImageKey("jongno.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI079")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI079")
                                    .mascotImageKey("dobong.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI080")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI080")
                                    .mascotImageKey("seoul.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI081")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI081")
                                    .mascotImageKey("dongdaemun.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI082")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI082")
                                    .mascotImageKey("seoul.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI083")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI083")
                                    .mascotImageKey("dongdaemun.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI084")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI084")
                                    .mascotImageKey("mapo.jpg")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI085")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI085")
                                    .mascotImageKey("gangseo.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI086")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI086")
                                    .mascotImageKey("guro.jpg")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI087")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI087")
                                    .mascotImageKey("gangdong.jpg")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI088")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI088")
                                    .mascotImageKey("jongno.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI089")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI089")
                                    .mascotImageKey("seoul.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI090")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI090")
                                    .mascotImageKey("mapo.jpg")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI091")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI091")
                                    .mascotImageKey("jung.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI092")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI092")
                                    .mascotImageKey("seoul.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI093")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI093")
                                    .mascotImageKey("gwangjin.jpg")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI094")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI094")
                                    .mascotImageKey("mapo.jpg")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI095")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI095")
                                    .mascotImageKey("seoul.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI096")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI096")
                                    .mascotImageKey("gangbuk.png")
                                    .build()
                    ));
//            mascotImageRepository.findByRegionCode("POI097")
//                    .orElseGet(() -> mascotImageRepository.save(
//                            MascotImage.builder()
//                                    .regionCode("POI097")
//                                    .mascotImageKey("")
//                                    .build()
//                    ));
            mascotImageRepository.findByRegionCode("POI098")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI098")
                                    .mascotImageKey("seoul.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI099")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI099")
                                    .mascotImageKey("jung.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI100")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI100")
                                    .mascotImageKey("gwacheon.jpg") //과천시 마스코트 추가해야함
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI101")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI101")
                                    .mascotImageKey("seongdong.jpg")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI102")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI102")
                                    .mascotImageKey("gwangjin.jpg")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI103")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI103")
                                    .mascotImageKey("yeongdeungpo.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI104")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI104")
                                    .mascotImageKey("gwangjin.jpg")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI105")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI105")
                                    .mascotImageKey("yeongdeungpo.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI106")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI106")
                                    .mascotImageKey("mapo.jpg")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI107")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI107")
                                    .mascotImageKey("seongdong.jpg")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI108")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI108")
                                    .mascotImageKey("seoul.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI109")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI109")
                                    .mascotImageKey("songpa.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI110")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI110")
                                    .mascotImageKey("songpa.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI111")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI111")
                                    .mascotImageKey("songpa.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI112")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI112")
                                    .mascotImageKey("seoul.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI113")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI113")
                                    .mascotImageKey("jongno.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI114")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI114")
                                    .mascotImageKey("jung.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI115")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI115")
                                    .mascotImageKey("jung.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI116")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI116")
                                    .mascotImageKey("jongno.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI117")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI117")
                                    .mascotImageKey("yangcheon.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI118")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI118")
                                    .mascotImageKey("songpa.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI119")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI119")
                                    .mascotImageKey("songpa.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI120")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI120")
                                    .mascotImageKey("songpa.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI121")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI121")
                                    .mascotImageKey("songpa.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI122")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI122")
                                    .mascotImageKey("seodaemun.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI123")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI123")
                                    .mascotImageKey("dongjak.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI124")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI124")
                                    .mascotImageKey("seodaemun.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI125")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI125")
                                    .mascotImageKey("guro.jpg")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI126")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI126")
                                    .mascotImageKey("yeongdeungpo.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI127")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI127")
                                    .mascotImageKey("songpa.png")
                                    .build()
                    ));
            mascotImageRepository.findByRegionCode("POI128")
                    .orElseGet(() -> mascotImageRepository.save(
                            MascotImage.builder()
                                    .regionCode("POI128")
                                    .mascotImageKey("seodaemun.png")
                                    .build()
                    ));
        };
    }
}