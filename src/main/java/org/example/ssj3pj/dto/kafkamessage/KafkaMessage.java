package org.example.ssj3pj.dto.kafkamessage;

public record KafkaMessage(String source, long ts, Double temp, Double humidity, String rawJson) {}
