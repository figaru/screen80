package com.devxop.screen80;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.AlarmManager;
import android.app.DownloadManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.VideoView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.socket.engineio.client.transports.WebSocket;

public class MainActivity extends AppCompatActivity {

    static final String LOG_TAG = "ServiceThread";

    private io.socket.client.Socket socket;

    private static final String SERVER = "http://10.0.2.2";

    private static final int ID_REQUEST_STORAGE = 100;
    private static final int ID_REQUEST_READ_STORAGE = 110;

    private static String STORAGE_VIDEO = "";
    private static String STORAGE_IMAGE = "";
    private static String APP_PATH;

    private static BroadcastReceiver br;


    private static WebView webView;

    private VideoView videoView;
    private VideoView videoViewNext;

    private VideoView[] videoViews;
    private static int nextViewIndex = 0;
    private static int currentViewIndex = 0;

    private static int videoPlayIndex = 0;
    public boolean playingVideos = false;
    private static String videoPlayName = "";


    private ImageView imageView;
    Runnable imageRunnable;
    private static int imagePlayIndex = 0;
    public boolean playingImages = false;
    private static String imagePlayName = "";


    private static int imageInterval = 6000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        imageView = findViewById(R.id.myImageView);
        videoView = findViewById(R.id.myVideoView);



        /*videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
                Log.d(LOG_TAG, "Video complete event");
                playingVideos = false;
                setDisplayView("videos");
            }
        });*/

        videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                setDisplayView("videos");
                return false;
            }
        });

        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(final MediaPlayer mp) {
                Log.d("MEDIAL PLAYER", "Duration video:´" + videoView.getDuration());
                /*PlaybackParams myPlayBackParams = new PlaybackParams();
                myPlayBackParams.setSpeed(2f); //you can set speed here
                mp.setPlaybackParams(myPlayBackParams);*/
                mp.setLooping(true);
                //mp.start();
            }
        });



        br = new DownloadBroadcastReceiver();
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        filter.addAction(Intent.ACTION_INPUT_METHOD_CHANGED);
        this.registerReceiver(br, filter);

        try {
            APP_PATH = getDataDir(getApplicationContext());
            StorageManager.set(getApplicationContext(), "app_path", APP_PATH);
        } catch (Exception e) {
            e.printStackTrace();
        }
        STORAGE_VIDEO = APP_PATH + "/devxop/videos";
        STORAGE_IMAGE = APP_PATH + "/devxop/images";
        Config.STORAGE_UPGRADE = APP_PATH + "/devxop/apk";


        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // No explanation needed; request the permission
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    ID_REQUEST_STORAGE);
        } else {
            // Permission has already been granted -> Start
            //setup published view offline
            String view = StorageManager.get(getApplicationContext(), StorageManager.PUBLISHED_VIEW);
            setDisplayView(view);

            //Init and start Socket
            initSocket();

            //set 15 second server ping
            intervalUpdate();

        }



        /*webView = (WebView) findViewById(R.id.myWebView);
        webView.setWebViewClient(new myWebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);*/
        //webView.loadUrl("http://10.0.2.2:5000");


    }

    public void intervalUpdate() {
        if (socket != null && socket.connected()) {
            socket.emit("device.request.ping",
                    StorageManager.get(getApplicationContext(), StorageManager.DEVICE_ID),
                    StorageManager.get(getApplicationContext(), StorageManager.UPDATE_VERSION));
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                intervalUpdate();
            }
        }, 15 * 1000);
    }

    public void resetViews() {

        playingImages = false;
        playingVideos = false;

        imageView.removeCallbacks(imageRunnable);
        imageView.post(new Runnable() {
            @Override
            public void run() {

                imageView.setVisibility(View.INVISIBLE);
            }
        });

        videoView.post(new Runnable() {
            @Override
            public void run() {
                videoView.stopPlayback();
                //videoView.setVideoPath("");
                videoView.setVisibility(View.INVISIBLE);
            }
        });
    }

    public void setDisplayView(String viewType) {
        Log.d("SET VIEW", viewType);
        switch (viewType) {
            case "videos":

                resetViews();
                playingVideos = true;
                StorageManager.set(getApplicationContext(), StorageManager.PUBLISHED_VIEW, "videos");
                playVideos(false);
                break;
            case "images":
                try {
                    imageInterval = Integer.parseInt(StorageManager.get(getApplicationContext(), StorageManager.IMAGE_INTERVAL));
                } catch (NumberFormatException e) {
                    //catch
                }

                if(playingImages)
                    break;


                resetViews();
                playingImages = true;
                StorageManager.set(getApplicationContext(), StorageManager.PUBLISHED_VIEW, "images");
                playImages();
                break;
        }

    }

    public void playImages() {

        imageRunnable = new Runnable() {

            @Override
            public void run() {
                imageView.setVisibility(View.VISIBLE);

                File[] files = getFiles(STORAGE_IMAGE);
                if (files != null && files.length > 0) {

                    if (imagePlayIndex > files.length - 1) {
                        //reset index
                        imagePlayIndex = 0;
                    }
                    String imageName = files[imagePlayIndex].getName();

                    //image not the same -> change image view
                    String filePath = files[imagePlayIndex].getAbsolutePath();
                    imageView.setImageBitmap(BitmapFactory.decodeFile(filePath));

                    //increment after video play
                    imagePlayName = imageName;
                    imagePlayIndex++;
                }

                imageView.postDelayed(imageRunnable, imageInterval);
            }
        };

        imageView.post(imageRunnable);
    }

    public void playVideos(final boolean startup) {

        videoView.post(new Runnable() {
            @Override
            public void run() {
                videoView.setVisibility(View.VISIBLE);

                File[] files = getFiles(STORAGE_VIDEO);
                if (files != null && files.length > 0) {
                    boolean removeFiles = false;
                    String publishedVideo = StorageManager.get(getApplicationContext(), StorageManager.PUBLISHED_VIDEO);
                    for(int i = 0; i < files.length; i++){
                        File file = files[i];

                        if(file != null && file.exists()){

                            Log.d("PLAYING VIDEO", publishedVideo);
                            Log.d("PLAYING VIDEO", file.getName());
                            if(file.getName().equals(publishedVideo)){
                                videoView.setVideoPath(file.getAbsolutePath());
                                videoView.start();
                            }else{
                                removeFiles = true;
                            }
                        }
                    }

                    if(removeFiles){
                        removeAllExceptOne(STORAGE_VIDEO, publishedVideo);
                    }
                    /*File file;
                    if(file != null){
                        //image not the same -> change image view

                    }*/


                }
            }
        });
    }


    private void initSocket() {
        Log.d(LOG_TAG, "starting socket connection...");

        try {
            IO.Options opts = new IO.Options();
            opts.transports = new String[]{WebSocket.NAME};
            opts.forceNew = true;
            socket = IO.socket(Config.SOCKET_API, opts);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            Log.d(LOG_TAG, e.toString());
        }
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                Log.d(LOG_TAG, "CONNECTED SOCKET");
                System.out.println("SOCKET CONNECTED!");
                socket.emit("device.request.startup",
                        StorageManager.get(getApplicationContext(), StorageManager.DEVICE_ID),
                        Config.VERSION);

                socket.emit("device.request.sync",
                        StorageManager.get(getApplicationContext(), "device_id"),
                        StorageManager.get(getApplicationContext(), StorageManager.AUTH_CODE));
            }

        }).on("update", new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                if (args != null && args.length > 0) {
                    //we received v_id update
                    StorageManager.set(getApplicationContext(), StorageManager.UPDATE_VERSION, args[0].toString());
                    Log.d("UPDATE VERSION", "VERSION ID_" + args[0].toString());
                }
                Log.d("SOCKET", "Received update from server");
                socket.emit("device.request.sync",
                        StorageManager.get(getApplicationContext(), "device_id"),
                        StorageManager.get(getApplicationContext(), StorageManager.AUTH_CODE));
            }

        }).on("restart", new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                Log.d("SYSTEM FORCE RESTART", "---------------------------------------------------------");
                Intent mStartActivity = new Intent(getApplicationContext(), LauncherActivity.class);
                int mPendingIntentId = 123456;
                PendingIntent mPendingIntent = PendingIntent.getActivity(getApplicationContext(), mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
                AlarmManager mgr = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
                mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
                System.exit(0);

            }

        }).on("video", new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                Log.d(LOG_TAG, "RECEIVED VIDEO EVENT ");

                try {
                    JSONObject obj = new JSONObject(args[0].toString());

                    String fileUrl = obj.getString("url"); //To retrieve vehicleId
                    final String fileId = obj.getString("file_id");
                    final String fileExt = obj.getString("file_ext");

                    Path path = Paths.get(STORAGE_VIDEO + "/" + fileId + fileExt);
                    Files.createDirectories(path.getParent());

                    Log.d("ACTIVITY", obj.toString());
                    Log.d("ACTIVITY", fileUrl);
                    Log.d("ACTIVITY", fileId);
                    Log.d("ACTIVITY", fileExt);

                    File file = new File(String.valueOf(path));
                    if (file.exists()) {
                        //file.delete();

                        Log.d("ACTIVITY", "File exists");

                        Intent intent = new Intent();
                        intent.setAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
                        intent.putExtra("action", "videos");
                            intent.putExtra("path", STORAGE_VIDEO);
                        intent.putExtra("filename", "" + fileId + fileExt);
                        sendBroadcast(intent);
                    } else {
                        File createFile = new File(String.valueOf(path) + "_undefined");
                        createFile.createNewFile();


                        Log.d(LOG_TAG, "File Download: File does not exist -> downloading...");
                        DownloadFile download = new DownloadFile(getApplicationContext(),
                                SERVER + fileUrl,
                                STORAGE_VIDEO,
                                fileId + fileExt, new AsyncResponse() {
                            @Override
                            public void processFinish(String callbackResponse) {
                                Log.d("Callback response ->", callbackResponse);
                                //process after callback
                                Intent intent = new Intent();
                                intent.setAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
                                intent.putExtra("action", "videos");
                                intent.putExtra("path", STORAGE_VIDEO);
                                intent.putExtra("filename", "" + fileId + fileExt);
                                sendBroadcast(intent);
                            }
                        });

                        download.execute(); //set parameter avoid crash
                    }
                }catch (JSONException | IOException e){
                    Log.d("Error", e.toString());
                }

                /*try {

                    JSONArray jsonArray = new JSONArray(args[0].toString());
                    ArrayList<String> videosList = new ArrayList<String>();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i); //0 for just retrieving first object you can loop it
                        String fileUrl = obj.getString("url"); //To retrieve vehicleId
                        String fileId = obj.getString("file_id");
                        String fileExt = obj.getString("file_ext");

                        Log.d("DOWNLOAD", SERVER + fileUrl);

                        videosList.add(fileId + fileExt);


                        Path path = Paths.get(STORAGE_VIDEO + "/" + fileId + fileExt);
                        Files.createDirectories(path.getParent());

                        File file = new File(String.valueOf(path));
                        if (file.exists()) {
                            //file.delete();
                            Intent intent = new Intent();
                            intent.setAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
                            intent.putExtra("action", "videos");
                            intent.putExtra("path", STORAGE_VIDEO);
                            sendBroadcast(intent);
                        } else {
                            File createFile = new File(String.valueOf(path) + "_undefined");
                            createFile.createNewFile();


                            Log.d(LOG_TAG, "File Download: File does not exist -> downloading...");
                            DownloadFile download = new DownloadFile(getApplicationContext(),
                                    SERVER + fileUrl,
                                    STORAGE_VIDEO,
                                    fileId + fileExt, new AsyncResponse() {
                                @Override
                                public void processFinish(String callbackResponse) {
                                    Log.d("Callback response ->", callbackResponse);
                                    //process after callback
                                    Intent intent = new Intent();
                                    intent.setAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
                                    intent.putExtra("action", "videos");
                                    intent.putExtra("path", STORAGE_VIDEO);
                                    sendBroadcast(intent);
                                }
                            });

                            download.execute(); //set parameter avoid crash
                        }

                    }
                    StorageManager.set(getApplicationContext(), StorageManager.VIDEOS, videosList.toString());


                } catch (JSONException | IOException e) {
                    e.printStackTrace();
                }*/
            }

        }).on("image", new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                Log.d(LOG_TAG, "RECEIVED IMAGE EVENT");
                if (args != null && args.length == 2) {
                    //args.length == 2 -> interval exists
                    StorageManager.set(getApplicationContext(), StorageManager.IMAGE_INTERVAL, args[1].toString());
                }

                try {

                    JSONArray jsonArray = new JSONArray(args[0].toString());
                    ArrayList<String> imagesList = new ArrayList<String>();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i); //0 for just retrieving first object you can loop it
                        String fileUrl = obj.getString("url"); //To retrieve vehicleId
                        String fileId = obj.getString("file_id");
                        String fileExt = obj.getString("file_ext");

                        Log.d("DOWNLOAD", SERVER + fileUrl);

                        imagesList.add(fileId + fileExt);


                        Path path = Paths.get(STORAGE_IMAGE + "/" + fileId + fileExt);
                        Files.createDirectories(path.getParent());

                        File file = new File(String.valueOf(path));
                        if (file.exists()) {
                            //file.delete();
                            Intent intent = new Intent();
                            intent.setAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
                            intent.putExtra("action", "images");
                            intent.putExtra("path", STORAGE_IMAGE);
                            sendBroadcast(intent);
                        } else {
                            File createFile = new File(path.toString() + "_undefined");
                            createFile.createNewFile();


                            //Log.d(LOG_TAG, "File Download: File does not exist -> downloading...");
                            DownloadFile download = new DownloadFile(getApplicationContext(),
                                    SERVER + fileUrl,
                                    STORAGE_IMAGE,
                                    fileId + fileExt, new AsyncResponse() {
                                @Override
                                public void processFinish(String callbackResponse) {
                                    Log.d("Callback response ->", callbackResponse);
                                    //process after callback
                                    Intent intent = new Intent();
                                    intent.setAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
                                    intent.putExtra("action", "images");
                                    intent.putExtra("path", STORAGE_IMAGE);
                                    sendBroadcast(intent);
                                }
                            });

                            download.execute();//worthless param avoid exception
                        }

                    }
                    StorageManager.set(getApplicationContext(), StorageManager.IMAGES, imagesList.toString());


                } catch (JSONException | IOException e) {
                    e.printStackTrace();
                }
            }

        }).on("dispose", new Emitter.Listener() {
            //by receiveing this message we want to clear all data -> device Id etc.
            //unauthorized access or other errors
            @Override
            public void call(Object... args) {
                Log.d(LOG_TAG, "disposing...");
                StorageManager.set(getApplicationContext(), StorageManager.AUTH_CODE, "");

                Intent intent = new Intent(MainActivity.this,
                        LoginActivity.class);
                startActivity(intent);

                finish();
            }

        }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                Log.d(LOG_TAG, "Disconected socket");
            }

        });
        socket.connect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(br != null)
            unregisterReceiver(br);
    }

    public void removeFileWithName(String location, String filename) {
        File[] files = getFiles(location);
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                if (files[i].getName() == filename) {
                    files[i].delete();
                    break;
                }
            }
        }
    }

    private void removeFileFromIndex(String location, int index) {
        File[] files = getFiles(location);
        if (files != null) {
            files[index].delete();
        }
    }

    public void removeAllFiles(String location) {
        File[] files = getFiles(location);
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                files[i].delete();
            }

        }
    }

    public void removeAllExceptOne(String location, String filename) {
        File[] files = getFiles(location);
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                Log.d("REMOVE All EXCEPT ONE", files[i].getName());
                Log.d("REMOVE All EXCEPT ONE", filename);

                if(files[i].getName().equals(filename)){
                    Log.d("REMOVE All EXCEPT ONE", "keep file");
                    //keep
                }else{
                    //delete
                    Log.d("REMOVE All EXCEPT ONE", "delete file");
                    files[i].delete();
                }

            }

        }
    }

    public File[] getFiles(String path) {
        File directory = new File(path);
        File[] files = directory.listFiles();

        int deleted = 0;
        for (int i = 0; i < files.length; i++) {
            if (files[i].getName().contains("_undefined")) {
                //we want to hide undefined -> downloading files.
                files[i] = null;
                deleted++;
            }
        }

        int nSize = files.length - deleted;
        File[] newFiles = new File[nSize];
        if (nSize > 0) {
            System.arraycopy(files, 0, newFiles, 0, newFiles.length);
        }
        //System.out.println(Arrays.toString(newFiles));
        return newFiles;
    }

    public static String getDataDir(Context context) throws Exception {
        return context.getPackageManager()
                .getPackageInfo(context.getPackageName(), 0)
                .applicationInfo.dataDir;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case ID_REQUEST_STORAGE:
            case ID_REQUEST_READ_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    Intent intent = getIntent();
                    finish();
                    startActivity(intent);
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    finish();
                }
                return;
            }
        }
    }


}
