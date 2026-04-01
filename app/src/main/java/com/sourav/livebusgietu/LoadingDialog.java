package com.sourav.livebusgietu;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.view.LayoutInflater;

public class LoadingDialog {
    private final Activity activity;
    private AlertDialog dialog;

    public LoadingDialog(Activity myActivity) {
        this.activity = myActivity;
    }

    @SuppressLint("InflateParams")
    public void startLoadingDiloag() {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;

        if (dialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            LayoutInflater inflater = activity.getLayoutInflater();
            builder.setView(inflater.inflate(R.layout.custom_dialog, null));
            builder.setCancelable(false);
            dialog = builder.create();
        }
        if (!dialog.isShowing()) {
            dialog.show();
        }
    }

    public void dismissDialog() {
        try {
            if (dialog != null && dialog.isShowing()) {
                if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                    dialog.dismiss();
                }
                dialog = null;
            }
        } catch (Exception ignored) {

        }
    }
}
