package com.stableflow.system.exception;

public enum ErrorCode {
    UNAUTHORIZED(40101, "Unauthorized"),
    FORBIDDEN(40301, "Forbidden"),
    MERCHANT_NOT_FOUND(40401, "Merchant not found"),
    PAYMENT_CONFIG_NOT_FOUND(40402, "Merchant payment config not found"),
    INVOICE_NOT_FOUND(40403, "Invoice not found"),
    PAYMENT_PROOF_NOT_FOUND(40404, "Payment proof not found"),
    INVALID_CREDENTIALS(40001, "Invalid credentials"),
    INVALID_REQUEST(40002, "Invalid request"),
    EMAIL_ALREADY_REGISTERED(40003, "Email already registered"),
    BLOCKCHAIN_RPC_TIMEOUT(50010, "Blockchain RPC timeout"),
    BLOCKCHAIN_RPC_HTTP_ERROR(50011, "Blockchain RPC HTTP error"),
    BLOCKCHAIN_RPC_ERROR(50012, "Blockchain RPC error"),
    BLOCKCHAIN_RPC_EMPTY_RESPONSE(50013, "Blockchain RPC returned empty response"),
    CONFIGURATION_ERROR(50001, "Configuration error"),
    SYSTEM_ERROR(50000, "System error");

    private final int code;
    private final String defaultMessage;

    ErrorCode(int code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public int getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
