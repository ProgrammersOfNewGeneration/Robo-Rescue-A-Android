package com.aronbordin.robo.camera;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.FrameLayout;

public class MainActivity extends Activity {
    private Camera mCamera = null;
    private CameraPreview mPreview;
    private FrameLayout mFrameCamera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createCamera();
        if(mCamera != null) {
            mPreview = new CameraPreview(this, mCamera, this);
            mFrameCamera = (FrameLayout) findViewById(R.id.frameCamera);
            mFrameCamera.addView(mPreview);
        }

    }

    protected void createCamera(){
        try{
            mCamera = Camera.open(0);
            mCamera.setDisplayOrientation(90);
        } catch (Exception e){
            Log.e("ERRO", "Erro ao abrir camera!");
            mCamera = null;
        }
    }
}
