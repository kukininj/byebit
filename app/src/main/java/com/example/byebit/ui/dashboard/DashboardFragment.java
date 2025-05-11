package com.example.byebit.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.lifecycle.Lifecycle;
import org.web3j.crypto.Credentials;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.byebit.R;
import com.example.byebit.adapter.WalletAdapter;
import androidx.appcompat.app.AlertDialog;
import com.example.byebit.ui.dialog.WalletUnlockDialogFragment;

import com.example.byebit.ui.dialog.WalletDetailDialogResult;
import com.example.byebit.ui.dialog.WalletDetailsDialogFragment;


import com.example.byebit.databinding.FragmentDashboardBinding;
import com.example.byebit.domain.WalletHandle;
import com.example.byebit.security.BiometricService;
import com.example.byebit.util.WalletExporter;

import java.util.List;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;


public class DashboardFragment extends Fragment implements WalletAdapter.OnDetailsClickListener, MenuProvider {

    private FragmentDashboardBinding binding;
    private DashboardViewModel dashboardViewModel;
    private WalletAdapter walletAdapter;
    private BiometricService biometricService;

    private static final String TAG = "DashboardFragment";
    private static final String DEFAULT_EXPORT_FILE_NAME = "byebit_wallets_export.zip";
    private ActivityResultLauncher<Intent> exportWalletsLauncher;
    private List<WalletHandle> walletsToExportHolder;
    private WalletExporter walletExporter;

    private WalletHandle currentWalletForDetails;
    private final CompositeDisposable disposables = new CompositeDisposable();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        biometricService = new BiometricService(this);

        walletExporter = new WalletExporter(requireContext().getApplicationContext());

        exportWalletsLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null && walletsToExportHolder != null && !walletsToExportHolder.isEmpty()) {
                            // MODIFIED: Call WalletExporter
                            Toast.makeText(getContext(), "Exporting wallets...", Toast.LENGTH_SHORT).show();
                            walletExporter.exportWalletsToZip(uri, walletsToExportHolder, new WalletExporter.ExportListener() {
                                @Override
                                public void onExportSuccess() {
                                    Toast.makeText(getContext(), "Wallets exported successfully!", Toast.LENGTH_LONG).show();
                                }

                                @Override
                                public void onExportFailure(String errorMessage) {
                                    Toast.makeText(getContext(), "Export failed: " + errorMessage, Toast.LENGTH_LONG).show();
                                    Log.w(TAG, "Export failed via WalletExporter: " + errorMessage);
                                }
                            });
                        } else {
                            Toast.makeText(getContext(), "Export cancelled or no wallets selected.", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Export URI was null or walletsToExportHolder was empty/null.");
                        }
                    } else {
                        Toast.makeText(getContext(), "Export cancelled.", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Export activity result was not OK or data was null.");
                    }
                    // Clear the holder
                    walletsToExportHolder = null;
                }
        );
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        dashboardViewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        setupRecyclerView();


        binding.fabCreateWallet.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(v);
            navController.navigate(R.id.action_dashboard_to_createWalletFragment);
        });

        binding.fabExportWallets.setOnClickListener(v -> {
            if (dashboardViewModel.getSavedWallets().getValue() != null) {
                List<WalletHandle> currentWallets = dashboardViewModel.getSavedWallets().getValue();
                if (currentWallets != null && !currentWallets.isEmpty()) {
                    this.walletsToExportHolder = currentWallets;
                    launchSaveZipFilePicker();
                } else {
                    Toast.makeText(getContext(), "No wallets to export.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getContext(), "Wallet data not available yet. Please try again.", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "Attempted export but ViewModel or LiveData value was null.");
            }
        });

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        dashboardViewModel.getSavedWallets().observe(getViewLifecycleOwner(), wallets -> {
            if (wallets != null) {
                walletAdapter.setWallets(wallets);
            }
        });

        dashboardViewModel.getCredentialsResult().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;

            if (result.isLoading()) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Loading wallet details...", Toast.LENGTH_SHORT).show();
                }
            } else if (result.isError()) {
                Log.w(TAG, "Failed to load credentials for wallet " + (this.currentWalletForDetails != null ? this.currentWalletForDetails.getName() : "N/A") + ": " + result.getError());
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error: " + result.getError(), Toast.LENGTH_LONG).show();
                }
                dashboardViewModel.clearCredentialsResult();
                this.currentWalletForDetails = null;
            } else if (result.isSuccess() && result.getCredentials() != null) {
                if (this.currentWalletForDetails != null) {
                    // MODIFIED: Show the new WalletDetailsDialogFragment
                    Credentials credentials = result.getCredentials();
                    String privateKey = credentials.getEcKeyPair().getPrivateKey().toString(16);

                    WalletDetailsDialogFragment dialogFragment = WalletDetailsDialogFragment.newInstance(
                            this.currentWalletForDetails.getName(),
                            this.currentWalletForDetails.getAddress(),
                            privateKey
                    );
                    dialogFragment.show(getChildFragmentManager(), WalletDetailsDialogFragment.TAG);

                    // Subscribe to dialog events
                    disposables.add(dialogFragment.getDialogEvents()
                        .observeOn(Schedulers.from(requireContext().getMainExecutor()))
                        .subscribe(
                            walletDetailDialogResult -> {
                                final WalletHandle walletInContext = this.currentWalletForDetails; // Capture for use in lambdas

                                if (walletDetailDialogResult.action == WalletDetailDialogResult.Action.REQUEST_DELETE_CONFIRMATION) {
                                    deleteWalletWithConfirmation(walletInContext);
                                } else if (walletDetailDialogResult.action == WalletDetailDialogResult.Action.CLOSE ||
                                           walletDetailDialogResult.action == WalletDetailDialogResult.Action.DISMISS) {
                                    if (walletInContext != null) {
                                        Log.d(TAG, "WalletDetailsDialogFragment closed/dismissed via Rx, clearing currentWalletForDetails for: " + walletInContext.getName());
                                    }
                                    this.currentWalletForDetails = null; // Clear wallet context
                                }
                            },
                            throwable -> {
                                Log.e(TAG, "Error in WalletDetailsDialogFragment Rx stream", throwable);
                                if (getContext() != null) {
                                    Toast.makeText(getContext(), "Error processing wallet details action.", Toast.LENGTH_SHORT).show();
                                }
                                this.currentWalletForDetails = null; // Clear wallet context on error
                            },
                            () -> {
                                Log.d(TAG, "WalletDetailsDialogFragment Rx stream completed.");
                            }
                        ));
                } else {
                    Log.e(TAG, "currentWalletForDetails is null when credentials result received successfully.");
                }
                dashboardViewModel.clearCredentialsResult(); // Keep this to reset the LiveData state
            }
        });
    }

    private void deleteWalletWithConfirmation(WalletHandle walletInContext) {
        if (walletInContext != null) {
            new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.confirm_delete_wallet_title))
                .setMessage(getString(R.string.confirm_delete_wallet_message, walletInContext.getName()))
                .setPositiveButton(getString(R.string.delete_button_confirm), (confirmDialog, confirmWhich) -> {
                    dashboardViewModel.deleteWallet(walletInContext);
                    Toast.makeText(getContext(), getString(R.string.wallet_deleted_toast_param, walletInContext.getName()), Toast.LENGTH_SHORT).show();
                    this.currentWalletForDetails = null;
                })
                .setNegativeButton(R.string.cancel_button, (dialogInterface, i) -> {
                    this.currentWalletForDetails = null;
                })
                .setOnDismissListener(dialogInterface -> {
                    if (this.currentWalletForDetails == walletInContext) {
                        this.currentWalletForDetails = null;
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
        } else {
            Log.w(TAG, "Request delete confirmation received, but currentWalletForDetails was null.");
            this.currentWalletForDetails = null;
        }
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = binding.recyclerViewWallets;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true); // Optimization if item size doesn't change

        walletAdapter = new WalletAdapter();
        recyclerView.setAdapter(walletAdapter);

        walletAdapter.setOnItemClickListener(wallet -> {
            String walletAddress = wallet.getAddress();

            ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);

            ClipData clip = ClipData.newPlainText("Wallet Address", walletAddress);

            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getContext(), "Address copied to clipboard", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Failed to copy address", Toast.LENGTH_SHORT).show();
            }

            dashboardViewModel.fetchBalanceForAddress(walletAddress);
        });

        walletAdapter.setOnDetailsClickListener(this);
    }

    @Override
    public void onDetailsClick(WalletHandle wallet) {
        if (getContext() == null || wallet == null) {
            Log.w(TAG, "onDetailsClick called with null context or wallet.");
            return;
        }
        this.currentWalletForDetails = wallet; // Set the context for the current operation

        WalletUnlockDialogFragment walletUnlockDialog = WalletUnlockDialogFragment.newInstance(
                this.currentWalletForDetails.getName(),
                this.currentWalletForDetails.getEncryptedPassword(),
                this.currentWalletForDetails.getIv()
        );

        disposables.add(walletUnlockDialog.getDialogEvents()
                .observeOn(Schedulers.from(requireContext().getMainExecutor())) // Ensure UI updates on main thread
                .subscribe(
                        passwordResult -> {
                            if (passwordResult.isSuccess() && passwordResult.password != null) {
                                dashboardViewModel.loadCredentialsForWallet(this.currentWalletForDetails, passwordResult.password);
                            } else { // Cancelled or other non-success from WalletUnlockDialogFragment
                                Log.d(TAG, "Wallet unlock cancelled or failed for: " + this.currentWalletForDetails.getName());
                                Toast.makeText(getContext(), getString(R.string.wallet_unlock_cancelled_or_failed), Toast.LENGTH_SHORT).show();
                                this.currentWalletForDetails = null; // Clear wallet context as unlock failed/cancelled
                            }
                        },
                        throwable -> {
                            String walletNameForError = (this.currentWalletForDetails != null && this.currentWalletForDetails.getId().equals(wallet.getId()))
                                                        ? this.currentWalletForDetails.getName()
                                                        : wallet.getName(); // Fallback to original wallet name for log if context changed
                            Log.e(TAG, "Error during wallet unlock process for: " + walletNameForError, throwable);
                            if (getContext() != null) { // Check context before Toast
                                Toast.makeText(getContext(), getString(R.string.wallet_unlock_error), Toast.LENGTH_LONG).show();
                            }
                            // Clear currentWalletForDetails if it was the one associated with this failed operation
                            if (this.currentWalletForDetails != null && this.currentWalletForDetails.getId().equals(wallet.getId())) {
                                this.currentWalletForDetails = null;
                            }
                        }
                ));

        walletUnlockDialog.show(getChildFragmentManager(), "WalletUnlockDialogTag");
    }

    private void launchSaveZipFilePicker() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        intent.putExtra(Intent.EXTRA_TITLE, DEFAULT_EXPORT_FILE_NAME);

        // Check if there's an activity to handle this intent
        if (getContext() != null && intent.resolveActivity(requireContext().getPackageManager()) != null) {
            exportWalletsLauncher.launch(intent);
        } else {
            Toast.makeText(getContext(), "No app found to handle file creation.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "No activity found to handle ACTION_CREATE_DOCUMENT for application/zip");
            this.walletsToExportHolder = null; // Clear holder if we can't launch
        }
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.dashboard_menu, menu);
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.action_refresh_balances) {
            if (dashboardViewModel != null) {
                dashboardViewModel.refreshAllWalletBalances();
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Refreshing balances...", Toast.LENGTH_SHORT).show();
                }
            }
            return true; // Indicate that the item selection event has been handled
        }
        return false; // Return false to allow other components to handle the event
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // MODIFIED: Shutdown WalletExporter's executor
        if (walletExporter != null) {
            walletExporter.shutdown();
        }
    }

    public void onDestroyView() {
        super.onDestroyView();
        // MenuProvider is automatically removed when using getViewLifecycleOwner()
        disposables.clear(); // Clear all subscriptions
        // with addMenuProvider, so explicit removal is not strictly necessary.
        binding = null;
    }

}
