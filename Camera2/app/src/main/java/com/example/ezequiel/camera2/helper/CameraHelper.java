package com.example.ezequiel.camera2.helper;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.Image;
import android.os.Build;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.example.ezequiel.camera2.others.Camera2Source;
import com.example.ezequiel.camera2.others.CameraSource;
import com.example.ezequiel.camera2.others.CameraSourcePreview;
import com.example.ezequiel.camera2.others.GraphicOverlay;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

/**
 * Created by Ashish Das on 20/11/2019.
 */
public class CameraHelper
{
	private final static String TAG = "CameraHelper";

	private Context mContext;

	// CAMERA VERSION ONE DECLARATIONS
	private CameraSource mCameraSource = null;
	// CAMERA VERSION TWO DECLARATIONS
	private Camera2Source mCamera2Source = null;

	// COMMON TO BOTH CAMERAS
	private CameraSourcePreview mCameraPreview;
	private FaceDetector previewFaceDetector = null;
	private GraphicOverlay mGraphicOverlay;

	// DEFAULT CAMERA BEING OPENED
	private boolean usingFrontCamera = true;

	// MUST BE CAREFUL USING THIS VARIABLE.
	// ANY ATTEMPT TO START CAMERA2 ON API < 21 WILL CRASH.
	private boolean useCamera2 = false;
	private boolean wasActivityResumed = false;

	public interface AutoFocusListener
	{
		void onAutoFocus(boolean success);
	}

	public abstract static class OnPictureTakenListener implements CameraSource.ShutterCallback, CameraSource.PictureCallback, Camera2Source.ShutterCallback, Camera2Source.PictureCallback
	{
		public abstract void onShutter();

		public abstract void onPictureTaken(final Image image);

		public abstract void onPictureTaken(final Bitmap pic);
	}

	public interface VideoRecordingStatusListener
	{
		void onStatusChange(final VideoRecordingStatus status, final String videoFile, final Exception error);
	}

	public enum VideoRecordingStatus
	{
		STARTING, RECORDING, STOPPING, STOPPED
	}

	private final FaceDetectionTask mFaceDetectionTask;
	private VideoRecordingStatusListener mVideoRecordingStatusListener;
	private CameraSourcePreview.OnTouchListener mCameraPreviewTouchListener;

