package org.example.ssj3pj.repository;

import org.example.ssj3pj.entity.Job;
import org.example.ssj3pj.entity.Prompt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PromptRepository extends JpaRepository<Prompt, Long> {
}
