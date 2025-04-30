package com.example.byebit.ui.createwallet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast; // Import Toast

import androidx.annotation.NonNull;
import androidx.annotation.Nullable; // Import Nullable
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController; // Import NavController
import androidx.navigation.Navigation; // Import Navigation helper

import com.example.byebit.databinding.FragmentCreateWalletBinding;

public class CreateWalletFragment extends Fragment {

    private FragmentCreateWalletBinding binding;
    private CreateWalletViewModel createWalletViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        createWalletViewModel = new ViewModelProvider(this).get(CreateWalletViewModel.class);

        binding = FragmentCreateWalletBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

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

        // Add more password strength validation if needed

        if (isValid) {
            // Call ViewModel to create the wallet
            createWalletViewModel.createWallet(name, password);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