	public CameraHelper(final Context context, final CameraSourcePreview cameraPreview, final GraphicOverlay graphicOverlay,
	                    final FaceDetectionTask faceDetectionTask)
	{
		mContext = context;
		mCameraPreview = cameraPreview;
		mGraphicOverlay = graphicOverlay;
		mFaceDetectionTask = faceDetectionTask;

		useCamera2 = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);
		mCameraPreview.setOnTouchListener(mOnCameraPreviewTouchListener);
	}

	public boolean isPermissionGranted()
	{
		try
		{
			GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
			int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(mContext);

			return (resultCode == ConnectionResult.SUCCESS) && (ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
					&& (ContextCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return false;
	}

	public void setCameraPreviewTouchListener(final CameraSourcePreview.OnTouchListener cameraPreviewTouchListener)
	{
		mCameraPreviewTouchListener = cameraPreviewTouchListener;
	}

	public boolean isUsingFrontCamera()
	{
		return usingFrontCamera;
	}

	public void startFrontCamera()
	{
		stopCameraPreview();

		createCameraPreviewFront();
		usingFrontCamera = true;
	}

	public void startBackCamera()
	{
		stopCameraPreview();

		createCameraPreviewBack();
		usingFrontCamera = false;
	}

	public void takePicture(@NonNull OnPictureTakenListener listener)
	{
		if (useCamera2)
		{
			if (mCamera2Source != null)
			{
				mCamera2Source.takePicture(listener, listener);
			}
		}
		else
		{
			if (mCameraSource != null)
			{
				mCameraSource.takePicture(listener, listener);
			}
		}
	}

	public boolean isRecordingVideo()
	{
		return mVideoRecordingHelper.isRecording();
	}

	public void startRecordingVideo(VideoRecordingStatusListener videoRecordingStatusListener)
	{
		mVideoRecordingStatusListener = videoRecordingStatusListener;

		mVideoRecordingHelper.startRecording();
	}

	public void stopRecordingVideo()
	{
		mVideoRecordingHelper.stopRecording();
	}

	public void recordVideo()
	{
		mVideoRecordingHelper.recordVideo();
	}

	public void stopVideo()
	{
		mVideoRecordingHelper.stopVideo();
	}

	public void autoFocus(final AutoFocusListener autoFocusListener, MotionEvent pEvent, int screenW, int screenH)
	{
		if (useCamera2)
		{
			if (mCamera2Source != null)
			{
				mCamera2Source.autoFocus(new Camera2Source.AutoFocusCallback()
				{
					@Override
					public void onAutoFocus(boolean success)
					{
						notifyAutoFocusCallback(autoFocusListener, success);
					}
				}, pEvent, screenW, screenH);
			}
			else
			{
				notifyAutoFocusCallback(autoFocusListener, false);
			}
		}
		else
		{
			if (mCameraSource != null)
			{
				mCameraSource.autoFocus(new CameraSource.AutoFocusCallback()
				{
					@Override
					public void onAutoFocus(boolean success)
					{
						notifyAutoFocusCallback(autoFocusListener, success);
					}
				});
			}
			else
			{
				notifyAutoFocusCallback(autoFocusListener, false);
			}
		}
	}

	private void notifyAutoFocusCallback(AutoFocusListener autoFocusListener, boolean success)
	{
		if (autoFocusListener != null)
		{
			autoFocusListener.onAutoFocus(success);
		}
	}

	private void notifyVideoRecordingStatusCallback(VideoRecordingStatusListener statusCallback, VideoRecordingStatus status, String videoFile, Exception error)
	{
		if (statusCallback != null)
		{
			statusCallback.onStatusChange(status, videoFile, error);
		}
	}

	private final VideoRecordingHelper mVideoRecordingHelper = new VideoRecordingHelper()
	{
		public void recordVideo()
		{
			if (useCamera2)
			{
				if (mCamera2Source != null)
				{
					mCamera2Source.recordVideo(this, this, this);
				}
			}
			else
			{
				if (mCameraSource != null)
				{
					mCameraSource.recordVideo(this, this, this);
				}
			}
		}

		public void stopVideo()
		{
			if (useCamera2)
			{
				if (mCamera2Source != null)
				{
					mCamera2Source.stopVideo();
				}
			}
			else
			{
				if (mCameraSource != null)
				{
					mCameraSource.stopVideo();
				}
			}
		}

		@Override
		void onStatusChange(final VideoRecordingStatus videoRecordingStatus, final String videoFile, final Exception error)
		{
			notifyVideoRecordingStatusCallback(mVideoRecordingStatusListener, videoRecordingStatus, videoFile, error);
		}
	};

	private void createCameraPreviewFront()
	{
		previewFaceDetector = new FaceDetector.Builder(mContext)
				.setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
				.setLandmarkType(FaceDetector.ALL_LANDMARKS)
				.setMode(FaceDetector.FAST_MODE)
				.setProminentFaceOnly(true)
				.setTrackingEnabled(true)
				.build();

		if (previewFaceDetector.isOperational())
		{
			previewFaceDetector.setProcessor(new MultiProcessor.Builder<>(new GraphicFaceTrackerFactory()).build());
		}
		else
		{
			Toast.makeText(mContext, "FACE DETECTION NOT AVAILABLE", Toast.LENGTH_SHORT).show();
		}

		if (useCamera2)
		{
			mCamera2Source = new Camera2Source.Builder(mContext, previewFaceDetector)
					.setFocusMode(Camera2Source.CAMERA_AF_AUTO)
					.setFlashMode(Camera2Source.CAMERA_FLASH_AUTO)
					.setFacing(Camera2Source.CAMERA_FACING_FRONT)
					.build();

			//IF CAMERA2 HARDWARE LEVEL IS LEGACY, CAMERA2 IS NOT NATIVE.
			//WE WILL USE CAMERA1.
			if (mCamera2Source.isCamera2Native())
			{
				startCameraPreview();
			}
			else
			{
				useCamera2 = false;
				if (usingFrontCamera)
				{
					createCameraPreviewFront();
				}
				else
				{
					createCameraPreviewBack();
				}
			}
		}
		else
		{
			mCameraSource = new CameraSource.Builder(mContext, previewFaceDetector)
					.setFacing(CameraSource.CAMERA_FACING_FRONT)
					.setRequestedFps(30.0f)
					.build();

			startCameraPreview();
		}
	}

	private void createCameraPreviewBack()
	{
		previewFaceDetector = new FaceDetector.Builder(mContext)
				.setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
				.setLandmarkType(FaceDetector.ALL_LANDMARKS)
				.setMode(FaceDetector.FAST_MODE)
				.setProminentFaceOnly(true)
				.setTrackingEnabled(true)
				.build();

		if (previewFaceDetector.isOperational())
		{
			previewFaceDetector.setProcessor(new MultiProcessor.Builder<>(new GraphicFaceTrackerFactory()).build());
		}
		else
		{
			Toast.makeText(mContext, "FACE DETECTION NOT AVAILABLE", Toast.LENGTH_SHORT).show();
		}

		if (useCamera2)
		{
			mCamera2Source = new Camera2Source.Builder(mContext, previewFaceDetector)
					.setFocusMode(Camera2Source.CAMERA_AF_AUTO)
					.setFlashMode(Camera2Source.CAMERA_FLASH_AUTO)
					.setFacing(Camera2Source.CAMERA_FACING_BACK)
					.build();

			//IF CAMERA2 HARDWARE LEVEL IS LEGACY, CAMERA2 IS NOT NATIVE.
			//WE WILL USE CAMERA1.
			if (mCamera2Source.isCamera2Native())
			{
				startCameraPreview();
			}
			else
			{
				useCamera2 = false;
				if (usingFrontCamera)
				{
					createCameraPreviewFront();
				}
				else
				{
					createCameraPreviewBack();
				}
			}
		}
		else
		{
			mCameraSource = new CameraSource.Builder(mContext, previewFaceDetector)
					.setFacing(CameraSource.CAMERA_FACING_BACK)
					.setRequestedFps(30.0f)
					.build();

			startCameraPreview();
		}
	}

	private void startCameraPreview()
	{
		if (useCamera2)
		{
			if (mCamera2Source != null)
			{
				try
				{
					mCameraPreview.start(mCamera2Source, mGraphicOverlay);
				}
				catch (IOException e)
				{
					Log.e(TAG, "Unable to start camera source 2.", e);
					mCamera2Source.release();
					mCamera2Source = null;
				}
			}
		}
		else
		{
			if (mCameraSource != null)
			{
				try
				{
					mCameraPreview.start(mCameraSource, mGraphicOverlay);
				}
				catch (IOException e)
				{
					Log.e(TAG, "Unable to start camera source.", e);
					mCameraSource.release();
					mCameraSource = null;
				}
			}
		}
	}

	private void stopCameraPreview()
	{
		if (mCameraPreview != null)
		{
			mCameraPreview.stop();
		}
	}

	private class GraphicFaceTrackerFactory implements MultiProcessor.Factory<Face>
	{
		@Override
		public Tracker<Face> create(Face face)
		{
			return new GraphicFaceTracker(mGraphicOverlay, mFaceDetectionTask);
		}
	}

	private final CameraSourcePreview.OnTouchListener mOnCameraPreviewTouchListener = new CameraSourcePreview.OnTouchListener()
	{
		@Override
		public boolean onTouch(View v, MotionEvent pEvent)
		{
			v.onTouchEvent(pEvent);

			if (mCameraPreviewTouchListener != null)
			{
				mCameraPreviewTouchListener.onTouch(v, pEvent);
			}
			else if (pEvent.getAction() == MotionEvent.ACTION_DOWN)
			{
				autoFocus(null, pEvent, v.getWidth(), v.getHeight());
			}

			return false;
		}
	};

	public void onResume()
	{
		if (wasActivityResumed)
		//If the CAMERA2 is paused then resumed, it won't start again unless creating the whole camera again.
		{
			if (useCamera2)
			{
				if (usingFrontCamera)
				{
					createCameraPreviewFront();
				}
				else
				{
					createCameraPreviewBack();
				}
			}
			else
			{
				startCameraPreview();
			}

			if(mFaceDetectionTask != null)
			{
				mFaceDetectionTask.startThread();
			}
		}
	}

	public void onPause()
	{
		wasActivityResumed = true;

		if (isRecordingVideo())
		{
			stopRecordingVideo();
		}

		stopCameraPreview();

		if(mFaceDetectionTask != null)
		{
			mFaceDetectionTask.stopThread();
		}
	}

	public void onDestroy()
	{
		if (isRecordingVideo())
		{
			stopRecordingVideo();
		}

		if(mFaceDetectionTask != null)
		{
			mFaceDetectionTask.stopThread();
		}

		stopCameraPreview();
		if (previewFaceDetector != null)
		{
			previewFaceDetector.release();
		}
	}
}
