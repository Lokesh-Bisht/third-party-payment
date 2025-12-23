package dev.lokeshbisht.intent_service.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.lokeshbisht.intent_service.enums.IntentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateIntentStatusRequest {

    @NotNull(message = "Status can't be null.")
    @JsonProperty("status")
    private IntentStatus intentStatus;

    @NotNull
    @JsonProperty("statusReason")
    private StatusReasonRequestDto statusReasonRequestDto;
}
