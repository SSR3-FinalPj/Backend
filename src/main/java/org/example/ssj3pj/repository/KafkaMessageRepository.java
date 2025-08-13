package org.example.ssj3pj.repository;

import org.example.ssj3pj.entity.KafkaMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KafkaMessageRepository extends JpaRepository<KafkaMessageEntity, Long> {}
