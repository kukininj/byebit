package com.example.byebit.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.app.Activity; // ADD
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent; // ADD
import android.net.Uri; // ADD
import android.os.Handler; // ADD
import android.os.Looper; // ADD
import android.util.Log; // ADD
import android.view.View;
import android.view.Menu; // ADD THIS
import android.view.MenuInflater; // ADD THIS
import android.view.MenuItem; // ADD THIS
import android.view.ViewGroup;
import android.widget.EditText;
import android.text.InputType;
import org.web3j.crypto.Credentials;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher; // ADD
import androidx.activity.result.contract.ActivityResultContracts; // ADD
// Remove TextView import if no longer needed
// import android.widget.TextView;

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
// ADD: AlertDialog import
import androidx.appcompat.app.AlertDialog;
// import android.os.Bundle; // Already present
// import android.view.LayoutInflater; // Already present

// Remove TextView import if no longer needed
// import android.widget.TextView;

// import androidx.recyclerview.widget.LinearLayoutManager; // Already present
// import androidx.recyclerview.widget.RecyclerView; // Already present

import com.example.byebit.databinding.FragmentDashboardBinding;
import com.example.byebit.domain.WalletHandle; // ADD

import java.io.File; // ADD
import java.io.FileInputStream; // ADD
import java.io.IOException; // ADD
import java.io.OutputStream; // ADD
import java.util.List; // ADD
import java.util.concurrent.ExecutorService; // ADD
import java.util.concurrent.Executors; // ADD
import java.util.zip.ZipEntry; // ADD
import java.util.zip.ZipOutputStream; // ADD

// ADD THIS IMPORT if not already present for R.id.fab_export_wallets


// IMPLEMENT the WalletAdapter.OnItemLongClickListener interface
public class DashboardFragment extends Fragment implements WalletAdapter.OnItemLongClickListener, WalletAdapter.OnDetailsClickListener {

    private FragmentDashboardBinding binding;
    private DashboardViewModel dashboardViewModel; // Make ViewModel accessible
    private WalletAdapter walletAdapter; // Adapter for the RecyclerView

