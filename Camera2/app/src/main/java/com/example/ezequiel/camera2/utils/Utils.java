package com.example.ezequiel.camera2.utils;

/**
 * Created by Ezequiel Adrian on 24/02/2017.
 */

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.PointF;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.images.Size;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Utils
{
	private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());

	public static int dpToPx(int dp)
	{
		return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
	}

	public static int getScreenHeight(Context c)
	{
		WindowManager wm = (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		return size.y;
	}

	public static int getScreenWidth(Context c)
	{
		WindowManager wm = (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		return size.x;
	}

	public static float getScreenRatio(Context c)
	{
		DisplayMetrics metrics = c.getResources().getDisplayMetrics();
		return ((float) metrics.heightPixels / (float) metrics.widthPixels);
	}

	public static int getScreenRotation(Context c)
	{
		WindowManager wm = (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
		return wm.getDefaultDisplay().getRotation();
	}

	public static int distancePointsF(PointF p1, PointF p2)
	{
		return (int) Math.sqrt((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y) * (p1.y - p2.y));
	}

	public static PointF middlePoint(PointF p1, PointF p2)
	{
		if (p1 == null || p2 == null)
		{
			return null;
		}
		return new PointF((p1.x + p2.x) / 2, (p1.y + p2.y) / 2);
	}

	public static Size[] sizeToSize(android.util.Size[] sizes)
	{
		Size[] size = new Size[sizes.length];
		for (int i = 0; i < sizes.length; i++)
		{
			size[i] = new Size(sizes[i].getWidth(), sizes[i].getHeight());
		}
		return size;
	}

	public static boolean checkGooglePlayAvailability(final Activity activity)
	{
		GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
		int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(activity);
		if (resultCode == ConnectionResult.SUCCESS)
		{
			return true;
		}
		else
		{
			if (googleApiAvailability.isUserResolvableError(resultCode))
			{
				googleApiAvailability.getErrorDialog(activity, resultCode, 2404).show();
			}
		}
		return false;
	}

	public static String getNewImageFilePath()
	{
		File folder = new File(Environment.getExternalStorageDirectory(), "Camera2/images");
		if ((folder.exists() && folder.isDirectory()) || folder.mkdirs())
		{
			return folder.getAbsolutePath() + "/" + formatter.format(new Date()) + ".png";
		}

		return Environment.getExternalStorageDirectory() + "/" + formatter.format(new Date()) + ".png";
	}

	public static String getNewVideoFilePath()
	{
		File folder = new File(Environment.getExternalStorageDirectory(), "Camera2/videos");
		if ((folder.exists() && folder.isDirectory()) || folder.mkdirs())
		{
			return folder.getAbsolutePath() + "/" + formatter.format(new Date()) + ".mp4";
		}

		return Environment.getExternalStorageDirectory() + "/" + formatter.format(new Date()) + ".mp4";
	}
}
