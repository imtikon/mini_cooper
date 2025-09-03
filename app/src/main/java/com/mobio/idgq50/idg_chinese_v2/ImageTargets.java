package com.mobio.idgq50.idg_chinese_v2;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.mobio.idgq50.idg_chinese_v2.customviews.DialogController;
import com.mobio.idgq50.idg_chinese_v2.utils.Logger;
import com.mobio.idgq50.idg_chinese_v2.utils.MobioApp;
import com.mobio.idgq50.idg_chinese_v2.utils.Values;
import com.mobioapp.androidnativevuforiahelper.native_renderer.ARActivity;
import com.mobioapp.androidnativevuforiahelper.native_renderer.Dependency.LoadingDialogHandler;
import com.mobioapp.androidnativevuforiahelper.native_renderer.Dependency.SampleApplicationException;
import com.mobioapp.androidnativevuforiahelper.native_renderer.Dependency.SampleApplicationGLView;
import com.mobioapp.androidnativevuforiahelper.native_renderer.Dependency.SampleApplicationSession;
import com.mobioapp.androidnativevuforiahelper.native_renderer.Loaders.Multi3DLoader;
import com.mobioapp.androidnativevuforiahelper.native_renderer.Loaders.ObjLoader;
import com.mobioapp.androidnativevuforiahelper.native_renderer.Loaders.ObjLoaderUtility;
import com.mobioapp.androidnativevuforiahelper.native_renderer.Model.Object;
import com.mobioapp.androidnativevuforiahelper.native_renderer.Renderer.ARRenderer;
import com.mobioapp.androidnativevuforiahelper.native_renderer.TouchDetection.ObjectTouchDetector;
import com.vuforia.CameraDevice;
import com.vuforia.DataSet;
import com.vuforia.Frame;
import com.vuforia.Image;
import com.vuforia.ObjectTracker;
import com.vuforia.PIXEL_FORMAT;
import com.vuforia.STORAGE_TYPE;
import com.vuforia.State;
import com.vuforia.Trackable;
import com.vuforia.Tracker;
import com.vuforia.TrackerManager;
import com.vuforia.Vuforia;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class ImageTargets extends ARActivity implements CompoundButton.OnCheckedChangeListener {
    private static final String LOGTAG = "ImageTargets";
    private static int MULTIPLE_DETECTION_COUNT = 3;
    private static int SINGLE_DETECTION_COUNT = 3;
    public boolean automaticFlashDone = false;
    SampleApplicationSession vuforiaAppSession;
    boolean mIsDroidDevice = false;
    private DataSet mCurrentDataset;
    private int mCurrentDatasetSelectionIndex = 0;
    private ArrayList<String> mDatasetStrings = new ArrayList<>();
    private SampleApplicationGLView mGlView;
    private ARRenderer mRenderer;
    private boolean mSwitchDatasetAsap = false;
    private boolean mContAutofocus = true;
    private boolean mExtendedTracking = false;
    private Multi3DLoader loader;
    private RelativeLayout mUILayout;
    private LoadingDialogHandler loadingDialogHandler = new LoadingDialogHandler(this);
    private AlertDialog mErrorDialog;
    private ToggleButton flashSwitch;
    private ImageView ivTargetTop, ivTargetBottom;
    private TextView tvTargetTop, tvTargetBottom, tvMultipleMessage;
    private RelativeLayout rlTargetTop, rlTargetBottom;
    private Typeface tf;
    private Resources resources = null;
    private float average = 0;
    private int iterationCount = 0;
    private boolean pausedAtleastOnce = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(LOGTAG, "onCreate");
        super.onCreate(savedInstanceState);

        vuforiaAppSession = new SampleApplicationSession(this, Values.VUFORIA_KEY); // vuforia initialization
        startLoadingAnimation(); // loading animation

        mDatasetStrings.add("mini_cooper.xml");

        vuforiaAppSession.initAR(this, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // initialize AR
        mIsDroidDevice = android.os.Build.MODEL.toLowerCase().startsWith("droid");
    }

    @Override
    protected void onResume() {
        Log.d(LOGTAG, "onResume");

        if (pausedAtleastOnce) {
            Log.d(LOGTAG, "pausedToResume");

            try { // camera flash light switch status check and make decisions
                if (flashSwitch.isChecked()) {
                    Log.d(LOGTAG, "flashCheck");
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            ObjLoaderUtility.getInstance().flashLightOn(); // light on in multi3Dloader
                        }
                    }, 1000);
                }
            } catch (Exception e) {
                Log.e("InitError", e.toString());
            }
        }
        if (mGlView != null) mGlView.onResume();

        super.onResume();
        showProgressIndicator(true);
        // This is needed for some Droid devices to force portrait
        if (mIsDroidDevice) { // set orientation
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        vuforiaAppSession.onResume(); // continue vuforia session
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
        Log.d(LOGTAG, "onConfigurationChanged");
        super.onConfigurationChanged(config);
        vuforiaAppSession.onConfigurationChanged();
    }

    @Override
    protected void onPause() {
        Log.d(LOGTAG, "onPause");
        super.onPause();

        /*if (mGlView != null) {
            mGlView.setVisibility(View.INVISIBLE);
            mGlView.onPause();
        }*/

        if (mGlView != null) {
            mGlView.onPause();
        }

        try {
            vuforiaAppSession.pauseAR();
        } catch (SampleApplicationException e) {
            Log.e(LOGTAG, e.getString());
        }
        pausedAtleastOnce = true;
    }

    @Override
    protected void onDestroy() {
        Log.d(LOGTAG, "onDestroy");
        super.onDestroy();

        try {
            vuforiaAppSession.stopAR();
        } catch (SampleApplicationException e) {
            Log.e(LOGTAG, e.getString());
        }

    }

    @SuppressLint("ClickableViewAccessibility")
    public void initApplicationAR() {
        int depthSize = 16;
        int stencilSize = 0;
        boolean translucent = Vuforia.requiresAlpha();

        ObjLoaderUtility.getInstance().setSimultenousDitectionCount(MULTIPLE_DETECTION_COUNT); //Sets number of simultenous detection count
        mGlView = new SampleApplicationGLView(this);
        mGlView.init(translucent, depthSize, stencilSize);
        ObjLoader objLoader = null;
        try {
            loader = ObjLoaderUtility.getInstance().getCurrentScopedLoader();
            //loader.getLoader(0).getObjectModel().logTextureCoords();
        } catch (Exception e) {
            e.printStackTrace();
        }

        mRenderer = new ARRenderer(this, vuforiaAppSession, loader); //Creating AR renderer instance
        mGlView.setRenderer(mRenderer);

        //Touch Detection
        mGlView.setOnTouchListener(new ObjectTouchDetector(ImageTargets.this, mGlView, this.loader) {

            @Override
            public void onObjectClicked(int id, Object object, int currentLoaderId, String modelName) {

                if (modelName.contains("combimeter.obj")) {
                    Toast.makeText(getApplicationContext(), modelName.substring(0,modelName.length() - 4), Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    /*int index = 0;
                    Log.e("EpubNum", object.getName().trim());
                    index = Integer.parseInt(object.getName().trim());
                    Intent intent = new Intent(ImageTargets.this, ChapterContent.class);
                    intent.putExtra("epubtype", Values.BUTTON_TYPE);
                    intent.putExtra("text", "Button");
                    intent.putExtra("modelName", modelName);
                    intent.putExtra("index", index); // Index value will be updated according to objects name
                    startActivity(intent);*/
                    Toast.makeText(getApplicationContext(), modelName.substring(0,modelName.length() - 4), Toast.LENGTH_SHORT).show();
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onNoObjectClicked() {
            }
        });

    }

    // loading animation
    protected void startLoadingAnimation() {
        mUILayout = (RelativeLayout) View.inflate(this, R.layout.camera_overlay,
                null);
        mUILayout.setVisibility(View.VISIBLE);
        loadingDialogHandler.mLoadingDialogContainer = mUILayout
                .findViewById(R.id.loading_indicator);

        loadingDialogHandler
                .sendEmptyMessage(LoadingDialogHandler.SHOW_LOADING_DIALOG);
        addContentView(mUILayout, new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));
    }

    @Override
    public boolean doLoadTrackersData() {
        TrackerManager tManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) tManager
                .getTracker(ObjectTracker.getClassType());
        if (objectTracker == null)
            return false;

        if (mCurrentDataset == null)
            mCurrentDataset = objectTracker.createDataSet();

        if (mCurrentDataset == null)
            return false;

        if (!mCurrentDataset.load(
                mDatasetStrings.get(mCurrentDatasetSelectionIndex),
                STORAGE_TYPE.STORAGE_APPRESOURCE))
            return false;

        if (!objectTracker.activateDataSet(mCurrentDataset))
            return false;

        int numTrackables = mCurrentDataset.getNumTrackables();
        for (int count = 0; count < numTrackables; count++) {
            Trackable trackable = mCurrentDataset.getTrackable(count);
            if (isExtendedTrackingActive()) {
                trackable.startExtendedTracking();
            }

            String name = "Current Dataset : " + trackable.getName();
            trackable.setUserData(name);
            Logger.error(LOGTAG, "UserData:Set the following user data " + trackable.getUserData());
        }
        return true;
    }

    @Override
    public boolean doUnloadTrackersData() {
        boolean result = true;
        TrackerManager tManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) tManager
                .getTracker(ObjectTracker.getClassType());
        if (objectTracker == null)
            return false;
        if (mCurrentDataset != null && mCurrentDataset.isActive()) {
            if (objectTracker.getActiveDataSet(0).equals(mCurrentDataset)
                    && !objectTracker.deactivateDataSet(mCurrentDataset)) {
                result = false;
            } else if (!objectTracker.destroyDataSet(mCurrentDataset)) {
                result = false;
            }
            mCurrentDataset = null;
        }
        return result;
    }

    @Override
    public void onVuforiaResumed() {
        if (mGlView != null) {
            mGlView.setVisibility(View.VISIBLE);
            mGlView.onResume();
        }
    }

    @Override
    public void onVuforiaStarted() {
        mRenderer.updateConfiguration();
        if (mContAutofocus) {
            if (!CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO)) {
                if (!CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_TRIGGERAUTO)) {
                    CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_NORMAL);
                }
            }
        }
        showProgressIndicator(false);
    }

    public void showProgressIndicator(boolean show) {
        if (loadingDialogHandler != null) {
            if (show) {
                loadingDialogHandler
                        .sendEmptyMessage(LoadingDialogHandler.SHOW_LOADING_DIALOG);
            } else {
                loadingDialogHandler
                        .sendEmptyMessage(LoadingDialogHandler.HIDE_LOADING_DIALOG);
            }
        }
    }

    @Override
    public void onInitARDone(SampleApplicationException exception) {

        if (exception == null) {

            resources = new Resources(ImageTargets.this.getAssets(), new DisplayMetrics(), MobioApp.getInstance().changeLocalLanguage(ImageTargets.this, "en"));

            initApplicationAR();
            mRenderer.setActive(true);
            setContentView(R.layout.image_targets);
            LinearLayout view = findViewById(R.id.gl_view);
            view.addView(mGlView);
            tvMultipleMessage = findViewById(R.id.ARMessage);
            mUILayout.bringToFront();
            mUILayout.setBackgroundColor(Color.TRANSPARENT);
            vuforiaAppSession.startAR(CameraDevice.CAMERA_DIRECTION.CAMERA_DIRECTION_DEFAULT);
            findViewById(R.id.btReload).setVisibility(ObjLoaderUtility.getInstance().getCurrentScopedLoader().isStaticModeOn() ? View.VISIBLE : View.GONE);
            this.tf = Typeface.createFromAsset(ImageTargets.this.getAssets(), "fonts/InfinitiBrand-Light.ttf");
            flashSwitch = findViewById(R.id.flash);
            flashSwitch.setOnCheckedChangeListener(this);
            flashSwitch.setText(R.string.off);
            ivTargetTop = findViewById(R.id.ivTargetTop);
            tvTargetTop = findViewById(R.id.tvTargetTop);
            ivTargetBottom = findViewById(R.id.ivTargetBottom);
            tvTargetBottom = findViewById(R.id.tvTargetBottom);
            rlTargetBottom = findViewById(R.id.rlTargetBottom);
            rlTargetTop = findViewById(R.id.rlTargetTop);
            ((Button) (findViewById(R.id.btReload))).setTypeface(this.tf);
            tvTargetTop.setTypeface(this.tf);
            tvTargetBottom.setTypeface(this.tf);
            flashSwitch.setTypeface(this.tf);
            tvMultipleMessage.setTypeface(this.tf);

            loader.setStaticModeOn(false);
            findViewById(R.id.btReload).setVisibility(ObjLoaderUtility.getInstance().getCurrentScopedLoader().isStaticModeOn() ? View.VISIBLE : View.GONE);
            tvMultipleMessage.setVisibility(ObjLoaderUtility.getInstance().getCurrentScopedLoader().isStaticModeOn() ? View.GONE : View.VISIBLE);
            rlTargetTop.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (rlTargetBottom.getVisibility() == View.INVISIBLE)
                        rlTargetBottom.setVisibility(View.VISIBLE);
                    else
                        rlTargetBottom.setVisibility(View.INVISIBLE);
                }
            });

            rlTargetBottom.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (rlTargetBottom.getVisibility() == View.VISIBLE)
                        rlTargetBottom.setVisibility(View.INVISIBLE);
                    if (tvTargetTop.getText().equals(resources.getString(R.string.single_target))) {
                        loader.setStaticModeOn(false);
                        tvTargetTop.setText(resources.getString(R.string.multi_target));
                        ivTargetTop.setImageResource(R.drawable.multiple_target);
                        tvTargetBottom.setText(resources.getString(R.string.single_target));
                        ivTargetBottom.setImageResource(R.drawable.single_target);
                    } else {
                        loader.setStaticModeOn(true);
                        tvTargetTop.setText(resources.getString(R.string.single_target));
                        ivTargetTop.setImageResource(R.drawable.single_target);
                        tvTargetBottom.setText(resources.getString(R.string.multi_target));
                        ivTargetBottom.setImageResource(R.drawable.multiple_target);
                    }
                    ARRenderer.getInstance().refresh();
                    findViewById(R.id.btReload).setVisibility(ObjLoaderUtility.getInstance().getCurrentScopedLoader().isStaticModeOn() ? View.VISIBLE : View.GONE);
                    tvMultipleMessage.setVisibility(ObjLoaderUtility.getInstance().getCurrentScopedLoader().isStaticModeOn() ? View.GONE : View.VISIBLE);
                }
            });
        } else {
            Log.e(LOGTAG, exception.getString());
            showInitializationErrorMessage(exception.getString());
        }
    }

    public void showInitializationErrorMessage(String message) {
        final String errorMessage = message;
        runOnUiThread(new Runnable() {
            public void run() {
                if (mErrorDialog != null) {
                    mErrorDialog.dismiss();
                }

                // Generates an Alert Dialog to show the error message
                AlertDialog.Builder builder = new AlertDialog.Builder(
                        ImageTargets.this);
                builder
                        .setMessage(errorMessage)
                        .setTitle(getString(R.string.INIT_ERROR))
                        .setCancelable(false)
                        .setIcon(0)
                        .setPositiveButton(getString(R.string.button_OK),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        finish();
                                    }
                                });

                mErrorDialog = builder.create();
                mErrorDialog.show();
            }
        });
    }

    @Override
    public void onVuforiaUpdate(State state) {
        if (mSwitchDatasetAsap) {
            mSwitchDatasetAsap = false;
            TrackerManager tm = TrackerManager.getInstance();
            ObjectTracker ot = (ObjectTracker) tm.getTracker(ObjectTracker
                    .getClassType());
            if (ot == null || mCurrentDataset == null
                    || ot.getActiveDataSet(0) == null) {
                Log.d(LOGTAG, "Failed to swap datasets");
                return;
            }

            doUnloadTrackersData();
            doLoadTrackersData();
        }
        if (!automaticFlashDone) automaticFlashMode(state);
        else System.gc();
    }

    /**
     * Turns on flash light depending on the camera data
     *
     * @param state Vuforia State
     */
    public void automaticFlashMode(State state) {
        Image imageRGB565 = null;
        Frame frame = state.getFrame();

        for (int i = 0; i < frame.getNumImages(); ++i) {
            Image image = frame.getImage(i);
            if (image.getFormat() == PIXEL_FORMAT.GRAYSCALE) {
                imageRGB565 = image;
                break;
            }
        }


        if (imageRGB565 != null) {
            ByteBuffer pixels = imageRGB565.getPixels();
            byte[] pixelArray = new byte[pixels.remaining()];
            pixels.get(pixelArray, 0, pixelArray.length);
            double totalLuminance = 0;
            for (int p = 0; p < pixelArray.length; p += 4) {
                totalLuminance += pixelArray[p] * 0.299 + pixelArray[p + 1] * 0.587 + pixelArray[p + 2] * 0.114;
            }
            totalLuminance /= (pixelArray.length);
            totalLuminance /= 255.0;

            if (iterationCount <= 20 && totalLuminance > 0) {
                average += totalLuminance;
                iterationCount++;
            } else if (iterationCount > 20) {

                average /= 20;
                Log.d("Average", average + "");
                if (average < 0.01 && average > 0.0) {

                    ObjLoaderUtility.getInstance().flashLightOn();
                    automaticFlashDone = true;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            flashSwitch.setChecked(true);
                        }
                    });
                }
                average = 0;
                iterationCount = 0;
            }

        } else {
            Log.d("ImageCapture", "Negative");
        }
    }

    @Override
    public boolean doInitTrackers() {
        boolean result = true;

        TrackerManager tManager = TrackerManager.getInstance();
        Tracker tracker;

        // Trying to initialize the image tracker
        tracker = tManager.initTracker(ObjectTracker.getClassType());
        if (tracker == null) {
            Log.e(LOGTAG, "Tracker not initialized. Tracker already initialized or the camera is already started");
            result = false;
        } else {
            Log.i(LOGTAG, "Tracker successfully initialized");
        }
        return result;
    }

    @Override
    public boolean doStartTrackers() {
        boolean result = true;
        Tracker objectTracker = TrackerManager.getInstance().getTracker(
                ObjectTracker.getClassType());
        if (objectTracker != null)
            objectTracker.start();
        return result;
    }

    @Override
    public boolean doStopTrackers() {
        boolean result = true;
        Tracker objectTracker = TrackerManager.getInstance().getTracker(
                ObjectTracker.getClassType());
        if (objectTracker != null)
            objectTracker.stop();
        return result;
    }

    @Override
    public boolean doDeinitTrackers() {
        boolean result = true;
        TrackerManager tManager = TrackerManager.getInstance();
        tManager.deinitTracker(ObjectTracker.getClassType());
        return result;
    }

    protected boolean isExtendedTrackingActive() {
        return mExtendedTracking;
    }

    @Override
    public LoadingDialogHandler getLoadingDialogHandler() {
        return loadingDialogHandler;
    }

    public void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    public void refresh(View view) {
        mRenderer.refresh();
    }

    @Override
    public void finish() {
        super.finish();
    }

    public void defaultMode(View view) {
        mRenderer.deafult();
    }

    // Initiate back pressed action
    public void back(View view) {
        backCheck();     // Shows confirmation dialog
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
        if (isChecked) {
            ObjLoaderUtility.getInstance().flashLightOn(); // light ON
            flashSwitch.setCompoundDrawablesWithIntrinsicBounds(R.drawable.flash_on, 0, 0, 0);
            flashSwitch.setTextOn(resources.getString(R.string.on));
        } else {
            ObjLoaderUtility.getInstance().flashLightOff(); // Light OFF
            flashSwitch.setCompoundDrawablesWithIntrinsicBounds(R.drawable.flash_off, 0, 0, 0);
            flashSwitch.setTextOn(resources.getString(R.string.off));
        }
    }

    @Override
    protected void onStop() {
        ObjLoaderUtility.getInstance().flashLightOff(); // Turns off flashlight when the activity is stopped
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        try {
            if (ARRenderer.getInstance().isSingleModeObjectIsDrawing())
                ARRenderer.getInstance().refresh(); // Refreshes the renderer if it is drawing in Single Mode
            else
                backCheck();     // Shows confirmation dialog
        } catch (Exception e) {
            e.printStackTrace();
            backCheck();
        }
    }

    // Shows confirmation dialog
    private void backCheck() {
        final Dialog dialog = new DialogController(ImageTargets.this).langDialog();
        TextView txtViewTitle = dialog.findViewById(R.id.txt_title);
        txtViewTitle.setText(getResources().getString(R.string.exit_alert));

        Button btnYes = dialog.findViewById(R.id.btn_ok); // positive button
        Button btnCancel = dialog.findViewById(R.id.btn_cancel); // negative button
        // positive button click
        btnYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancel();
                finish();
            }
        });
        // negative button click
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }


}
