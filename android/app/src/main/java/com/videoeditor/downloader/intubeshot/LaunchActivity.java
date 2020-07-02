package com.videoeditor.downloader.intubeshot;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.videoeditor.downloader.intubeshot.base.BaseActivity;

public class LaunchActivity extends BaseActivity {

    private final int REQUEST_PERMISSION = 456;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean hasPermission = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
            } else {
                hasPermission = true;
            }
        } else {
            hasPermission = true;
        }
        if (hasPermission) {
//            startActivity(new Intent(this, TestCmdActivity.class));
            startVideoEditActivity();
        } else {
            setContentView(R.layout.activity_launch);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        int perIndex = 0;
        if (requestCode == REQUEST_PERMISSION) {
            boolean hasPermission = true;
            for (int result : grantResults) {
                if (result == PackageManager.PERMISSION_DENIED) {
                    hasPermission = false;
                    break;
                }
                perIndex++;
            }
            if (hasPermission) {
                startVideoEditActivity();
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    boolean b = shouldShowRequestPermissionRationale(perIndex == 0 ? Manifest.permission.READ_EXTERNAL_STORAGE :
                            Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    String msg;
                    if (b) {
                        msg = getString(R.string.ve_show_request_permission);
                    } else {
                        msg = getString(R.string.ve_show_request_never_permission);
                    }
                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.FDeleteMaterialAlertDialog);
                    builder.setTitle(R.string.ve_warning);
                    builder.setMessage(msg);
                    builder.setPositiveButton(R.string.ve_quit, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    });
                    builder.setCancelable(false);
                    builder.show();
                }
            }
        }
    }

    private void startVideoEditActivity() {
        startActivity(new Intent(this, SelectVideoActivity.class));
        finish();
    }
}
