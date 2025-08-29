package org.example.ssj3pj.services.youtube;

import lombok.RequiredArgsConstructor;
import org.example.ssj3pj.dto.dashboard.JobResultDto;
import org.example.ssj3pj.repository.JobResultRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class JobResultService {

    private final JobResultRepository jobResultRepository;

    public List<JobResultDto> getUserJobResults(Long userId) {
        return jobResultRepository.findAllByUserId(userId).stream()
                .map(r -> new JobResultDto(r.getId(), r.getCreatedAt()))
                .toList();
    }
}
