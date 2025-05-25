package com.example.byebit.remote;

import com.fasterxml.jackson.annotation.JsonCreator; // Import Jackson annotation for constructors
import com.fasterxml.jackson.annotation.JsonProperty; // Import Jackson annotation

// Represents a single transaction from Etherscan
public class EtherscanTransaction {
    // Fields are final, so we need a @JsonCreator constructor

    private final String blockNumber;
    private final String timeStamp;
    private final String hash;
    private final String nonce;
    private final String blockHash;
    private final String from;
    private final String contractAddress;
    private final String to;
    private final String value;
    private final String gas;
    private final String gasUsed;
    private final String gasPrice;
    private final String isError;
    private final String txReceiptStatus;
    private final String input;
    private final String confirmations;

    // Use @JsonCreator to tell Jackson to use this constructor for deserialization.
    // Each parameter must be annotated with @JsonProperty to map to the JSON key.
    @JsonCreator
    public EtherscanTransaction(
            @JsonProperty("blockNumber") String blockNumber,
            @JsonProperty("timeStamp") String timeStamp,
            @JsonProperty("hash") String hash,
            @JsonProperty("nonce") String nonce,
            @JsonProperty("blockHash") String blockHash,
            @JsonProperty("from") String from,
            @JsonProperty("contractAddress") String contractAddress,
            @JsonProperty("to") String to,
            @JsonProperty("value") String value,
            @JsonProperty("gas") String gas,
            @JsonProperty("gasUsed") String gasUsed,
            @JsonProperty("gasPrice") String gasPrice,
            @JsonProperty("isError") String isError,
            @JsonProperty("txreceipt_status") String txReceiptStatus,
            @JsonProperty("input") String input,
            @JsonProperty("confirmations") String confirmations) {
        this.blockNumber = blockNumber;
        this.timeStamp = timeStamp;
        this.hash = hash;
        this.nonce = nonce;
        this.blockHash = blockHash;
        this.from = from;
        this.contractAddress = contractAddress;
        this.to = to;
        this.value = value;
        this.gas = gas;
        this.gasUsed = gasUsed;
        this.gasPrice = gasPrice;
        this.isError = isError;
        this.txReceiptStatus = txReceiptStatus;
        this.input = input;
        this.confirmations = confirmations;
    }

    // Getters (unchanged)
    public String getBlockNumber() { return blockNumber; }
    public String getTimeStamp() { return timeStamp; }
    public String getHash() { return hash; }
    public String getNonce() { return nonce; }
    public String getBlockHash() { return blockHash; }
    public String getFrom() { return from; }
    public String getContractAddress() { return contractAddress; }
    public String getTo() { return to; }
    public String getValue() { return value; }
    public String getGas() { return gas; }
    public String getGasUsed() { return gasUsed; }
    public String getGasPrice() { return gasPrice; }
    public String getIsError() { return isError; }
    public String getTxReceiptStatus() { return txReceiptStatus; }
    public String getInput() { return input; }
    public String getConfirmations() { return confirmations; }
}
