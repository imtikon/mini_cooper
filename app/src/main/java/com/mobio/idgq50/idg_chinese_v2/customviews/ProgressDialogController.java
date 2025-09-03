package com.mobio.idgq50.idg_chinese_v2.customviews;

import android.app.Activity;
import android.app.ProgressDialog;

import com.mobio.idgq50.idg_chinese_v2.R;

/**
 * Created by rohan on 3/22/17.
 */

public class ProgressDialogController {

    private Activity activity;
    private ProgressDialog progressDialog;

    public ProgressDialogController(Activity activity) {
        this.activity = activity;
    }

    public ProgressDialog downloadProgress(String msg) {
        progressDialog = new ProgressDialog(activity);
        progressDialog.setTitle(activity.getResources().getString(R.string.alert_status));
        progressDialog.setMessage(msg + 0.0 + "%");
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.show();
        return progressDialog;
    }

    public ProgressDialog showDialog(String msg) {
        progressDialog = new ProgressDialog(activity);
        progressDialog.setMessage(msg);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.show();
        return progressDialog;
    }

}
