package com.example.byebit.ui.createwallet;

import static java.util.Optional.ofNullable;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.byebit.databinding.FragmentCreateWalletBinding;
import com.example.byebit.security.AuthenticationFailureReason;
import com.example.byebit.security.AuthenticationListener;
import com.example.byebit.security.BiometricService;

import java.util.Objects;

public class CreateWalletFragment extends Fragment {

    private FragmentCreateWalletBinding binding;
    private CreateWalletViewModel createWalletViewModel;
    private BiometricService biometricService;
    private CheckBox savePasswordCheckBox;
    private com.google.android.material.textfield.TextInputLayout recoveryPhraseInputLayout;
    private com.google.android.material.textfield.TextInputEditText recoveryPhraseEditText;
    private CheckBox showRecoveryPhraseCheckBox;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        createWalletViewModel = new ViewModelProvider(this).get(CreateWalletViewModel.class);
        biometricService = new BiometricService(this);

        binding = FragmentCreateWalletBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        savePasswordCheckBox = binding.checkboxSavePassword;
        // Initialize the new UI elements from the binding
        showRecoveryPhraseCheckBox = binding.checkboxShowRecoveryPhrase;
        recoveryPhraseInputLayout = binding.textInputLayoutRecoveryPhrase;
        recoveryPhraseEditText = binding.editTextPrivateKey; // Assuming this ID exists in your layout

        // Initially hide the recovery phrase input field
        recoveryPhraseInputLayout.setVisibility(View.GONE);

        // Set up the listener for the checkbox to toggle the visibility of the recovery phrase input field
        showRecoveryPhraseCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            recoveryPhraseInputLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });


        binding.buttonCreateWallet.setOnClickListener(v -> attemptCreateWallet());

        createWalletViewModel.getCreationResult().observe(getViewLifecycleOwner(), result -> {
            if (result.isSuccess()) {
                Toast.makeText(getContext(), "Wallet '" + result.getSuccessData().getName() + "' created!", Toast.LENGTH_SHORT).show();
                NavController navController = Navigation.findNavController(root);
                navController.popBackStack();
            } else if (result.isError()) {
                Toast.makeText(getContext(), "Error creating wallet: " + result.getErrorMessage(), Toast.LENGTH_LONG).show();
            }
        });

        createWalletViewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading) {
                binding.progressBarCreating.setVisibility(View.VISIBLE);
                binding.buttonCreateWallet.setEnabled(false);
            } else {
                binding.progressBarCreating.setVisibility(View.GONE);
                binding.buttonCreateWallet.setEnabled(true);
            }
        });

        return root;
    }

    private void attemptCreateWallet() {
        String name = binding.editTextWalletName.getText().toString().trim();
        String password = binding.editTextPassword.getText().toString();
        String confirmPassword = binding.editTextConfirmPassword.getText().toString();
        String privateKey = ofNullable(binding.editTextPrivateKey.getText())
                .map(Objects::toString)
                .orElse(null);
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

        if (isValid && savePassword) {
            biometricService.encrypt(name, password, new AuthenticationListener() {
                @Override
                public void onSuccess(byte[] result, byte[] iv) {
                    createWalletViewModel.createWallet(name, password, privateKey, result, iv);
                }

                @Override
                public void onFailure(AuthenticationFailureReason reason) {
                    Toast.makeText(getContext(), "Biometric authentication failed: " + reason, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onCancel() {
                    Toast.makeText(getContext(), "Biometric authentication cancelled", Toast.LENGTH_SHORT).show();
                }
            });
        } else if (isValid && !savePassword) {
            createWalletViewModel.createWallet(name, password, privateKey, null, null);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
