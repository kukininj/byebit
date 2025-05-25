package com.example.byebit.remote;

import com.fasterxml.jackson.annotation.JsonProperty; // Import Jackson annotation

import java.util.List;

// Main response wrapper
public class EtherscanApiResponse {
    @JsonProperty("status") // Use Jackson's @JsonProperty
    private String status;
    @JsonProperty("message") // Use Jackson's @JsonProperty
    private String message;
    @JsonProperty("result") // Use Jackson's @JsonProperty
    private List<EtherscanTransaction> result;

    // Default no-arg constructor is often required by Jackson for deserialization,
    // unless a @JsonCreator constructor is fully defined for all fields.
    public EtherscanApiResponse() {
    }

    // Constructor with all fields for convenience (optional, but good practice if you create these objects yourself)
    public EtherscanApiResponse(String status, String message, List<EtherscanTransaction> result) {
        this.status = status;
        this.message = message;
        this.result = result;
    }

    // Getters
    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public List<EtherscanTransaction> getResult() { return result; }

    // Setters (optional, but if fields are not final, Jackson can use them for deserialization)
    public void setStatus(String status) { this.status = status; }
    public void setMessage(String message) { this.message = message; }
    public void setResult(List<EtherscanTransaction> result) { this.result = result; }
}
