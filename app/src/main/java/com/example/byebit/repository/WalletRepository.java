package com.example.byebit.repository;

import android.content.Context;

import com.example.byebit.config.AppDatabase;
import com.example.byebit.dao.WalletHandleDao;
import com.example.byebit.domain.WalletHandle;

import org.web3j.crypto.Bip39Wallet;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Wallet;
import org.web3j.crypto.WalletFile;
import org.web3j.crypto.WalletUtils;
import org.web3j.crypto.exception.CipherException;

import java.io.File;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.List;

public class WalletRepository {

    private final WalletHandleDao walletHandleDao;

    private final File walletsDir;

    public WalletRepository(Context context) {
        this.walletHandleDao = AppDatabase.getDatabase(context).getWalletHandleDao();
        this.walletsDir = context.getFilesDir();
    }

    public List<WalletHandle> getSavedWallets() {
        return null;
    }

    public WalletHandle createNewWallet(String name, String password) throws InvalidAlgorithmParameterException, CipherException, NoSuchAlgorithmException, IOException, NoSuchProviderException {
        String filename = WalletUtils.generateNewWalletFile(password, walletsDir);
        Credentials credentials = WalletUtils.loadCredentials(password, new File(walletsDir, filename));

    }
}
