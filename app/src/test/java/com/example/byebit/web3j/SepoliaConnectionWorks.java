package com.example.byebit.web3j;

import org.junit.Assert;
import org.web3j.protocol.Web3j;
import org.junit.Test;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.EthBlockNumber;
import org.web3j.protocol.http.HttpService;

import java.math.BigInteger;

public class SepoliaConnectionWorks {
    @Test
    public void connectionWorks() {
        Web3j web3j = Web3j.build(new HttpService("https://sepolia.drpc.org"));

        Request<?, EthBlockNumber> ethBlockNumberRequest = web3j.ethBlockNumber();

        try {
            BigInteger blockNumber = ethBlockNumberRequest.send().getBlockNumber();

            System.out.println(blockNumber);

            Assert.assertTrue(blockNumber.compareTo(BigInteger.ONE) > 0);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
}
