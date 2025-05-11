package com.example.byebit.ui.createwallet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast; // Import Toast
import android.widget.CheckBox; // Import CheckBox

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController; // Import NavController
import androidx.navigation.Navigation; // Import Navigation helper

import com.example.byebit.databinding.FragmentCreateWalletBinding;
import com.example.byebit.security.AuthenticationFailureReason;
import com.example.byebit.security.AuthenticationListener;
import com.example.byebit.security.BiometricService;

public class CreateWalletFragment extends Fragment {

    private FragmentCreateWalletBinding binding;
    private CreateWalletViewModel createWalletViewModel;
    private BiometricService biometricService;
    // ADD THIS FIELD
    private CheckBox savePasswordCheckBox;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        createWalletViewModel = new ViewModelProvider(this).get(CreateWalletViewModel.class);
        biometricService = new BiometricService(this);

        binding = FragmentCreateWalletBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // ADD THIS LINE TO GET REFERENCE TO THE CHECKBOX
        savePasswordCheckBox = binding.checkboxSavePassword;

        // Set click listener for the create button
        binding.buttonCreateWallet.setOnClickListener(v -> attemptCreateWallet());

        // Observe the creation result
        createWalletViewModel.getCreationResult().observe(getViewLifecycleOwner(), result -> {
            if (result.isSuccess()) {
                // Wallet created successfully, navigate back
                Toast.makeText(getContext(), "Wallet '" + result.getSuccessData().getName() + "' created!", Toast.LENGTH_SHORT).show();
                NavController navController = Navigation.findNavController(root);
                navController.popBackStack(); // Go back to the previous screen (Dashboard)
            } else if (result.isError()) {
                // Show error message
                Toast.makeText(getContext(), "Error creating wallet: " + result.getErrorMessage(), Toast.LENGTH_LONG).show();
            }
            // Loading state is handled by the isLoading observer
        });

        // Observe the loading state
        createWalletViewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading) {
                binding.progressBarCreating.setVisibility(View.VISIBLE);
                binding.buttonCreateWallet.setEnabled(false); // Disable button while loading
            } else {
                binding.progressBarCreating.setVisibility(View.GONE);
                binding.buttonCreateWallet.setEnabled(true); // Re-enable button
            }
        });

        return root;
    }

    private void attemptCreateWallet() {
        String name = binding.editTextWalletName.getText().toString().trim();
        String password = binding.editTextPassword.getText().toString();
        String confirmPassword = binding.editTextConfirmPassword.getText().toString();
        // ADD THIS LINE TO GET THE CHECKBOX STATE
        boolean savePassword = savePasswordCheckBox.isChecked();


        // Clear previous errors
        binding.textInputLayoutWalletName.setError(null);
        binding.textInputLayoutPassword.setError(null);
        binding.textInputLayoutConfirmPassword.setError(null);

        boolean isValid = true;

        if (name.isEmpty()) {
            binding.textInputLayoutWalletName.setError("Wallet name cannot be empty");
            isValid = false;
        }

        if (password.isEmpty()) {
            binding.textInputLayoutPassword.setError("Password cannot be empty");
            isValid = false;
        }

        if (confirmPassword.isEmpty()) {
            binding.textInputLayoutConfirmPassword.setError("Confirm password cannot be empty");
            isValid = false;
        }

        if (!password.equals(confirmPassword)) {
            binding.textInputLayoutConfirmPassword.setError("Passwords do not match");
            isValid = false;
        }

        if (isValid) {
            // Call ViewModel to create the wallet
            biometricService.encrypt(name, password, new AuthenticationListener() {
                @Override
                public void onSuccess(byte[] result, byte[] iv) {
                    // MODIFY THIS LINE TO PASS THE savePassword BOOLEAN
                    createWalletViewModel.createWallet(name, password, result, iv, savePassword);
                }

                @Override
                public void onFailure(AuthenticationFailureReason reason) {
                    // TODO: Handle authentication failure appropriately
                    // For now, just log or show a generic error
                    Toast.makeText(getContext(), "Biometric authentication failed: " + reason, Toast.LENGTH_SHORT).show();
                    // Consider if wallet creation should proceed without biometric auth or be cancelled
                    // For this task, we assume biometric auth is required if attempted.
                    // If it fails, the operation stops here.
                }

                @Override
                public void onCancel() {
                    // TODO: Handle authentication cancellation
                    Toast.makeText(getContext(), "Biometric authentication cancelled", Toast.LENGTH_SHORT).show();
                    // Operation is cancelled
                }
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
