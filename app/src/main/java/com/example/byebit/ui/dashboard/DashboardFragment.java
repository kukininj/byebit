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
// Menu, MenuInflater, MenuItem are kept as they are used by MenuProvider methods
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.lifecycle.Lifecycle;
import android.widget.EditText;
import android.text.InputType;
import org.web3j.crypto.Credentials;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable; // Import Nullable
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager; // Import LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView; // Import RecyclerView
import androidx.navigation.NavController;
import androidx.navigation.Navigation; // Import Navigation helper

import com.example.byebit.R;
import com.example.byebit.adapter.WalletAdapter;
import androidx.appcompat.app.AlertDialog;




import com.example.byebit.databinding.FragmentDashboardBinding;
import com.example.byebit.domain.WalletHandle;
import com.example.byebit.security.AuthenticationFailureReason;
import com.example.byebit.security.AuthenticationListener;
import com.example.byebit.security.BiometricService;
import com.example.byebit.util.WalletExporter;

import java.nio.charset.StandardCharsets;
import java.util.List;




public class DashboardFragment extends Fragment implements WalletAdapter.OnItemLongClickListener, WalletAdapter.OnDetailsClickListener, MenuProvider {

    private FragmentDashboardBinding binding;
    private DashboardViewModel dashboardViewModel;
    private WalletAdapter walletAdapter;
    private BiometricService biometricService;

    private static final String TAG = "DashboardFragment";
    private static final String DEFAULT_EXPORT_FILE_NAME = "byebit_wallets_export.zip";
    private ActivityResultLauncher<Intent> exportWalletsLauncher;
    private List<WalletHandle> walletsToExportHolder; // To hold wallets between SAF launch and result
    private WalletExporter walletExporter;

    private WalletHandle currentWalletForDetails;

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
            // Ensure ViewModel is available and LiveData has a value
            if (dashboardViewModel.getSavedWallets().getValue() != null) {
                List<WalletHandle> currentWallets = dashboardViewModel.getSavedWallets().getValue();
                if (currentWallets != null && !currentWallets.isEmpty()) {
                    this.walletsToExportHolder = currentWallets; // Store for the launcher callback
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
            // Update the adapter's data when the wallet list changes
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
                    showWalletDetailsDialog(this.currentWalletForDetails, result.getCredentials());
                } else {
                    Log.e(TAG, "currentWalletForDetails is null when credentials result received successfully.");
                }
                dashboardViewModel.clearCredentialsResult();
            }
        });
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

        walletAdapter.setOnItemLongClickListener(this);
        walletAdapter.setOnDetailsClickListener(this);
    }

    @Override
    public void onDetailsClick(WalletHandle wallet) {
        if (getContext() == null) return;
        this.currentWalletForDetails = wallet;

        biometricService.decrypt(wallet, new AuthenticationListener() {
            @Override
            public void onSuccess(byte[] result, byte[] iv) {
                String password = new String(result, StandardCharsets.UTF_8);
                dashboardViewModel.loadCredentialsForWallet(wallet, password);
            }

            @Override
            public void onFailure(AuthenticationFailureReason reason) {
                Log.w(TAG, "Biometric decryption failed for wallet " + (wallet != null ? wallet.getName() : "null") + ". Reason: " + reason.name());
                if (getContext() != null) {
                    Toast.makeText(getContext(), R.string.biometric_unlock_fail_fallback, Toast.LENGTH_SHORT).show();
                }
                showFallbackPasswordInput(wallet);
            }

            @Override
            public void onCancel() {
                if (getContext() != null) {
                    Toast.makeText(getContext(), R.string.wallet_unlock_cancelled, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showFallbackPasswordInput(WalletHandle wallet) {
        AlertDialog.Builder passwordDialogBuilder = new AlertDialog.Builder(requireContext());
        passwordDialogBuilder.setTitle(getString(R.string.enter_password_for_wallet, wallet.getName()));

        final EditText passwordInput = new EditText(requireContext());
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordInput.setHint(R.string.password_hint);
        // For better padding, wrap EditText in a FrameLayout or similar
        android.widget.FrameLayout container = new android.widget.FrameLayout(requireContext());
        android.widget.FrameLayout.LayoutParams params = new  android.widget.FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int margin = (int) getResources().getDimension(R.dimen.dialog_edittext_margin); // Define dialog_edittext_margin in dimens.xml
        params.leftMargin = margin;
        params.rightMargin = margin;
        passwordInput.setLayoutParams(params);
        container.addView(passwordInput);
        passwordDialogBuilder.setView(container);


        passwordDialogBuilder.setPositiveButton(R.string.ok_button, (dialog, which) -> {
            String password = passwordInput.getText().toString();
            if (password.isEmpty()) {
                Toast.makeText(getContext(), R.string.password_cannot_be_empty, Toast.LENGTH_SHORT).show();
                this.currentWalletForDetails = null;
                return;
            }
            dashboardViewModel.loadCredentialsForWallet(wallet, password);
        });
        passwordDialogBuilder.setNegativeButton(R.string.cancel_button, (dialog, which) -> {
            dialog.cancel();
            this.currentWalletForDetails = null;
        });
        passwordDialogBuilder.show();
    }

    private void showWalletDetailsDialog(WalletHandle wallet, Credentials credentials) {
        if (getContext() == null) return;

        String privateKey = credentials.getEcKeyPair().getPrivateKey().toString(16);

        AlertDialog.Builder detailsDialogBuilder = new AlertDialog.Builder(requireContext());
        detailsDialogBuilder.setTitle(getString(R.string.wallet_details_title, wallet.getName()));

        String message = getString(R.string.wallet_address_label) + "\n" + wallet.getAddress() +
                         "\n\n" + getString(R.string.private_key_label) + "\n" + privateKey;
        detailsDialogBuilder.setMessage(message);

        detailsDialogBuilder.setPositiveButton(R.string.delete_wallet_button_details, (dialog, which) -> {
            new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.confirm_delete_wallet_title))
                .setMessage(getString(R.string.confirm_delete_wallet_message, wallet.getName()))
                .setPositiveButton(getString(R.string.delete_button_confirm), (confirmDialog, confirmWhich) -> {
                    dashboardViewModel.deleteWallet(wallet);
                    Toast.makeText(getContext(), getString(R.string.wallet_deleted_toast_param, wallet.getName()), Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel_button, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
        });

        detailsDialogBuilder.setNeutralButton(R.string.copy_private_key_button, (dialog, which) -> {
            ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Private Key", privateKey);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getContext(), R.string.private_key_copied_toast, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), R.string.failed_to_copy_private_key_toast, Toast.LENGTH_SHORT).show();
            }
        });


        detailsDialogBuilder.setNegativeButton(R.string.close_button, (dialog, which) -> {
            dialog.dismiss();
        });

        detailsDialogBuilder.setOnDismissListener(dialogInterface -> {
            this.currentWalletForDetails = null;
        });

        detailsDialogBuilder.setCancelable(false);
        detailsDialogBuilder.show();
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
        // with addMenuProvider, so explicit removal is not strictly necessary.
        binding = null;
    }

}
