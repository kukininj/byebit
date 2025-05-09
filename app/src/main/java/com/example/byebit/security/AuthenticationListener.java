package com.example.byebit.security;

public interface AuthenticationListener {
    void onSuccess(byte[] result, byte[] iv);

    void onFailure(AuthenticationFailureReason reason);

    void onCancel();
}
