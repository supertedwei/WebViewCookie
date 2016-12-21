
--- Save Cookies to SD card ---

I havd been tried Android SDK for getting cookies from WebView. Unfortunately the "secure" cookies
could not be read from WebView via Android SDK API. It ends up I found where WebView stores those cookies.
There is a cookies DB located in "/data/data/com.supergigi.webviewcookie/app_webview/Cookies".
What I do to save those cookies is to copy this DB file to SD card and that's it.


--- Load Cookies from SD card --

Read those from SD card DB file, then set cookies to webview via Android SDK API.


--- Remove all Cookies in webview --

Nothing special, just use Android SDK API


--- Note ---
For whole solutions, you can find in MainActivity.java. Others are just helper classes and they are
nothing to do with cookies backup and restore.
