package com.example.byebit.remote;

import com.fasterxml.jackson.databind.DeserializationFeature; // For ObjectMapper configuration
import com.fasterxml.jackson.databind.ObjectMapper; // Jackson's core object mapper

import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory; // Import Jackson converter

public class RetrofitClient {
    private static Retrofit retrofit = null;
    private static ObjectMapper objectMapper = null; // Declare ObjectMapper

    public static EtherscanApiService getApiService() {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
            // Configure ObjectMapper:
            // 1. Ignore unknown properties in JSON, useful for API responses that might change.
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            // You can add more configurations here, e.g., for custom serializers/deserializers
            // for BigDecimal if it were directly in EtherscanTransaction.
        }

        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(EtherscanApiService.BASE_URL)
                    .addConverterFactory(JacksonConverterFactory.create(objectMapper)) // Use Jackson converter with our configured ObjectMapper
                    .build();
        }
        return retrofit.create(EtherscanApiService.class);
    }
}
