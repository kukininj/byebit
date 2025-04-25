package com.example.byebit.web3j;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.web3j.crypto.Bip39Wallet;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Wallet;
import org.web3j.crypto.WalletFile;
import org.web3j.crypto.WalletUtils;
import org.web3j.crypto.exception.CipherException;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

public class WalletGenerationTest {
    public static final String TEST_PASSWORD = "test_password";

    @Test
    public void testWalletCreation() throws CipherException, IOException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
        File wallets = new File("src/test/java/com/example/byebit/web3j/test_wallets");
        String fileName = WalletUtils.generateNewWalletFile(TEST_PASSWORD, wallets);
        Credentials credentials = WalletUtils.loadCredentials(TEST_PASSWORD, new File(wallets, fileName));

        System.out.println(credentials.getAddress());
        System.out.println(credentials.getEcKeyPair());
        System.out.println(credentials.getEcKeyPair().getPrivateKey());
        System.out.println(credentials.getEcKeyPair().getPublicKey());

        Assertions.assertNotNull(credentials.getAddress());
        Assertions.assertNotNull(credentials.getEcKeyPair());
        Assertions.assertNotNull(credentials.getEcKeyPair().getPrivateKey());
        Assertions.assertNotNull(credentials.getEcKeyPair().getPublicKey());
    }

    @Test
    public void testLoadWallet() throws CipherException, IOException {
        File walletFile = new File("src/test/java/com/example/byebit/web3j/test_wallets/61505d33018d68108c485ba5e545a7f47437207c.json");
        Credentials credentials = WalletUtils.loadCredentials(TEST_PASSWORD, walletFile);

        Assertions.assertEquals("0x61505d33018d68108c485ba5e545a7f47437207c", credentials.getAddress());
        Assertions.assertNotNull(credentials.getEcKeyPair());
        Assertions.assertEquals(
                new BigInteger("49618169033127414112230692144403486239410148758275077027067941749016906244084"),
                credentials.getEcKeyPair().getPrivateKey()
        );
        Assertions.assertEquals(
                new BigInteger("2695491235255978938385213635856061265261644979341846721530706923868479475439586570157808646429831487197926265813427000981087534059678135136975151608196491"),
                credentials.getEcKeyPair().getPublicKey()
        );
    }
}