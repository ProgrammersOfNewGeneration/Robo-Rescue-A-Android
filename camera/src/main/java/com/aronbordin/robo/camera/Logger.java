package com.aronbordin.robo.camera;

import android.util.Log;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Created by neo on 08/06/14.
 */
public class Logger {
    protected String TAG = "Logger";
    private TextView mTxtLogger = null;
    private ScrollView mScrollLogger = null;

    Logger(TextView txt, ScrollView scroll){
        mTxtLogger = txt;
        mScrollLogger = scroll;
    }

    public void Logar(String txt){
        try {
            Log.d(TAG, txt);

            mTxtLogger.append(txt + "\n");

            mScrollLogger.post(new Runnable() {
                @Override
                public void run() {
                    mScrollLogger.fullScroll(View.FOCUS_DOWN);
                }
            });
        } catch (Exception e){
            Log.e(TAG, "ERROR AO LOGAR!!!!!" + e.getMessage());
        }
    }

    public void LogarErro(String txt){
        try {
            Log.e(TAG, txt);
            mTxtLogger.append(txt + "\n");

            mScrollLogger.post(new Runnable() {
                @Override
                public void run() {
                    mScrollLogger.fullScroll(View.FOCUS_DOWN);
                }
            });
        } catch (Exception e){
            Log.e(TAG, "ERROR AO LOGAR!!!!!" + e.getMessage());
        }
    }
}
