package dev.lokeshbisht.intent_service.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.lokeshbisht.intent_service.enums.IntentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateIntentResponse {

    private String intentId;

    private IntentStatus status;

    private StatusReason statusReason;

    @JsonIgnore
    private Boolean isConflicted;

    private OffsetDateTime createdAt;

    @JsonProperty(namespace = "statusUpdatedAt")
    private OffsetDateTime updatedAt;
}
