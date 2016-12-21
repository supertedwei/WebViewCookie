package com.supergigi.webviewcookie;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by tedwei on 10/7/16.
 */

public class LoggerTask extends AsyncTask<Void, Void, Void> {

    private static final String LOG_TAG = LoggerTask.class.getSimpleName();

    private File rootDir;
    private File loggerDir;
    private String appVersion;
    private Context context;

    public LoggerTask(Context context) {
        this.context = context;
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            appVersion = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException ex) {
            // do nothing
        }
    }

    @Override
    protected Void doInBackground(Void... voids) {
        String extDir = Environment.getExternalStorageDirectory().getPath();
        rootDir = new File(extDir, "/SuperGigiLogger");
        rootDir.mkdirs();
        loggerDir = new File(rootDir, "/" + System.currentTimeMillis());
        loggerDir.mkdirs();
        if (!loggerDir.exists()) {
            Log.d(LOG_TAG, "loggerDir doesn't exist");
            return null;
        }

        try {
            startLogcat();
            startOutputInfo();
            startCopyPackageFolders();
            startZipFiles();
        } catch (Exception ex) {
            Log.e(LOG_TAG, "", ex);
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);

        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            if (!activity.isFinishing()) {
                Toast.makeText(activity, "Logger Task finished!", Toast.LENGTH_SHORT).show();
            }
        }
        Log.d(LOG_TAG, "Logger Task finished!");
    }

    protected void startZipFiles() {
        File zipDir = rootDir;
        File zipFile = new File(zipDir, "log_" + System.currentTimeMillis() + ".zip");
        Log.d(LOG_TAG, "compressing files to : " + zipFile.getAbsolutePath());
        compress2Zip(zipFile, loggerDir, rootDir);
    }

    protected void startCopyDataFolder(Context context, PackageInfo packageInfo) {
        final String dataDir = packageInfo.applicationInfo.dataDir;
        File srcDir = new File(dataDir);
        File destDir = new File(loggerDir, context.getPackageName());
        Log.d(LOG_TAG, "Copy Data folder (from) : " + srcDir.getAbsolutePath());
        Log.d(LOG_TAG, "Copy Data folder (to)   : " + destDir.getAbsolutePath());
        try {
            FileUtils.copyDirectory(srcDir, destDir);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    protected void startCopyPackageFolders() {
        PackageManager pm = context.getPackageManager();
        String[] packageNames = pm.getPackagesForUid(android.os.Process.myUid());
        for (String packageName : packageNames) {
            Log.d(LOG_TAG, "Found package : " + packageName);
            Context targetContext;
            try {
                targetContext = context.createPackageContext(packageName, 0);
                PackageInfo packageInfo = pm.getPackageInfo(packageName, 0);
                startCopyDataFolder(targetContext, packageInfo);
            } catch (PackageManager.NameNotFoundException e) {
                Log.d(LOG_TAG, "Shall not happen : " + e.getMessage());
            }
        }
    }

    protected void startOutputInfo() throws IOException {
        Log.d(LOG_TAG, "outputing info ...");
        Runtime runtime = Runtime.getRuntime();
        //
        {
            String filename = new File(loggerDir, "info.txt").getAbsolutePath();
            Log.d(LOG_TAG, "writing : " + filename);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename)));
            writer.write("appVersion = [" + appVersion + "]");
            writer.newLine();
            writer.write("clientHwId = [" + Utils.getClientHwId() + "]");
            writer.newLine();
            writer.write("clientOs = [" + Utils.getClientOS() + "]");
            writer.newLine();
            writer.write("clientModel = [" + Utils.getClientModel() + "]");
            writer.newLine();
            writer.close();
        }
    }

    protected void startLogcat() throws IOException {
        Runtime runtime = Runtime.getRuntime();
        //
        {
            String filename = new File(loggerDir, "logcat_main_" + System.currentTimeMillis() + ".txt").getAbsolutePath();
            Log.d(LOG_TAG, "writing : " + filename);
            Process process = runtime.exec("/system/bin/logcat -d -v time -b main -f " + filename);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                // just consume input stream
                Log.d(LOG_TAG, "runtime.exec : " + line);
            }
        }
        //
        {
            String filename = new File(loggerDir, "logcat_events_" + System.currentTimeMillis() + ".txt").getAbsolutePath();
            Log.d(LOG_TAG, "writing : " + filename);
            Process process = runtime.exec("/system/bin/logcat -d -v time -b events -f " + filename);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                // just consume input stream
                Log.d(LOG_TAG, "runtime.exec : " + line);
            }
        }
    }

    public static void compress2Zip(File zipFile, File srcDir, File rootDir) {
        try {
            String strRootDir = rootDir.getAbsolutePath();
            FileOutputStream fos = new FileOutputStream(zipFile);
            ZipOutputStream zos = new ZipOutputStream(fos);
            addDirToArchive(zos, srcDir, strRootDir);
            // close the ZipOutputStream
            zos.close();
        } catch (IOException ioe) {
            Log.e(LOG_TAG, "", ioe);
        }

    }

    private static void addDirToArchive(ZipOutputStream zos, File srcFile, String strRootDir) {
        File[] files = srcFile.listFiles();
        for (int i = 0; i < files.length; i++) {
            // if the file is directory, use recursion
            if (files[i].isDirectory()) {
                addDirToArchive(zos, files[i], strRootDir);
                continue;
            }

            try {
                String filename = files[i].getAbsolutePath();
                filename = filename.replace(strRootDir, "");
                Log.d(LOG_TAG, "Adding file: " + filename);
                // create byte buffer
                byte[] buffer = new byte[1024];
                FileInputStream fis = new FileInputStream(files[i]);
                zos.putNextEntry(new ZipEntry(filename));
                int length;
                while ((length = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, length);
                }
                zos.closeEntry();
                // close the InputStream
                fis.close();
            } catch (IOException ioe) {
                Log.e(LOG_TAG, "", ioe);
            }
        }
    }

    public static class Utils {

        public static String getClientOS() {
            return "Android " + Build.VERSION.RELEASE;
        }

        public static String getClientModel() {
            return Build.MANUFACTURER + " " + Build.MODEL;
        }

        public static String getClientHwId() {
            return Build.SERIAL;
        }
    }

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    /**
     * Checks if the app has permission to write to device storage
     *
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }
}
