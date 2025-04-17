package com.example.byebit.web3j;

import com.example.byebit.generated.HelloWorld;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.DefaultGasProvider;


@Disabled
public class DeployContractTest {

    @Test
    public void deploy() {
        Web3j web3j = Web3j.build(new HttpService("https://sepolia.drpc.org"));
        TransactionManager transactionManager = new RawTransactionManager(
                web3j,
                Credentials.create(TestConfig.TEST_WALLET_PRIVATE_KEY)
        );
        ContractGasProvider contractGasProvider = new DefaultGasProvider();
        HelloWorld helloWorld = null;
        try {
            helloWorld = HelloWorld.deploy(web3j, transactionManager, contractGasProvider).send();

            Assertions.assertTrue(helloWorld.isValid());
        } catch (Exception e) {
            Assertions.fail(e.getMessage());
        }


        System.out.println(helloWorld.getContractAddress());
        System.out.println(helloWorld.getContractBinary());
    }
}
