package com.zajel.exact;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.graphics.Color;
import android.net.Uri;
import android.content.Intent;
import android.view.ViewGroup;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceError;
import android.widget.Toast;
import android.webkit.JavascriptInterface;
import android.os.Environment;
import android.provider.MediaStore;
import android.content.ContentValues;
import android.content.ContentResolver;
import java.io.OutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.pm.PackageManager;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends Activity {
    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    private static final int FILE_CHOOSER_REQUEST = 1001;
    private static final int NOTIFICATION_PERMISSION_REQUEST = 2002;
    private static final String CHANNEL_ID = "zajel_channel";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.rgb(0, 83, 61));
        getWindow().setNavigationBarColor(Color.rgb(0, 83, 61));

        webView = new WebView(this);
        webView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setTextZoom(100);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            settings.setAllowFileAccessFromFileURLs(true);
            settings.setAllowUniversalAccessFromFileURLs(true);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String url = uri.toString();
                if (url.startsWith("file:///android_asset/")) {
                    return false;
                }
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                } catch (Exception ignored) {}
                return true;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> filePath, FileChooserParams params) {
                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                }
                filePathCallback = filePath;
                Intent intent = params.createIntent();
                try {
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST);
                } catch (Exception e) {
                    filePathCallback = null;
                    Toast.makeText(MainActivity.this, "تعذر فتح اختيار الملف", Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
            }
        });

        webView.addJavascriptInterface(new ZajelBridge(), "ZajelAndroid");
        prepareNotifications();

        setContentView(webView);
        webView.loadUrl("file:///android_asset/zajel_bus_app/index.html");
    }


    private void prepareNotifications() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "Zajel Notifications",
                        NotificationManager.IMPORTANCE_DEFAULT
                );
                channel.setDescription("إشعارات تطبيق زاجل للرسائل والتحديثات");
                NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (manager != null) {
                    manager.createNotificationChannel(channel);
                }
            }

            if (Build.VERSION.SDK_INT >= 33) {
                if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void showLocalNotification(String title, String body) {
        try {
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager == null) return;

            if (Build.VERSION.SDK_INT >= 33 &&
                    checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST);
                return;
            }

            Intent launchIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
            if (launchIntent == null) {
                launchIntent = new Intent(this, MainActivity.class);
            }
            launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this,
                    (int) (System.currentTimeMillis() % 100000),
                    launchIntent,
                    flags
            );

            Notification.Builder builder;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder = new Notification.Builder(this, CHANNEL_ID);
            } else {
                builder = new Notification.Builder(this);
            }

            builder.setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title == null || title.trim().isEmpty() ? "زاجل" : title)
                    .setContentText(body == null ? "" : body)
                    .setStyle(new Notification.BigTextStyle().bigText(body == null ? "" : body))
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);

            manager.notify((int) (System.currentTimeMillis() % 100000), builder.build());
        } catch (Exception ignored) {
        }
    }


    public class ZajelBridge {
        @JavascriptInterface
        public void showNotification(final String title, final String body) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showLocalNotification(title, body);
                }
            });
        }

        @JavascriptInterface
        public void setNotificationRole(final String role, final String companyId) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String safeCompany = companyId == null || companyId.trim().isEmpty()
                                ? "zajel_main"
                                : companyId.trim().replaceAll("[^A-Za-z0-9_\\-]", "_");

                        String managerTopic = "zajel_" + safeCompany + "_manager";
                        String employeeTopic = "zajel_" + safeCompany + "_employee";

                        FirebaseMessaging.getInstance().unsubscribeFromTopic(managerTopic);
                        FirebaseMessaging.getInstance().unsubscribeFromTopic(employeeTopic);

                        if ("manager".equals(role)) {
                            FirebaseMessaging.getInstance().subscribeToTopic(managerTopic);
                        } else if ("employee".equals(role)) {
                            FirebaseMessaging.getInstance().subscribeToTopic(employeeTopic);
                        }
                    } catch (Exception ignored) {
                    }
                }
            });
        }

        @JavascriptInterface
        public void saveBackup(final String json, final String filename) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String safeName = filename == null || filename.trim().isEmpty()
                                ? "backup-zajel-bus-invoices.json"
                                : filename.trim();

                        if (!safeName.endsWith(".json")) {
                            safeName = safeName + ".json";
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            ContentValues values = new ContentValues();
                            values.put(MediaStore.Downloads.DISPLAY_NAME, safeName);
                            values.put(MediaStore.Downloads.MIME_TYPE, "application/json");
                            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                            ContentResolver resolver = getContentResolver();
                            Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                            if (uri == null) {
                                throw new Exception("Cannot create download file");
                            }

                            OutputStream outputStream = resolver.openOutputStream(uri);
                            if (outputStream == null) {
                                throw new Exception("Cannot open output stream");
                            }

                            outputStream.write(json.getBytes(StandardCharsets.UTF_8));
                            outputStream.flush();
                            outputStream.close();

                            Toast.makeText(MainActivity.this, "تم حفظ النسخة الاحتياطية في مجلد التنزيلات", Toast.LENGTH_LONG).show();
                        } else {
                            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                            if (!dir.exists()) dir.mkdirs();
                            File file = new File(dir, safeName);
                            FileOutputStream outputStream = new FileOutputStream(file);
                            outputStream.write(json.getBytes(StandardCharsets.UTF_8));
                            outputStream.flush();
                            outputStream.close();

                            Toast.makeText(MainActivity.this, "تم حفظ النسخة الاحتياطية في التنزيلات", Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        try {
                            Intent sendIntent = new Intent(Intent.ACTION_SEND);
                            sendIntent.setType("application/json");
                            sendIntent.putExtra(Intent.EXTRA_SUBJECT, filename);
                            sendIntent.putExtra(Intent.EXTRA_TEXT, json);
                            startActivity(Intent.createChooser(sendIntent, "مشاركة النسخة الاحتياطية"));
                        } catch (Exception ignored) {
                            Toast.makeText(MainActivity.this, "تعذر حفظ النسخة الاحتياطية", Toast.LENGTH_LONG).show();
                        }
                    }
                }
            });
        }
    }


    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_CHOOSER_REQUEST && filePathCallback != null) {
            Uri[] results = null;
            if (resultCode == Activity.RESULT_OK && data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    results = new Uri[]{uri};
                }
            }
            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}