    // ADD THESE MEMBER VARIABLES
    private static final String TAG = "DashboardFragmentExport"; // For logging, changed slightly to avoid conflict if TAG exists
    private static final String DEFAULT_EXPORT_FILE_NAME = "byebit_wallets_export.zip";
    private ActivityResultLauncher<Intent> exportWalletsLauncher;
    private List<WalletHandle> walletsToExportHolder; // To hold wallets between SAF launch and result
    private final ExecutorService executorService = Executors.newSingleThreadExecutor(); // For background zipping
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper()); // To post results to UI thread

    private WalletHandle currentWalletForDetails;

    // ADD THIS ENTIRE onCreate METHOD
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true); // ADD THIS LINE to indicate the fragment has menu items

        // ViewModel is initialized in onCreateView, which is fine.
        // Initialize ActivityResultLauncher here
        exportWalletsLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null && walletsToExportHolder != null && !walletsToExportHolder.isEmpty()) {
                            // Perform zipping in background
                            zipWalletsToUri(uri, walletsToExportHolder);
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

    // ADD THIS METHOD to inflate the menu
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.dashboard_menu, menu);
    }

    // ADD THIS METHOD to handle menu item selections
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_refresh_balances) {
            if (dashboardViewModel != null) {
                dashboardViewModel.refreshAllWalletBalances();
                Toast.makeText(getContext(), "Refreshing balances...", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // Initialize ViewModel
        dashboardViewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        // Inflate layout using view binding
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Setup RecyclerView
        setupRecyclerView();

        // Remove old TextView observation
        // final TextView textView = binding.textDashboard;
        // dashboardViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        // Set click listener for the create wallet FAB
        binding.fabCreateWallet.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(v);
            // MODIFIED: Use R.id directly as R should be imported
            navController.navigate(R.id.action_dashboard_to_createWalletFragment);
        });

        // MODIFY THIS CLICK LISTENER
        binding.fabExportWallets.setOnClickListener(v -> {
            // Ensure ViewModel is available and LiveData has a value
            if (dashboardViewModel != null && dashboardViewModel.getSavedWallets().getValue() != null) {
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

        // Observe the LiveData from the ViewModel
        dashboardViewModel.getSavedWallets().observe(getViewLifecycleOwner(), wallets -> {
            // Update the adapter's data when the wallet list changes
            if (wallets != null) {
                walletAdapter.setWallets(wallets);
            }
        });

        dashboardViewModel.getCredentialsResult().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;

            if (result.isLoading()) {
                Toast.makeText(getContext(), "Loading wallet details...", Toast.LENGTH_SHORT).show();
            } else if (result.isError()) {
                Toast.makeText(getContext(), "Error: " + result.getError(), Toast.LENGTH_LONG).show();
                dashboardViewModel.clearCredentialsResult();
                this.currentWalletForDetails = null;
            } else if (result.isSuccess() && result.getCredentials() != null) {
                if (this.currentWalletForDetails != null) {
                    showWalletDetailsDialog(this.currentWalletForDetails, result.getCredentials());
                } else {
                    Log.e(TAG, "currentWalletForDetails is null when credentials result received.");
                }
                dashboardViewModel.clearCredentialsResult();
            }
        });
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = binding.recyclerViewWallets; // Get RecyclerView from binding
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext())); // Set LayoutManager
        recyclerView.setHasFixedSize(true); // Optimization if item size doesn't change

        walletAdapter = new WalletAdapter(); // Create the adapter
        recyclerView.setAdapter(walletAdapter); // Set the adapter

        // Set the item click listener
        walletAdapter.setOnItemClickListener(wallet -> {
            // Get the wallet address
            String walletAddress = wallet.getAddress();

            // Get the clipboard manager
            ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);

            // Create a ClipData object
            ClipData clip = ClipData.newPlainText("Wallet Address", walletAddress);

            // Set the clip data to the clipboard
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                // Show a confirmation message
                Toast.makeText(getContext(), "Address copied to clipboard", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Failed to copy address", Toast.LENGTH_SHORT).show();
            }

            // --- Add logic to fetch and display balance ---
            // Request the balance from the ViewModel
            // We observe it here directly for simplicity, but in a complex app,
            // you might manage this observation differently (e.g., in a detail fragment)
            dashboardViewModel.fetchBalanceForAddress(walletAddress);
            // --- End of balance fetching logic ---
        });

        // ADD: Set the long click listener
        walletAdapter.setOnItemLongClickListener(this);
        walletAdapter.setOnDetailsClickListener(this); // SET THE NEW LISTENER
    }


    @Override
    public void onDetailsClick(WalletHandle wallet) {
        if (getContext() == null) return;
        this.currentWalletForDetails = wallet;

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

    // ADD THIS METHOD
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

    // ADD THIS METHOD
    private void zipWalletsToUri(Uri destinationUri, List<WalletHandle> wallets) {
        if (getContext() == null) {
            Log.e(TAG, "Context is null in zipWalletsToUri. Aborting.");
            Toast.makeText(requireActivity().getApplicationContext(), "Export failed: Internal error (context lost)", Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(getContext(), "Exporting wallets...", Toast.LENGTH_SHORT).show();
        executorService.execute(() -> {
            boolean success = false;
            String errorMessage = null;
            File walletsDir = null; // Declare here for broader scope in try-catch

            try {
                // Ensure context is still valid inside executor
                Context currentContext = getContext();
                if (currentContext == null) {
                    throw new IOException("Context became null during background execution.");
                }
                walletsDir = currentContext.getFilesDir();
                if (!walletsDir.exists() || !walletsDir.isDirectory()) {
                    throw new IOException("Wallets directory not found at: " + walletsDir.getAbsolutePath());
                }

                try (OutputStream fos = currentContext.getContentResolver().openOutputStream(destinationUri);
                     ZipOutputStream zos = new ZipOutputStream(fos)) {

                    if (fos == null) {
                        throw new IOException("Failed to open output stream for URI: " + destinationUri);
                    }

                    byte[] buffer = new byte[4096]; // Increased buffer size
                    for (WalletHandle wallet : wallets) {
                        File walletFile = new File(walletsDir, wallet.getFilename());
                        if (walletFile.exists() && walletFile.isFile()) {
                            Log.d(TAG, "Adding to zip: " + walletFile.getAbsolutePath() + " (Size: " + walletFile.length() + " bytes)");
                            ZipEntry zipEntry = new ZipEntry(wallet.getFilename());
                            zos.putNextEntry(zipEntry);
                            try (FileInputStream fis = new FileInputStream(walletFile)) {
                                int length;
                                while ((length = fis.read(buffer)) > 0) {
                                    zos.write(buffer, 0, length);
                                }
                            }
                            zos.closeEntry();
                            Log.d(TAG, "Successfully added " + wallet.getFilename() + " to zip.");
                        } else {
                            Log.w(TAG, "Wallet file not found or is not a file, skipping: " + walletFile.getAbsolutePath());
                        }
                    }
                    zos.finish(); // Ensure all data is written
                    success = true;
                    Log.d(TAG, "Zipping process completed successfully.");
                }
            } catch (IOException e) {
                Log.e(TAG, "Error zipping wallets to " + destinationUri, e);
                errorMessage = e.getMessage();
                if (walletsDir != null) { // Log walletsDir path if it was initialized
                    Log.e(TAG, "Wallets directory was: " + walletsDir.getAbsolutePath());
                }
            } catch (NullPointerException e) {
                Log.e(TAG, "NullPointerException during zipping", e);
                errorMessage = "A null pointer occurred during export.";
            }


            final boolean finalSuccess = success;
            final String finalErrorMessage = errorMessage;
            mainThreadHandler.post(() -> {
                if (getContext() != null) { // Check context again before showing Toast
                    if (finalSuccess) {
                        Toast.makeText(getContext(), "Wallets exported successfully!", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getContext(), "Export failed: " + (finalErrorMessage != null ? finalErrorMessage : "Unknown error"), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Log.w(TAG, "Context was null when trying to post zipping result.");
                }
            });
        });
    }

    // ADD THIS METHOD (or add to existing onDestroy if you have one)
    @Override
    public void onDestroy() {
        super.onDestroy();
        // Shutdown executor service
        if (executorService != null && !executorService.isShutdown()) {
            Log.d(TAG, "Shutting down executor service.");
            executorService.shutdown();
        }
    }

    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

} // This is the closing brace of the DashboardFragment class
