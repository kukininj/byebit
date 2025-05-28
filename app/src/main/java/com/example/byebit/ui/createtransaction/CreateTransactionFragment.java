package com.example.byebit.ui.createtransaction;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.MutableObjectList;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.example.byebit.R;
import com.example.byebit.adapter.SpinnerTransactionAdapter;
import com.example.byebit.databinding.FragmentCreateTransactionBinding;
import com.example.byebit.domain.TransactionHandle;
import com.example.byebit.domain.WalletHandle;
import com.example.byebit.repository.WalletRepository;
import com.example.byebit.security.AuthenticationFailureReason;
import com.example.byebit.security.AuthenticationListener;
import com.example.byebit.security.BiometricService;
import com.example.byebit.ui.dashboard.DashboardViewModel;
import com.example.byebit.ui.dialog.WalletUnlockDialogFragment;

import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.Transfer;
import org.web3j.utils.Convert;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class CreateTransactionFragment extends Fragment {
    public static final int CHAIN_ID = 11155111;
    private final String TAG = getClass().getSimpleName();

    private FragmentCreateTransactionBinding binding;
    private SpinnerTransactionAdapter walletAdapter;
    private List<WalletHandle> wallets = new ArrayList<>();
    private Context context;
    private Activity activity;
    private CompositeDisposable disposables = new CompositeDisposable();
    private WalletHandle currentWalletForTransaction;
    private BiometricService biometricService;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private MutableLiveData<DashboardViewModel.CredentialsResult> credentialsResult = new MutableLiveData<>();
    private WalletRepository walletRepository;
    private CreateTransactionViewModel createTransactionViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = requireContext();
        activity = requireActivity();
        biometricService = new BiometricService(this);
        walletRepository = new WalletRepository(getContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        createTransactionViewModel =
                new ViewModelProvider(this).get(CreateTransactionViewModel.class);

        binding = FragmentCreateTransactionBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        walletAdapter = new SpinnerTransactionAdapter(context, wallets);
        binding.createTransactionWalletList.setAdapter(walletAdapter);

        binding.createTransactionDestination.setFilters(new InputFilter[]{
                (source, start, end, dest, dstart, dend) -> {
                    for (int i = start; i < end; i++) {
                        char c = source.charAt(i);
//                        if(!String.valueOf(c).matches("[0-9a-fA-F]")) {
                        if ((c < 'a' || c > 'f') && (c < 'A' || c > 'F') && (c < '0' || c > '9') && c != 'x' && c != 'X') {
                            return "";
                        }
                    }
                    return source.toString().toLowerCase();
                }
        });

        binding.buttonCreateTransaction.setOnClickListener(view -> {
            Web3j web3j = Web3j.build(new HttpService(context.getString(R.string.sepolia_rpc_url)));
            currentWalletForTransaction = (WalletHandle) binding.createTransactionWalletList.getSelectedItem();

            WalletUnlockDialogFragment dialog = WalletUnlockDialogFragment.newInstance(currentWalletForTransaction);

            disposables.add(dialog.getDialogEvents()
                    .observeOn(Schedulers.from(requireContext().getMainExecutor())) // Ensure UI updates on main thread
                    .subscribe(
                            passwordResult -> {
                                if (passwordResult.isSuccess() && passwordResult.password != null) {
                                    credentialsResult.setValue(DashboardViewModel.CredentialsResult.loading());
                                    executor.execute(() -> {
                                        try {
                                            Credentials credentials = walletRepository.getCredentials(currentWalletForTransaction, passwordResult.password);
                                            credentialsResult.postValue(DashboardViewModel.CredentialsResult.success(credentials));
                                        } catch (Exception e) {
                                            Log.e(TAG, "Failed to load credentials for wallet " + currentWalletForTransaction.getAddress());
                                            String errorMessage = e.getMessage();
                                            if (e instanceof org.web3j.crypto.exception.CipherException) {
                                                errorMessage = "Invalid password or corrupted wallet file.";
                                            } else if (e instanceof java.io.FileNotFoundException) {
                                                errorMessage = "Wallet file not found.";
                                            }
                                            credentialsResult.postValue(DashboardViewModel.CredentialsResult.error(errorMessage));
                                        }
                                    });
                                } else { // Cancelled or other non-success from WalletUnlockDialogFragment
                                    Log.d(TAG, "Wallet unlock cancelled or failed for: " + currentWalletForTransaction.getName());
                                    Toast.makeText(getContext(), getString(R.string.wallet_unlock_cancelled_or_failed), Toast.LENGTH_SHORT).show();
                                    currentWalletForTransaction = null; // Clear wallet context as unlock failed/cancelled
                                }
                            },
                            throwable -> {
                                String walletNameForError = currentWalletForTransaction.getName(); // Fallback to original wallet name for log if context changed
                                Log.e(TAG, "Error during wallet unlock process for: " + walletNameForError, throwable);
                                if (getContext() != null) { // Check context before Toast
                                    Toast.makeText(getContext(), getString(R.string.wallet_unlock_error), Toast.LENGTH_LONG).show();
                                }
                                // Clear currentWalletForDetails if it was the one associated with this failed operation
                                currentWalletForTransaction = null;
                            }
                    ));

            dialog.show(getChildFragmentManager(), "WalletUnlockDialogTag");

            credentialsResult.observe(getViewLifecycleOwner(), result -> {
                if (result == null) return;

                if (result.isSuccess() && result.getCredentials() != null) {
                    Toast.makeText(context, R.string.create_transaction_sending, Toast.LENGTH_SHORT).show();
                    TransactionManager txManager = new RawTransactionManager(web3j, result.credentials, CHAIN_ID);
                    Transfer transfer = new Transfer(web3j, txManager);
                    transfer.sendFunds(binding.createTransactionDestination.getText().toString(), new BigDecimal(binding.createTransactionAmount.getText().toString()), Convert.Unit.ETHER)
                            .sendAsync()
                            .thenAccept(receipt -> {
                                Log.d(TAG, receipt.toString());
                                activity.runOnUiThread(() -> Toast.makeText(context, context.getString(R.string.create_transaction_send_success), Toast.LENGTH_SHORT).show());
                            })
                            .exceptionally(throwable -> {
                                Log.w(TAG, throwable.getMessage(), throwable);
                                activity.runOnUiThread(() -> Toast.makeText(context, context.getString(R.string.create_transaction_send_failure), Toast.LENGTH_SHORT).show());
                                return null;
                            });
                    binding.createTransactionDestination.getText().clear();
                    binding.createTransactionAmount.getText().clear();
                } else if (result.isError()) {
                    Toast.makeText(getContext(), "Error: " + result.getError(), Toast.LENGTH_LONG).show();
                }
            });

        });

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        createTransactionViewModel.getSavedWallets().observe(getViewLifecycleOwner(), wallets -> {
            if (wallets == null) return;
            walletAdapter.setWallets(wallets);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        disposables.clear();
    }
}