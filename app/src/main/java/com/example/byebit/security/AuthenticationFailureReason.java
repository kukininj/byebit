package com.example.byebit.security;

public enum AuthenticationFailureReason {
    PASSWORD_NOT_STORED,
    AUTHENTICATION_FAILED,

    /**
     * The authentication failed for an unknown reason.
     */
    UNKNOWN,

    /**
     * Internal API error
     */
    INTERNAL_ERROR,

    /**
     * API not initialized
     */
    NOT_INITIALIZED_ERROR,

    /**
     * Biometric can't be starte due to missing permissions
     */
    MISSING_PERMISSIONS_ERROR,

    CRYPTO_ERROR,

    // ADD THESE:
    DECRYPTION_FAILED,
    ENCRYPTION_FAILED,
    CRYPTO_SETUP_FAILED,
    KEYSTORE_ERROR
}
