package com.example.moonh.inbody_scale;

/**
 * Created by moonh on 2017-05-12.
 */
import android.util.Log;

public class LogUtil {
    static boolean DEBUG = true;
    public static void i(String str){
        if(DEBUG)
            Log.i("IIIIIIIII", str);
    }
    public static void e(String str){
        if(DEBUG)
            Log.e("EEEEEEEE", str);
    }
}
