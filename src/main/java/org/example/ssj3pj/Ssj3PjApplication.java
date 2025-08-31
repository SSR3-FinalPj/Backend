package org.example.ssj3pj;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableKafka  // 다시 활성화
@EnableScheduling
public class Ssj3PjApplication {

    public static void main(String[] args) {
//        // Load .env file and set system properties
//        Dotenv dotenv = Dotenv.load();
//        dotenv.entries().forEach(entry -> {
//            System.setProperty(entry.getKey(), entry.getValue());
//        });
        SpringApplication.run(Ssj3PjApplication.class, args);
    }
}