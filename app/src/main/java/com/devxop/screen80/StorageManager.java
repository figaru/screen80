package com.devxop.screen80;

import android.content.Context;


import java.io.FileInputStream;
import java.io.FileOutputStream;

public class StorageManager {

    public static String DEVICE_ID = "device_id";
    public static String AUTH_CODE = "authorization_code";
    public static String UPDATE_VERSION = "version_id";

    public static String PUBLISHED_VIEW = "published_view";

    public static String PUBLISHED_VIDEO = "published_video";

    public static String VIDEOS = "videos_list";
    public static String IMAGES = "images_list";

    public static String IMAGE_INTERVAL = "image_interval";

    public StorageManager(){

    }

    public static void set(Context context, String key, String data){
        try{
            FileOutputStream fOut = context.openFileOutput(key , Context.MODE_PRIVATE);
            fOut.write(data.getBytes());
            fOut.close();
            //Toast.makeText(getBaseContext(),"file saved",Toast.LENGTH_SHORT).show();
        }
        catch (Exception e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
        }
    }

    public static String get(Context context, String key){
        try {
            FileInputStream fin = context.openFileInput(key);
            int c;
            String temp="";
            while( (c = fin.read()) != -1){
                temp = temp + Character.toString((char)c);
            }
            //tv.setText(temp);
            return  temp;
            //Toast.makeText(getBaseContext(),"file read",Toast.LENGTH_SHORT).show();
        }
        catch(Exception e){

        }

        return "";
    }

}