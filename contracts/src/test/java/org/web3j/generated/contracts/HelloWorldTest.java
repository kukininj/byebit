package org.web3j.generated.contracts;

import java.lang.Exception;
import java.lang.String;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.web3j.EVMTest;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

@EVMTest
class HelloWorldTest {
    private static HelloWorld helloWorld;

    @BeforeAll
    static void deploy(Web3j web3j, TransactionManager transactionManager,
                       ContractGasProvider contractGasProvider) throws Exception {
        helloWorld = HelloWorld.deploy(web3j, transactionManager, contractGasProvider).send();
    }

    @Test
    public void greeting() throws Exception {
    String stringVar = helloWorld.sayHello().send();
    Assertions.assertEquals("Hello World!", stringVar);
    }

}
