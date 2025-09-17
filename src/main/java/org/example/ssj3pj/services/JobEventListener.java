package org.example.ssj3pj.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ssj3pj.dto.response.JobResultCreatedEvent;
import org.example.ssj3pj.scheduler.DynamicVideoScheduler;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobEventListener {

    private final DynamicVideoScheduler dynamicVideoScheduler;

    @TransactionalEventListener
    public void handleJobCreatedEvent(JobResultCreatedEvent event) {
        log.info("Transaction committed for job {}. Starting scheduler.", event.getJobId());
        if (event.isInitial()) {
            dynamicVideoScheduler.startInitialJob(event.getJobId());
        } else {
            dynamicVideoScheduler.startRevisionJob(event.getJobId());
        }
    }
}
