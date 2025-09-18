package org.example.ssj3pj.dto.response;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class JobResultCreatedEvent extends ApplicationEvent {
    private final Long jobId;
    private final boolean isInitial;

    public JobResultCreatedEvent(Object source, Long jobId, boolean isInitial) {
        super(source);
        this.jobId = jobId;
        this.isInitial = isInitial;
    }
}
