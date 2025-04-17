package com.example.byebit.web3j;

import org.junit.jupiter.api.Assertions;
import org.web3j.protocol.Web3j;
import org.junit.jupiter.api.Test;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.EthBlockNumber;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.http.HttpService;

import java.math.BigInteger;

/**
 * <a href="https://docs.metamask.io/services/reference/ethereum/json-rpc-methods/">dokumentacja json-rpc</a>
 */
public class SepoliaConnectionWorksTest {

    @Test
    public void connectionWorks() {
        Web3j web3j = Web3j.build(new HttpService("https://sepolia.drpc.org"));

        try {
            Request<?, EthBlockNumber> ethBlockNumberRequest = web3j.ethBlockNumber();

            BigInteger blockNumber = ethBlockNumberRequest.send().getBlockNumber();

            System.out.println(blockNumber);

            Assertions.assertTrue(blockNumber.compareTo(BigInteger.ONE) > 0);
        } catch (Exception e) {
            Assertions.fail(e.getMessage());
        }
    }

    @Test
    public void checkBalanceTest() {
        Web3j web3j = Web3j.build(new HttpService("https://sepolia.drpc.org"));

        try {
            Request<?, EthGetBalance> ethGetBalanceRequest = web3j.ethGetBalance(
                    "0x9d1E794bd663C2050096b0A0C7f8c9a4de9759C7",
                    DefaultBlockParameter.valueOf("latest")
            );

            BigInteger balance = ethGetBalanceRequest.send().getBalance();

            System.out.println(balance);

            Assertions.assertTrue(balance.compareTo(BigInteger.ZERO) > 0);
        } catch (Exception e) {
            Assertions.fail(e.getMessage());
        }
    }
}
