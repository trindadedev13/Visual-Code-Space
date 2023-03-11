package com.raredev.vcspace.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.raredev.common.task.TaskExecutor;
import com.raredev.common.util.DialogUtils;
import com.raredev.common.util.FileUtil;
import com.raredev.vcspace.R;
import com.raredev.vcspace.VCSpaceApplication;
import com.raredev.vcspace.databinding.DialogInputBinding;
import java.io.File;
import java.util.Comparator;

public class FileManagerUtils {

  public static final Comparator<File> COMPARATOR =
      (file1, file2) -> {
        if (file1.isFile() && file2.isDirectory()) {
          return 1;
        } else if (file2.isFile() && file1.isDirectory()) {
          return -1;
        } else {
          return String.CASE_INSENSITIVE_ORDER.compare(file1.getName(), file2.getName());
        }
      };

  public static boolean isPermissionGaranted(Context context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      return Environment.isExternalStorageManager();
    } else {
      return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
          == PackageManager.PERMISSION_GRANTED;
    }
  }

  public static boolean isValidTextFile(String filename) {
    return !filename.matches(
        ".*\\.(bin|ttf|png|jpe?g|bmp|mp4|mp3|m4a|iso|so|zip|jar|dex|odex|vdex|7z|apk|apks|xapk)$");
  }

  public static void createFile(Activity act, File file, Concluded concluded) {
    createNew(act, file, concluded, false);
  }

  public static void createFolder(Activity act, File file, Concluded concluded) {
    createNew(act, file, concluded, true);
  }

  @SuppressWarnings("deprecation")
  private static void createNew(Activity act, File file, Concluded concluded, boolean isFolder) {
    LayoutInflater inflater = act.getLayoutInflater();
    DialogInputBinding binding = DialogInputBinding.inflate(inflater);
    EditText et_filename = binding.etInput;
    binding.tvInputLayout.setHint(
        isFolder
            ? act.getString(R.string.folder_name_hint)
            : act.getString(R.string.file_name_hint));

    new MaterialAlertDialogBuilder(act)
        .setTitle(isFolder ? R.string.new_folder_title : R.string.new_file_title)
        .setPositiveButton(
            R.string.create,
            (dlg, i) -> {
              if (isFolder) {
                File newFolder = new File(file, "/" + et_filename.getText().toString());
                if (!newFolder.exists()) {
                  if (newFolder.mkdirs()) {
                    concluded.concluded();
                  }
                }
              } else {
                File newFile = new File(file, "/" + et_filename.getText().toString());
                if (!newFile.exists()) {
                  if (FileUtil.writeFile(newFile.getAbsolutePath(), "")) {
                    concluded.concluded();
                  }
                }
              }
            })
        .setNegativeButton(R.string.cancel, null)
        .setView(binding.getRoot())
        .show();
  }

  public static void renameFile(Activity act, File file, OnFileRenamed onFileRenamed) {
    LayoutInflater inflater = act.getLayoutInflater();
    View v = inflater.inflate(R.layout.dialog_input, null);
    EditText et_filename = v.findViewById(R.id.et_input);
    et_filename.setText(file.getName());

    new MaterialAlertDialogBuilder(act)
        .setTitle(R.string.menu_rename)
        .setPositiveButton(
            R.string.menu_rename,
            (dlg, i) -> {
              File newFile = new File(file.getAbsolutePath());

              if (newFile.exists()) {
                if (newFile.renameTo(
                    new File(file.getParentFile(), et_filename.getText().toString()))) {
                  onFileRenamed.onFileRenamed(file, newFile);
                }
              }
            })
        .setNegativeButton(R.string.cancel, null)
        .setView(v)
        .show();
  }

  public static void deleteFile(Activity act, File file, Concluded concluded) {
    new MaterialAlertDialogBuilder(act)
        .setTitle(R.string.delete)
        .setMessage(
            act.getResources().getString(R.string.delete_message).replace("NAME", file.getName()))
        .setPositiveButton(
            R.string.delete,
            (dlg, i) -> {
              AlertDialog progress =
                  DialogUtils.newProgressDialog(
                          act,
                          act.getString(R.string.deleting),
                          act.getString(R.string.deleting_plase_wait))
                      .create();
              progress.setCancelable(false);
              progress.show();
              TaskExecutor.executeAsyncProvideError(
                  () -> {
                    return FileUtil.delete(file.getAbsolutePath());
                  },
                  (result, error) -> {
                    concluded.concluded();
                    progress.cancel();
                  });
            })
        .setNegativeButton(R.string.cancel, null)
        .show();
  }

  public static void takeFilePermissions(Activity activity) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      Intent intent = new Intent();
      intent.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
      Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
      intent.setData(uri);
      activity.startActivity(intent);
    } else {
      ActivityCompat.requestPermissions(
          activity,
          new String[] {
            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.MANAGE_EXTERNAL_STORAGE
          },
          1);
    }
  }

  public interface OnFileRenamed {
    void onFileRenamed(File oldFile, File newFile);
  }

  public interface Concluded {
    void concluded();
  }
}
