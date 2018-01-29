package com.flowerplus.jcprint;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by flowerplus-sdl on 2018/1/8.
 */

public class SPUtil {
    private SPUtil() {}

    public static void putString(String key, String value) {
        SharedPreferences sp = JCPrint.getContext().getSharedPreferences("JCPrint", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();

        editor.putString(key, value);

        editor.commit();
    }

    public static String getString(String key) {
        SharedPreferences sp = JCPrint.getContext().getSharedPreferences("JCPrint", Context.MODE_PRIVATE);
        return sp.getString(key, "");
    }
}
