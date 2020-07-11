package com.devxop.screen80;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

interface AsyncResponse {
    void processFinish(String output);
}

public class DownloadFile extends AsyncTask<String, String, String> {

    private Context context;
    private String fileUrl;
    private String storagePath;
    private String fileName;

    public AsyncResponse delegate = null;//Call back interface


    public DownloadFile(Context context, String fileUrl, String storagePath, String fileName, AsyncResponse asyncResponse) {
        this.context = context;
        this.fileUrl = fileUrl;
        this.storagePath = storagePath;
        this.fileName = fileName;

        delegate = asyncResponse;//Assigning call back interface through constructor
    }

    /**
     * Before starting background thread Show Progress Bar Dialog
     */
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        //showDialog(progress_bar_type);
    }

    /**
     * Downloading file in background thread
     */
    @Override
    protected String doInBackground(String... f_url) {
        //Toast.makeText(getApplicationContext(),"Download video...",Toast.LENGTH_LONG).show();
        int count;

        String checkUrl = this.fileUrl;

        try {
            URL url = new URL(checkUrl);
            URLConnection conection = url.openConnection();
            conection.connect();
            /*TODO
            * VALIDATE CONNECTION TIMEOUT -> crashes if server not available
            * */

            // this will be useful so that you can show a tipical 0-100%
            // progress bar
            int lenghtOfFile = conection.getContentLength();
/*
                if (lenghtOfFile < 1000) {
                    return "";
                }*/

            // download the file
            InputStream input = new BufferedInputStream(url.openStream(),
                    8192);


            // Output stream
            OutputStream output = new FileOutputStream(this.storagePath
                    + "/" + this.fileName + "_undefined");

            Log.d("DOWNLOAD FILE", "LOC: " + this.storagePath
                    + "/" + this.fileName);

            byte data[] = new byte[1024];

            long total = 0;
            int increment = 10;
            while ((count = input.read(data)) != -1) {
                total += count;
                // publishing the progress....
                // After this onProgressUpdate will be called
                publishProgress("" + (int) ((total * 100) / lenghtOfFile));

                final int progress = (int) ((total * 100) / lenghtOfFile);

                if ((progress / increment) > 1) {
                    increment += 10;
                    Log.d("DOWNLOAD FILE", "" + progress);
                }

                        /*myWebView.post(new Runnable() {
                            @Override
                            public void run() {
                                MainActivity.this.myWebView.evaluateJavascript("updateProgress('" + progress + "')", null);
                            }
                        });*/


                // writing data to file
                output.write(data, 0, count);
            }

            // flushing output
            output.flush();

            // closing streams
            output.close();
            input.close();

            /*Intent intent = new Intent();
            intent.setAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
            intent.putExtra("action", "video");
            intent.putExtra("path", this.storagePath);
            intent.putExtra("file","wzWmuGAfMBDwiGqGL.mp4");
            this.context.sendBroadcast(intent);*/

            //StorageManager.Set(getApplicationContext(), "stored_video", checkUrl);


            //Thread.sleep(3000);

            //playVideo();

            File from = new File(storagePath,this.fileName + "_undefined");
            File to = new File(storagePath, this.fileName);
            if(from.exists()){
                Log.d("DOWNLOAD", "Renaming file: " + from.getName());
                from.renameTo(to);
            }


            return "Downloaded file.";

        } catch (Exception e) {
            e.printStackTrace();
            //forceUpdate();
            File from = new File(storagePath,this.fileName + "_undefined");
            if(from.exists()){
                from.delete();
            }

        }


        return "Download file error unknown";
    }

    /**
     * Updating progress bar
     */
    protected void onProgressUpdate(String... progress) {
        // setting progress percentage
        //pDialog.setProgress(Integer.parseInt(progress[0]));
    }

    /**
     * After completing background task Dismiss the progress dialog
     **/
    @Override
    protected void onPostExecute(String aVoid) {
        // dismiss the dialog after the file was downloaded
        //dismissDialog(progress_bar_type);
        super.onPostExecute(aVoid);
        delegate.processFinish(aVoid);
    }

    public static String getDataDir(Context context) throws Exception {
        return context.getPackageManager()
                .getPackageInfo(context.getPackageName(), 0)
                .applicationInfo.dataDir;
    }

}

