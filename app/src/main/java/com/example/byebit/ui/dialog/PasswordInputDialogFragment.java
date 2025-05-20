package com.example.byebit.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import android.widget.CheckBox; // Add this import

import androidx.fragment.app.DialogFragment;

import com.example.byebit.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;


public class PasswordInputDialogFragment extends DialogFragment {

    private static final String ARG_TITLE = "title";
    private static final String ARG_HINT = "hint";
    private static final String ARG_POSITIVE_BUTTON_TEXT = "positive_button_text";

    private TextInputEditText passwordEditText;
    private CheckBox savePasswordCheckBox; // Add this field

    // RxJava Subject to emit dialog results
    private final PublishSubject<PasswordDialogResult> resultSubject = PublishSubject.create();

    public static PasswordInputDialogFragment newInstance(String title, String hint, String positiveButtonText) {
        PasswordInputDialogFragment fragment = new PasswordInputDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_HINT, hint);
        args.putString(ARG_POSITIVE_BUTTON_TEXT, positiveButtonText);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Returns an Observable that emits the dialog result (password or cancellation)
     * and then completes.
     *
     * @return Observable<PasswordDialogResult>
     */
    public Observable<PasswordDialogResult> getDialogEvents() {
        return resultSubject.hide(); // Hide to prevent casting to Subject
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Bundle args = getArguments();
        String title = "";
        String hint = getString(R.string.password_hint); // Default hint
        String positiveButtonText = getString(R.string.ok_button); // Default OK

        if (args != null) {
            title = args.getString(ARG_TITLE, "");
            hint = args.getString(ARG_HINT, getString(R.string.password_hint));
            positiveButtonText = args.getString(ARG_POSITIVE_BUTTON_TEXT, getString(R.string.ok_button));
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_password_input, null);

        passwordEditText = view.findViewById(R.id.password_edit_text);
        TextInputLayout passwordTextInputLayout = view.findViewById(R.id.password_text_input_layout);
        savePasswordCheckBox = view.findViewById(R.id.save_password_checkbox); // Find the checkbox

        if (!TextUtils.isEmpty(hint)) {
            passwordTextInputLayout.setHint(hint);
        }
        // The EditText hint in XML can also serve as a fallback if TextInputLayout's hint is not set.

        builder.setView(view)
                .setTitle(title)
                .setPositiveButton(positiveButtonText, null) // Listener set in onStart to prevent auto-dismiss
                .setNegativeButton(R.string.cancel_button, (dialog, id) -> { // This listener is called before onDismiss
                    resultSubject.onNext(PasswordDialogResult.cancelled());
                    // Dialog dismisses automatically, which will trigger onDismiss and complete the subject.
                });
        
        // Prevent dialog from being cancellable by touching outside or back button,
        // if that's the desired behavior. Default is true.
        // setCancelable(false); 

        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        AlertDialog d = (AlertDialog) getDialog();
        if (d != null) {
            final TextInputLayout passwordTextInputLayout = d.findViewById(R.id.password_text_input_layout);

            d.getButton(Dialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String password = passwordEditText.getText().toString();
                boolean savePassword = savePasswordCheckBox.isChecked(); // Get checkbox state

                if (TextUtils.isEmpty(password)) {
                    if (passwordTextInputLayout != null) {
                        passwordTextInputLayout.setError(getString(R.string.password_cannot_be_empty));
                    } else {
                        Toast.makeText(getContext(), R.string.password_cannot_be_empty, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    if (passwordTextInputLayout != null) {
                        passwordTextInputLayout.setError(null); // Clear error
                    }
                    // Pass the password and the checkbox state
                    resultSubject.onNext(PasswordDialogResult.success(password, savePassword));
                    dismiss(); // Dismiss dialog on success
                }
            });
        }
    }

    @Override
    public void onDetach() {
        // No listener to detach
        super.onDetach();
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        // Complete the subject when the dialog is dismissed, for any reason.
        // This ensures subscribers are notified that the dialog interaction has ended.
        resultSubject.onComplete();
    }
}
