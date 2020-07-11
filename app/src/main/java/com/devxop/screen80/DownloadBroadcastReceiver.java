package com.devxop.screen80;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DownloadBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "DownloadBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        final PendingResult pendingResult = goAsync();
        Task asyncTask = new Task(pendingResult, intent, context);
        asyncTask.execute();
        //Log.d(TAG, "received message");
    }

    private static class Task extends AsyncTask<String, Integer, String> {

        private final PendingResult pendingResult;
        private final Intent intent;
        private final Context context;

        private Task(PendingResult pendingResult, Intent intent, Context context) {
            this.pendingResult = pendingResult;
            this.intent = intent;
            this.context = context;
        }

        @Override
        protected String doInBackground(String... strings) {
            String action = intent.getExtras().getString("action");
            String path = intent.getExtras().getString("path");
            String filename = intent.getExtras().getString("filename");
            List<String> list = new ArrayList<String>();

            JSONArray jsonArray = null;
            try {

                /*if (action.equals("videos")) {
                    jsonArray = new JSONArray(StorageManager.get(this.context, StorageManager.VIDEOS));
                } else if (action.equals("images")) {
                    jsonArray = new JSONArray(StorageManager.get(this.context, StorageManager.IMAGES));
                }
*/
                if (action.equals("images")) {
                    jsonArray = new JSONArray(StorageManager.get(this.context, StorageManager.IMAGES));

                    for (int i = 0; i < jsonArray.length(); i++) {
                        list.add(jsonArray.getString(i));
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
                //do nothing -> Error occurred
                Log.d("FILES", "Error parsing JSON list string. Download Broadcast Receiver");
                return "";
            }

            MainActivity mainActivity = (MainActivity) this.context;
            if (action.equals("videos")) {
                //first check if already playing videos . if not -> play
                StorageManager.set(context, StorageManager.PUBLISHED_VIDEO, filename);
                mainActivity.resetViews();
                mainActivity.setDisplayView("videos");
            } else if (action.equals("images")) {
                //Log.d("BROADCAST", path);
                File directory = new File(path);
                File[] files = mainActivity.getFiles(path);
                if(files != null && list != null){
                    //Log.d("Files", "Size: "+ files.length);
                    int exists = 0;
                    for (int i = 0; i < files.length; i++)
                    {
                        if(files[i] != null && files[i].exists()){
                            //Log.d("Files", "FileName:" + files[i].getName());
                            if(list.contains(files[i].getName())){
                                //Log.d("FILES", "File exists in list -> keep");
                                exists++;
                            }else{
                                //Log.d("FILES", "File not in list -> Remove");
                                files[i].delete();
                            }
                        }

                    }

                    Log.d("RECEIVER", "Exists: "+exists);
                    Log.d("RECEVIVER", "List Size:" + list.size());

                    if(exists == list.size()){
                        //first check if already playing videos . if not -> play
                        mainActivity.setDisplayView("images");
                    }
                }

            }




            return "";
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            // Must call finish() so the BroadcastReceiver can be recycled.
            pendingResult.finish();
        }
    }
}