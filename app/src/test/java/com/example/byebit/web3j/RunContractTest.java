package com.example.byebit.web3j;

import com.example.byebit.generated.HelloWorld;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.DefaultGasProvider;

import java.io.IOException;


@Disabled
public class RunContractTest {

    private static HelloWorld helloWorld;

    private static Web3j web3j;

    private static TransactionManager transactionManager;

    private static final String CONTRACT_ADDRESS = "0x54693b6a5f569e0c4a258ebf10d7144a8f23ec6a";

    @BeforeAll
    public static void load() throws IOException {
        web3j = Web3j.build(new HttpService(TestConfig.SEPOLIA_RPC_URL));
        transactionManager = new RawTransactionManager(
                web3j,
                Credentials.create(TestConfig.TEST_WALLET_PRIVATE_KEY)
        );
        ContractGasProvider contractGasProvider = new DefaultGasProvider();

        helloWorld = HelloWorld.load(CONTRACT_ADDRESS, web3j, transactionManager, contractGasProvider);

        Assertions.assertTrue(helloWorld.isValid());

        System.out.println(helloWorld.getContractAddress());
        System.out.println(helloWorld.getContractBinary());
    }

    @Test
    void contractExists() throws IOException {
        Assertions.assertTrue(helloWorld.isValid());
        System.out.println(helloWorld.getContractAddress());
        System.out.println(helloWorld.getContractBinary());
    }

    @Test
    public void runContractTest() {
        try {
            String result = helloWorld.sayHello().send();

            System.out.println("Contract return value:");
            System.out.println(result);

            Assertions.assertEquals("Hello World!", result);
        } catch (Exception e) {
            Assertions.fail(e.getMessage());
        }
    }
}
