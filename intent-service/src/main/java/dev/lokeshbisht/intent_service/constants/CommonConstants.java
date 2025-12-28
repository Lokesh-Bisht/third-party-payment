package dev.lokeshbisht.intent_service.constants;

public class CommonConstants {

    private CommonConstants() {}

    public static final String PSU_ID_HEADER = "x-psu-id";

    public static final String CREATE_INTENT_REASON_CODE = "IN01";

    public static final String CREATE_INTENT_CODE = "INT_01";

    public static final String MISSING_HEADER_VALUE = "Header value is missing or blank";

    public static final String PSU_ID_INVALID_HEADER_VALUE = "x-psu-id must be a valid UUID";

    public static final String HEADER_VALIDATION_FAILED_ERROR_MSG = "One or more headers are missing or have invalid values.";

    public static final String INVALID_INTENT_ID_ERROR_MSG = "IntentId must be a valid UUID.";

    public static final String  INTERNAL_SERVER_ERROR_MSG = "An error occurred while creating the payment. Please, try again!";

    public static final String INTENT_NOT_FOUND_MSG = "Payment intent not found.";

    public static final String INTENT_ALREADY_EXISTS_MEG = "A payment intent with the provided intent ID already exists.";

    public static final String INTENT_STATUS_STALE_WRITE_MSG = "This payment was already updated. Please refresh to see the latest status.";

    public static final String INTENT_STATUS_UPDATE_CONFLICT_MSG = "Payment cannot be updated in current state.";

    public static final String INTENT_LOCK_BUSY_ERR_MSG = "Payment is being processed. Please try again.";

    public static final String INTENT_ACCESS_FORBIDDEN_MSG = "You are not authorized to access this payment intent.";

    public static final String INTENT_INVALID_STATUS_TRANSITION = "Invalid status transition";
}
