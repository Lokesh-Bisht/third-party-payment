package dev.lokeshbisht.intent_service.service.lock;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "lock.intent-status.retry")
public class IntentStatusLockRetryProperties {

    private Duration minBackoff;

    private int maxAttempts;

    private Duration maxBackoff;
}
