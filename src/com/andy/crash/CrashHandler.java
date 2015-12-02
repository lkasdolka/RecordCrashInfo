/**
 * 
 */
package com.andy.crash;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Environment;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;
import android.widget.SimpleAdapter;

/**
 * @author andy
 *
 */
public class CrashHandler implements UncaughtExceptionHandler {

	private static final String TAG = CrashHandler.class.getSimpleName();

	private static final String LOG_DIR = "crashlog";

	private static String USER_SET_LOG_DIR = null;

	// singleton
	private static CrashHandler INSTANCE = new CrashHandler();

	private Context mContext;

	private UncaughtExceptionHandler mDefaultHandler;

	// store device info and exception info
	private Map<String, String> info = new HashMap<String, String>();

	private CrashHandler() {
	}

	public static CrashHandler getInstance() {
		return INSTANCE;
	}

	public void init(Context context) {
		mContext = context;
		mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
		Thread.setDefaultUncaughtExceptionHandler(this);
	}

	public void setCrashLogDirectory(String name) {
		USER_SET_LOG_DIR = name;
	}

	/**
	 * When exception uncaught occurs, triggers this method
	 */
	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		Log.d("", "--> invoke uncaught exception handler");
		if(!handleException(ex) && mDefaultHandler!=null){
			mDefaultHandler.uncaughtException(thread, ex);
		}else{
			try {
				// sleep 3 seconds , then exit , in between this, you can upload error info onto server
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			android.os.Process.killProcess(Process.myPid());
			System.exit(1);
		}
	}

	public boolean handleException( Throwable ex) {
		Log.d("", "-->invoke handle exception");
		if (ex == null) {
			return false;
		}
		collectDeviceInfo(mContext);
		saveCrashInfoToDisk(ex);
		return true;
	}

	private void collectDeviceInfo(Context context) {

		try {
			PackageManager pm = context.getPackageManager();
			PackageInfo pi = pm.getPackageInfo(context.getPackageName(),
					PackageManager.GET_ACTIVITIES);
			if (pi != null) {
				String versionName = pi.versionName == null ? "null"
						: pi.versionName;
				String versionCode = pi.versionCode + "";
				info.put("versionName", versionName);
				info.put("versionCode", versionCode);
			}
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}

		// use reflection to get fields
		Field[] fields = Build.class.getDeclaredFields();

		for (Field field : fields) {
			try {
				field.setAccessible(true);
				info.put(field.getName(), field.get("").toString());
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			}
		}

	}

	private void saveCrashInfoToDisk(Throwable ex) {

		if (ex == null)
			return;

		StringBuilder builder = new StringBuilder();
		for (Map.Entry<String, String> entry : info.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			builder.append(key + "=" + value + "\n");
		}

		Writer writer = new StringWriter();
		PrintWriter printWriter = new PrintWriter(writer);
		ex.printStackTrace(printWriter);

		Throwable cause = ex.getCause();
		while (cause != null) {
			cause.printStackTrace(printWriter);
			cause = cause.getCause();
		}

		printWriter.close();
		String result = writer.toString();
		builder.append(result);

		// save to file
		long timestamp = System.currentTimeMillis();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
		String formatTime = formatter.format(new Date());
		String fileName = "crash_" + formatTime + ".log";

		String dirPath = Environment.getExternalStorageDirectory()
				+ File.separator
				+ (TextUtils.isEmpty(USER_SET_LOG_DIR) ? LOG_DIR
						: USER_SET_LOG_DIR);
		
		Log.d("","-->dirpath:"+dirPath+",filename:"+fileName);

		File dirPathFile = new File(dirPath);
		if (!dirPathFile.exists()) {
			Log.d("","-->dir not exist;");
			boolean res = dirPathFile.mkdirs();
			Log.d("", "--> create dir:"+res);
		}

		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(new File(dirPathFile,
					fileName));
			fos.write(builder.toString().getBytes());
			fos.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
