package com.supergigi.webviewcookie;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    String url;
    EditText urlView;
    WebView webview;

    private String contextPath;
    private String originalCookiePath;
    private String myCookieDbPath;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        CookieManager.setAcceptFileSchemeCookies(true);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        String extDir = Environment.getExternalStorageDirectory().getPath();

        myCookieDbPath = extDir + "/MyCookies";
        contextPath = "/data/data/" + getApplicationContext().getPackageName() + "/";
        originalCookiePath = contextPath + "app_webview/Cookies";

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        urlView = (EditText) findViewById(R.id.url);
        findViewById(R.id.load).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                url = urlView.getText().toString();
                webview.loadUrl(url);
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE))
            { WebView.setWebContentsDebuggingEnabled(true); }
        }

        CookieManager.getInstance().setAcceptCookie(true);
        webview = (WebView) findViewById(R.id.webview);
        webview.getSettings().setJavaScriptEnabled(true);
        webview.getSettings().setDomStorageEnabled(true);

        webview.setWebViewClient(new MyWebViewClient());

        //url = "https://www.facebook.com";
        //url = "https://bitbucket.org";
        url = "http://assembla.com/";
        webview.loadUrl(url);
        urlView.setText(url);

        verifyStoragePermissions(this);

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_load_cookies) {
            CookieManager cookieManager = CookieManager.getInstance();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                cookieManager.removeAllCookies(null);
            } else {
                cookieManager.removeAllCookie();
            }

            String path = myCookieDbPath;
            File pathFile = new File(path);
            if (!pathFile.exists()) {
                Toast.makeText(this, "MyCookie db file doesn't exist", Toast.LENGTH_SHORT).show();
                return true;
            }
            SQLiteDatabase db = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READWRITE);
            Cursor cursor = db.query("cookies", null, null, null, null, null, null);


            cookieManager.setAcceptCookie(true);
            while (cursor.moveToNext()) {
                String name = cursor.getString(cursor.getColumnIndex("name"));
                String value = cursor.getString(cursor.getColumnIndex("value"));
                String domain = cursor.getString(cursor.getColumnIndex("host_key"));
                String secure = cursor.getString(cursor.getColumnIndex("secure"));


                String cookie = name + "=" + value + "; domain=" + domain + "; ";
                if ("1".equals(secure)) {
                    cookie += " secure ";
                }
                Log.d("Test", "cookie : " + cookie);
                cookieManager.setCookie(url, cookie);
            }

            Toast.makeText(this, "All cookies has been loaded to web view", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_save_cookies) {

            InputStream myinput = null;
            try {
                myinput = new FileInputStream(originalCookiePath);

                //Open the empty db as the output stream
                OutputStream myoutput = new FileOutputStream(myCookieDbPath);

                // transfer byte to inputfile to outputfile
                byte[] buffer = new byte[1024];
                int length;
                while ((length = myinput.read(buffer))>0) {
                    myoutput.write(buffer,0,length);
                }

                //Close the streams
                myoutput.flush();
                myoutput.close();
                myinput.close();

                Toast.makeText(this, "All cookies has been stored to files", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e("Test", "", e);
            }
            return true;
        } else if (id == R.id.action_remove_cookies) {
            CookieManager cookieManager = CookieManager.getInstance();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                cookieManager.removeAllCookies(null);
            }
            else {
                cookieManager.removeAllCookie();
            }
            String cookies = CookieManager.getInstance().getCookie(url);
            Log.d("Test", "remove All the cookies in a string:" + cookies);
            Log.d("Test", "url:" + url);
            Toast.makeText(this, "All cookies has been removed", Toast.LENGTH_SHORT).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.run_logger) {
            new LoggerTask(this).execute();
        } else if (id == R.id.about) {
            // Handle the camera action
            PackageManager manager = this.getPackageManager();
            try {
                PackageInfo info = manager.getPackageInfo(this.getPackageName(), 0);
                String appVersion = info.versionName;
                Toast.makeText(this, "Version : " + appVersion, Toast.LENGTH_SHORT).show();
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private class MyWebViewClient extends WebViewClient {

        @SuppressWarnings("deprecation")
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return handleUri(view, url);
        }

        @TargetApi(Build.VERSION_CODES.N)
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            return handleUri(view, url);
        }

        private boolean handleUri(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }
    }
}
