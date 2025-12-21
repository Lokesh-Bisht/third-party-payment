package dev.lokeshbisht.intent_service.controller;

import dev.lokeshbisht.intent_service.dto.request.CreateIntentRequest;
import dev.lokeshbisht.intent_service.dto.response.CreateIntentResponse;
import dev.lokeshbisht.intent_service.service.PaymentIntentService;
import dev.lokeshbisht.intent_service.utils.HeaderValidator;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@CrossOrigin
@RestController
@RequestMapping("/v1.0")
public class PaymentIntentController {

    private final HeaderValidator headerValidator;

    private final PaymentIntentService paymentIntentService;

    private static final Logger logger = LoggerFactory.getLogger(PaymentIntentController.class);

    public PaymentIntentController(HeaderValidator headerValidator, PaymentIntentService paymentIntentService) {
        this.headerValidator = headerValidator;
        this.paymentIntentService = paymentIntentService;
    }

    @PostMapping("/intent")
    public Mono<ResponseEntity<CreateIntentResponse>> createIntent(
        @RequestHeader(name = "Content-Type") String contentType,
        @RequestHeader(required = false, name = "x-psu-id") String psuId,
        @Valid @RequestBody CreateIntentRequest createIntentRequest
    ) {
        logger.info("Received request to create intent for idempotencyKey={} psuId={}", createIntentRequest.getIdempotencyKey(), psuId);
        headerValidator.validateCreateIntentHeaders(psuId);
        createIntentRequest.setPsuId(UUID.fromString(psuId));
        return paymentIntentService.createPaymentIntent(createIntentRequest)
            .map(response -> {
                if (response.getIsConflicted()) {
                    return ResponseEntity.status(HttpStatus.OK).body(response);
                }
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            });
    }
}
