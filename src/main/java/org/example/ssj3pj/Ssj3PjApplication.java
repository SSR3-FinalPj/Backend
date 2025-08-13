package org.example.ssj3pj;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka  // 다시 활성화
public class Ssj3PjApplication {

    public static void main(String[] args) {
        SpringApplication.run(Ssj3PjApplication.class, args);
    }

}
