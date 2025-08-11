package org.example.ssj3pj.services;

import org.example.ssj3pj.dto.kafkamessage.KafkaMessage;
import org.example.ssj3pj.entity.KafkaMessageEntity;
import org.example.ssj3pj.repository.KafkaMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class KafkaMessageService {
    private final KafkaMessageRepository repo;

    @Transactional
    public void save(KafkaMessage m) {
        KafkaMessageEntity e = new KafkaMessageEntity();
        e.setSource(m.source());
        e.setTs(m.ts());
        e.setTemp(m.temp());
        e.setHumidity(m.humidity());
        e.setRawJson(m.rawJson());
        repo.save(e);
    }
}
