package com.flowerplus.jcprint;

import android.content.Context;

/**
 * Created by flowerplus-sdl on 2018/1/8.
 */

public class JCPrint {
    private static Context mContext;

    public static void install(Context context) {
        mContext = context.getApplicationContext();
    }

    public static Context getContext() {
        return mContext;
    }
}
