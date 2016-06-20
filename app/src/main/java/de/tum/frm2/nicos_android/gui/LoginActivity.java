//
// Copyright (C) 2016 Andreas Schulz <andreas.schulz@frm2.tum.de>
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 US


package de.tum.frm2.nicos_android.gui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import de.tum.frm2.nicos_android.nicos.ConnectionData;
import de.tum.frm2.nicos_android.util.NicosCallbackHandler;
import de.tum.frm2.nicos_android.nicos.NicosClient;
import de.tum.frm2.nicos_android.R;


public class LoginActivity extends AppCompatActivity {
    public final static String MESSAGE_CONNECTION_DATA =
            "de.tum.frm2.nicos_android.MESSAGE_CONNECTION_DATA";

    // Keep track of the login task to ensure we can cancel it if requested.
    // If authTask is not null: An attempt to login is currently running.
    private UserLoginTask authTask = null;

    private EditText editTextHostname;
    private EditText editTextPort;
    private EditText editTextUsername;
    private EditText editTextPassword;
    private View mProgressView;
    private View mLoginFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        // Set up the login form.

        editTextHostname = (EditText) findViewById(R.id.textedit_hostname);
        editTextPort = (EditText) findViewById(R.id.textedit_port);
        editTextUsername = (EditText) findViewById(R.id.textedit_username);
        editTextPassword = (EditText) findViewById(R.id.textedit_password);

        // Action when pressing "return" key while entering text on editTextPassword
        editTextPassword.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button loginButton = (Button) findViewById(R.id.loginButton);
        loginButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
    }

    @Override
    public void onBackPressed() {
        // If a login is attempting, cancel login sequence.
        if (authTask != null) {
            authTask.cancel(true);
            NicosClient.getClient().disconnect();
            showProgress(false);
            authTask = null;
            return;
        }

        super.onBackPressed();
    }

    private void attemptLogin() {
        if (authTask != null) {
            // A login task is already running.
            return;
        }

        // Reset errors messages inside editTexts.
        editTextHostname.setError(null);
        editTextPort.setError(null);
        editTextUsername.setError(null);
        editTextPassword.setError(null);

        // Parse data from editTexts.
        String hostname = editTextHostname.getText().toString();
        String portString = editTextPort.getText().toString();
        Integer port;
        try {
            // Despite the textEdit only allowing numeral input, this might fail when the user has
            // entered an empty string.
            port = Integer.valueOf(portString);
        } catch (NumberFormatException e) {
            port = -1;
        }

        String username = editTextUsername.getText().toString();
        String password = editTextPassword.getText().toString();

        // Whether login should be canceled (= invalid input data).
        boolean cancel = false;
        // First View (= editText) with invalid data which will be focussed once login attempt
        // failed.
        View focusView = null;

        if (TextUtils.isEmpty(hostname)) {
            editTextHostname.setError(getString(R.string.error_field_required));
            focusView = editTextHostname;
            cancel = true;
        }

        if (TextUtils.isEmpty(portString) || port == -1) {
            editTextPort.setError(getString(R.string.error_field_required));
            if (focusView == null) {
                focusView = editTextPort;
            }
            cancel = true;
        }

        if (TextUtils.isEmpty(username)) {
            editTextUsername.setError(getString(R.string.error_field_required));
            if (focusView == null) {
                focusView = editTextUsername;
            }
            cancel = true;
        }

        // Note: editTextPassword may be empty (for example: guest account).

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Create connection data object for NicosClient.
            ConnectionData connData = new ConnectionData(hostname,
                    port, username, password.toCharArray(), false);

            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            editTextPassword.setText("");
            NicosClient.getClient().disconnect();
            authTask = new UserLoginTask(this, connData);
            authTask.execute((Void) null);
        }
    }

    // BEGIN * from Android Login Template *
    // Shows the progress UI and hides the login form.
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }
    // END * from Android Login Template *


    // AsyncTask is an Android construct which can a common Java Thread in simple situations.
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        // A class that listens to errors from the NicosClients, to retrieve why connecting
        // might have failed.
        public class LastSignalErrorStore implements NicosCallbackHandler {
            private String lastErrorMessage;

            @Override
            public void handleSignal(String signal, Object data, Object args) {
                try {
                    lastErrorMessage = (String) data;
                }
                catch (Exception e) {
                    // There probably wasn't any error.
                }
            }

            public String getLastErrorMessage() {
                return lastErrorMessage;
            }
        }

        private LoginActivity loginActivity;
        private final ConnectionData connData;
        // If an error occurred, this String contains the error message.
        private String error;

        UserLoginTask(LoginActivity loginActivity, ConnectionData connData) {
            this.loginActivity = loginActivity;
            this.connData = connData;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            LastSignalErrorStore store = new LastSignalErrorStore();
            NicosClient.getClient().registerCallbackHandler(store);
            try {
                NicosClient.getClient().connect(connData);
            } catch (RuntimeException e) {
                // Is already connected...
            }
            NicosClient.getClient().unregisterCallbackHandler(store);
            // Connect was attempted. Look if client is actually connected...
            if (NicosClient.getClient().isConnected()) {
                return true;
            }
            error = store.getLastErrorMessage();
            return false;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            // Automatically called with doInBackground's return value.
            // There is no longer an attempt to login running (as we have either logged in or the
            // server rejected us).
            authTask = null;
            showProgress(false);

            if (success) {
                // Server accepted our credentials. Start MainActivity and provide it with the
                // connection data that worked.
                // This 'Intent' construct is essentially Android's way to carry over currently
                // existing data to our new Activity.
                Intent intent = new Intent(loginActivity, MainActivity.class);
                intent.putExtra(MESSAGE_CONNECTION_DATA, connData);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            } else {
                // Server rejected credentials. Show the reason for failure in an alert dialog
                // and return to the login form.
                try {
                    AlertDialog alertDialog = new AlertDialog.Builder(LoginActivity.this).create();
                    alertDialog.setTitle("Login failed");
                    alertDialog.setMessage(error);
                    alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Okay",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    alertDialog.show();
                }
                catch (Exception e) {
                    // Activity isn't running anymore (user probably put application in background).
                }
            }
        }
    }
}

