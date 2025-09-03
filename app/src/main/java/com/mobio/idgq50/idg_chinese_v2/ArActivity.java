package com.mobio.idgq50.idg_chinese_v2;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.mobio.idgq50.idg_chinese_v2.utils.MobioApp;
import com.mobio.idgq50.idg_chinese_v2.utils.Values;
import com.mobioapp.androidnativevuforiahelper.native_renderer.ARActivity;
import com.mobioapp.androidnativevuforiahelper.native_renderer.Loaders.Multi3DLoader;
import com.mobioapp.androidnativevuforiahelper.native_renderer.Loaders.ObjLoaderUtility;

import java.io.IOException;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.ACCESS_NETWORK_STATE;
import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class ArActivity extends AppCompatActivity implements View.OnClickListener {

    // Start------------ For permission related constants
    private static final int PERMISSION_REQUEST_CODE_CAMERA = 202;
    private static final int PERMISSION_REQUEST_CODE_STORAGE = 201;
    private static final int PERMISSION_REQUEST_CODE_ACCESS_FINE_LOCATION = 203;
    private static final int PERMISSION_REQUEST_CODE_ALL = 200;
    private ImageView imgViewScan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setListener();
        //load 3d model
        initLoading();
    }

    private void initViews() {
        imgViewScan = findViewById(R.id.img_view_scan);
    }

    private void setListener() {
        imgViewScan.setOnClickListener(this);
    }

    private void permissionAll() {
        if (!checkPermission()) {
            requestPermission();
        }
    }

    public void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{CAMERA,ACCESS_NETWORK_STATE,WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE_ALL);
    }

    private void requestSinglePermission(final String PERMISSION_NAME, final int PERMISSION_CODE) {
        ActivityCompat.requestPermissions(this, new String[]{PERMISSION_NAME}, PERMISSION_CODE);
    }

    public boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), ACCESS_NETWORK_STATE);
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(), CAMERA);
        int result2 = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE);
        return result == PackageManager.PERMISSION_GRANTED&&
                result1 == PackageManager.PERMISSION_GRANTED &&
                result2 == PackageManager.PERMISSION_GRANTED;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {

            case PERMISSION_REQUEST_CODE_ALL:
                if (grantResults.length > 0) {

                    boolean locationAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean cameraAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted = grantResults[2] == PackageManager.PERMISSION_GRANTED;
                    //if(storageAccepted) ((MyApplication)getApplicationContext()).startLoading();
                    if (locationAccepted && cameraAccepted && storageAccepted) {

                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (shouldShowRequestPermissionRationale(ACCESS_FINE_LOCATION)
                                    || shouldShowRequestPermissionRationale(CAMERA)
                                    || shouldShowRequestPermissionRationale(WRITE_EXTERNAL_STORAGE)) {
                                showMessageOKCancel("You need to allow all of the permissions",
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                    requestPermissions(new String[]{ACCESS_FINE_LOCATION, CAMERA, WRITE_EXTERNAL_STORAGE},
                                                            PERMISSION_REQUEST_CODE_ALL);
                                                }
                                            }
                                        });
                                return;
                            }
                        }

                    }
                }
                break;

            /*case PERMISSION_REQUEST_CODE_CAMERA:
                if (grantResults.length > 0) {
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted) {

                    } else {

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (shouldShowRequestPermissionRationale(CAMERA)) {
                                showMessageOKCancel("You need to allow access to CAMERA",
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                    requestSinglePermission(CAMERA, PERMISSION_REQUEST_CODE_CAMERA);
                                                }
                                            }
                                        });
                                return;
                            }
                        }
                    }
                }
                break;*/

            default:
                break;
        }
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(ArActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        Values.carDownloadfromMenu = false;
        super.onBackPressed();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.img_view_scan:
                if (!checkPermission()) {
                    permissionAll();
                } else {
                    // startActivity(new Intent(CarDownloadActivity.this, ImageTargets.class));
                    startActivity(new Intent(this, ImageTargets.class));
                }
                break;

            default:
                break;
        }
    }

    //Load 3d Model using ObjLoader jar
    public void initLoading() {
        Runnable runnable = new Runnable() {
            public void run() {
                try {
                    loadModels(1);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        new Thread(runnable).start();
    }

    private void loadModels(int type) throws IOException {
        ObjLoaderUtility objLoaderUtility = ObjLoaderUtility.getInstance();
        String typeString = type + "";

        try { //Disposes previous instance of Multi3DLoader if required
            if (!objLoaderUtility.getCurrentScope().equals(typeString)) {
                Log.e("Scope", typeString + " " + objLoaderUtility.getCurrentScope());
                objLoaderUtility.getCurrentScopedLoader().dispose();
            }
        } catch (Exception e) {
            Log.e("CurrentScope", "Currently Scoped object was not removed");
        }

        Multi3DLoader loader = objLoaderUtility.generateMulti3DLoader(typeString);
        objLoaderUtility.setRenderScope(typeString);
        // create an instance of Multi3DLoader
        //loader.setScalableModelID(1);
        // select car type

        if (loader.getModelCount() <= 0) {
            addCarData(type, loader);
        } else if (!loader.isAlreadyLoading()) loader.loadAllIfRequired();
    }

    private void addCarData(int type, Multi3DLoader loader) throws IOException {
        switch (type) {
            case 1:
                if (loader.getModelCount() < 5) {
                    loader.dispose();
                    Log.e("Model Loaded","---------  "  + loader.getModelCount());
                    //loader.setScalableModelID(3);
                    loader.addObj(this, getAssets(), "tire.obj", false, "tire.png", false, 0.19202352f, 0.75f, "tayre_01", "tayre_02", "tayre_03","tayre_04","tayre_05","tayre_06","tayre_07","tyre_08","tyre_09","tyre_10","tyre_11");
                    loader.addObj(this, getAssets(), "right_door.obj", false, "right_door.png", false, 0.05433527f, 0.15f, "door_07", "door_08", "door_09","door_10");
                    loader.addObj(this, getAssets(), "left_door.obj", false, "left_door.png", false, 0.05433527f, 0.15f, "door_07", "door_08", "door_09","door_10");
                    loader.addObj(this, getAssets(), "back.obj", false, "back.png", false, 0.21617998f, 0.75f, "back_08", "back_09", "back_10","back_11","back_12","back_13","back_14","back_15","back_16");
                    loader.addObj(this, getAssets(), "front.obj", false, "front.png", false, 0.05433527f, 0.2f, "front_03", "front_04", "front_05","front_02","front_01","front_06","front_07","front_08","front_09","front_10","front_11","front_12","front_13","front_14");
                }
                break;
            case 2:
                break;
            default:
                break;
        }
        loader.loadAllIfRequired();
    }
}
