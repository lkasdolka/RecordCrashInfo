# RecordCrashInfo
A tool to record android app crash infomation on sd card. Default diretory keeping error info files is the *crash* folder under your sd card directory.

## Initialization

Init the CrashHandler in **onCreate()** method in android.app.Application's subclass:
```
  CrashHandler.getInstance().init(getApplicationContext());
```

## Set Directory
The *default* error log storage diectory is 
```
Environment.getExternalStorageDirectory()+File.seperator+"crash"
```

Customize error log directory in **onCreate()** in android.app.Application's subclass:
```
  CrashHandler.getInstance().setCrashLogDirectory("myCrashLog");
```

## Add permission
Don't forget to add 
```
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
```

