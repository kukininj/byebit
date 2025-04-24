package com.example.byebit.web3j;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.web3j.crypto.Bip39Wallet;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Wallet;
import org.web3j.crypto.WalletUtils;
import org.web3j.crypto.exception.CipherException;

import java.io.File;
import java.io.IOException;
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

        Assertions.assertNotNull(credentials.getAddress());
        Assertions.assertNotNull(credentials.getEcKeyPair());
        Assertions.assertNotNull(credentials.getEcKeyPair().getPrivateKey());
        Assertions.assertNotNull(credentials.getEcKeyPair().getPublicKey());
    }
}
