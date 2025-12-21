package dev.lokeshbisht.intent_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatusReason {

    private String statusReasonCode;

    private String description;
}
