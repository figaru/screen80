package com.devxop.screen80;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.socket.engineio.client.transports.WebSocket;

import static com.devxop.screen80.MainActivity.LOG_TAG;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = LoginActivity.class.getSimpleName();
    private Button btnLogin;
    private EditText inputEmail;
    private EditText inputPassword;
    private String device_id;
    private String authorization_code;

    private io.socket.client.Socket socket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        inputEmail = (EditText) findViewById(R.id.email);
        inputPassword = (EditText) findViewById(R.id.password);
        btnLogin = (Button) findViewById(R.id.btnLogin);


        device_id = StorageManager.get(getApplicationContext(), "device_id");

        // Login button Click Event
        btnLogin.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                String email = inputEmail.getText().toString().trim();
                String password = inputPassword.getText().toString().trim();

                // Check for empty data in the form
                if (!email.isEmpty() && !password.isEmpty()) {
                    // login user
                    checkLogin(email, password);
                } else {
                    // Prompt user to enter credentials
                    Toast.makeText(getApplicationContext(),
                            "Please enter the credentials!", Toast.LENGTH_LONG)
                            .show();
                }
            }

        });


        //INITIATE SOCKET
        try {
            IO.Options opts = new IO.Options();
            opts.transports = new String[]{WebSocket.NAME};
            opts.reconnection = false;
            opts.forceNew = true;
            socket = IO.socket(Config.SOCKET_API, opts);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            Log.d(TAG, e.toString());
        }
    }

    /**
     * function to verify login details in mysql db
     * */
    private void checkLogin(final String email, final String password) {
        //socket Clear
        socketEnd();

        // Tag used to cancel the request
        String tag_string_req = "req_login";

        Log.d("LOGIN", "LOGING IN....");
        Log.d("LOGIN", email);
        Log.d("LOGIN", password);
        Log.d("LOGIN", device_id);


        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                Log.d(TAG, "CONNECTED SOCKET");
                System.out.println("SOCKET CONNECTED!");

                Map<String, String> params = new HashMap<String, String>();
                params.put("user", email);
                params.put("pass", password);
                params.put("device_id", device_id);

                JSONObject json = new JSONObject(params);

                socket.emit("device.request.login", json.toString());

            }

        }).on("login.success", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                socketEnd();
                if(args != null){
                    //we only expect one argument to pass -> auth code
                    String auth_code = args[0].toString();

                    Log.d(LOG_TAG, "Login successfull -> auth code:");
                    Log.d(LOG_TAG, auth_code);
                    StorageManager.set(getApplicationContext(), StorageManager.AUTH_CODE, auth_code);

                    Intent intent = new Intent(LoginActivity.this,
                            MainActivity.class);
                    startActivity(intent);

                    finish();
                }else{
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(getApplicationContext(),
                                    "Failed login attempt! unknown error", Toast.LENGTH_LONG)
                                    .show();
                        }
                    });
                }


            }

        }).on("login.failed", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                socketEnd();
                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(getApplicationContext(),
                                "Failed login attempt!", Toast.LENGTH_LONG)
                                .show();
                    }
                });



            }

        });

        socket.connect();

    }

    private void socketEnd(){
        socket.disconnect();
        socket.off("login.failed");
        socket.off("login.success");
        socket.off(Socket.EVENT_CONNECT);
    }

}
