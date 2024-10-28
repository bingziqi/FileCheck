package com.sowings.filecheck;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.progressindicator.LinearProgressIndicator;

public class CustomLoadingDialog {

    private AlertDialog dialog;
    private LinearProgressIndicator progressBar;
    private TextView title;
    private TextView fileName;

    public CustomLoadingDialog(Context context) {
        // 加载自定义布局
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_loading, null);
        progressBar = view.findViewById(R.id.progress_bar);
        title = view.findViewById(R.id.title);
        fileName = view.findViewById(R.id.file_name);

        // 创建 AlertDialog
        dialog = new AlertDialog.Builder(context).setView(view).setCancelable(false)  // 禁止手动取消
                .create();
    }

    public void show(int max) {
        progressBar.setMax(max);
        progressBar.setProgress(0);
        dialog.show();
    }
    public void show() {
        dialog.show();
    }

    public void updateProgress(int progress) {
        progressBar.setProgress(progress);
    }

    public void setTile(String titleName) {
        title.setText(titleName);
    }

    public void setFileName(String file) {
        if (null != file) {
            fileName.setVisibility(View.VISIBLE);
            fileName.setText(file);
        } else {
            fileName.setVisibility(View.GONE);
        }
    }

    public void dismiss() {
        if (dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    public void loadFiles() {
        progressBar.setIndeterminate(true);
        setTile("加载文件中...");
        setFileName(null);
        dialog.show();
    }
    public void checkFiles() {
        progressBar.setVisibility(View.INVISIBLE);
        progressBar.setIndeterminate(false);
        progressBar.setVisibility(View.VISIBLE);
    }
}

