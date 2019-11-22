package com.example.ezequiel.camera2;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ezequiel.camera2.helper.CameraHelper;
import com.example.ezequiel.camera2.helper.FaceDetectionTask;
import com.example.ezequiel.camera2.others.Camera2Source;
import com.example.ezequiel.camera2.others.CameraSource;
import com.example.ezequiel.camera2.others.CameraSourcePreview;
import com.example.ezequiel.camera2.others.GraphicOverlay;
import com.example.ezequiel.camera2.utils.Utils;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Created by Ashish Das on 20/11/2019.
 */
public class Test2Activity extends AppCompatActivity
{

	private static final String TAG = "Test2";
	private Context context;
	private static final int REQUEST_CAMERA_PERMISSION = 200;
	private static final int REQUEST_STORAGE_PERMISSION = 201;
	private TextView cameraVersion;
	private ImageView ivAutoFocus;

	// CAMERA VERSION ONE DECLARATIONS
	private CameraSource mCameraSource = null;

	// CAMERA VERSION TWO DECLARATIONS
	private Camera2Source mCamera2Source = null;

	// COMMON TO BOTH CAMERAS
	private CameraSourcePreview mPreview;
	private FaceDetector previewFaceDetector = null;
	private GraphicOverlay mGraphicOverlay;
	private boolean wasActivityResumed = false;

	private Button takePictureButton;
	private Button switchButton;
	private Button videoButton;

	private CameraHelper mCameraHelper;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_test);
		context = getApplicationContext();

		takePictureButton = (Button) findViewById(R.id.btn_takepicture);
		switchButton = (Button) findViewById(R.id.btn_switch);
		videoButton = (Button) findViewById(R.id.btn_video);
		mPreview = (CameraSourcePreview) findViewById(R.id.preview);
		mGraphicOverlay = (GraphicOverlay) findViewById(R.id.faceOverlay);
		cameraVersion = (TextView) findViewById(R.id.cameraVersion);
		ivAutoFocus = (ImageView) findViewById(R.id.ivAutoFocus);

		mCameraHelper = new CameraHelper(context, mPreview, mGraphicOverlay, mFaceDetectionTask);

		if (Utils.checkGooglePlayAvailability(this))
		{
			requestPermissionThenOpenCamera();

			switchButton.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					if (mCameraHelper.isUsingFrontCamera())
					{
						mCameraHelper.startBackCamera();
					}
					else
					{
						mCameraHelper.startFrontCamera();
					}
				}
			});

			takePictureButton.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					switchButton.setEnabled(false);
					videoButton.setEnabled(false);
					takePictureButton.setEnabled(false);

					mCameraHelper.takePicture(mPictureTakenListener);
				}
			});

			videoButton.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{

					if (mCameraHelper.isRecordingVideo())
					{
						mCameraHelper.stopRecordingVideo();
					}
					else
					{
						mCameraHelper.startRecordingVideo(mVideoRecordingStatusListener);
					}

				}
			});

			mCameraHelper.setCameraPreviewTouchListener(mCameraPreviewTouchListener);
		}
	}

	private FaceDetectionTask mFaceDetectionTask = new FaceDetectionTask(3000, 1000)
	{
		private int mFaceId = -1;

		@Override
		public void onFaceDetection(final Face face, final long millisUntilFinished)
		{
			handle(face, millisUntilFinished);
		}

		private synchronized void handle(final Face face, final long millisUntilFinished)
		{
			Log.i("FaceDetectionTask", "millisUntilFinished: " + millisUntilFinished / 1000);

			if (mCameraHelper.isRecordingVideo())
			{
				if (mFaceId != face.getId() || (Math.abs((millisUntilFinished / 1000)) >= 10))
				{
					mFaceDetectionTask.stopThread();

					shouldDeleteRecordedFile = (mFaceId != face.getId());
					mCameraHelper.stopRecordingVideo();
				}
			}
			else if (millisUntilFinished <= 0 && isFace(face))
			{
				mFaceId = face.getId();
				mCameraHelper.startRecordingVideo(mVideoRecordingStatusListener);
			}
		}

		private boolean isFace(Face face)
		{
			if (face != null && face.getLandmarks() != null)
			{
				int eyeProbability = 0;
				int mouthProbability = 0;
				int noseBaseProbability = 0;

				//DO NOT SET TO NULL THE NON EXISTENT LANDMARKS. USE OLDER ONES INSTEAD.
				for (Landmark landmark : face.getLandmarks())
				{
					switch (landmark.getType())
					{
						case Landmark.LEFT_EYE:
						case Landmark.RIGHT_EYE:
							eyeProbability = eyeProbability + 1;
							break;
						case Landmark.LEFT_MOUTH:
						case Landmark.RIGHT_MOUTH:
						case Landmark.BOTTOM_MOUTH:
							mouthProbability = mouthProbability + 1;
							break;
						case Landmark.NOSE_BASE:
							noseBaseProbability = 1;
							break;
					}
				}

				return (eyeProbability + mouthProbability + noseBaseProbability) > 4;
			}
			return false;
		}
	};

	private CameraSourcePreview.OnTouchListener mCameraPreviewTouchListener = new CameraSourcePreview.OnTouchListener()
	{
		@Override
		public boolean onTouch(final View v, final MotionEvent pEvent)
		{
			v.onTouchEvent(pEvent);
			if (pEvent.getAction() == MotionEvent.ACTION_DOWN)
			{
				int autoFocusX = (int) (pEvent.getX() - Utils.dpToPx(60) / 2);
				int autoFocusY = (int) (pEvent.getY() - Utils.dpToPx(60) / 2);
				ivAutoFocus.setTranslationX(autoFocusX);
				ivAutoFocus.setTranslationY(autoFocusY);
				ivAutoFocus.setVisibility(View.VISIBLE);
				ivAutoFocus.bringToFront();

				mCameraHelper.autoFocus(mAutoFocusListener, pEvent, v.getWidth(), v.getHeight());
			}
			return false;
		}
	};

	private CameraHelper.AutoFocusListener mAutoFocusListener = new CameraHelper.AutoFocusListener()
	{
		@Override
		public void onAutoFocus(final boolean success)
		{
			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					ivAutoFocus.setVisibility(View.GONE);
				}
			});
		}
	};

	private boolean shouldDeleteRecordedFile;

	private final CameraHelper.VideoRecordingStatusListener mVideoRecordingStatusListener = new CameraHelper.VideoRecordingStatusListener()
	{
		@Override
		public void onStatusChange(final CameraHelper.VideoRecordingStatus status, final String videoFile, final Exception error)
		{
			switch (status)
			{
				case STARTING:
				case STOPPING:
				{
					runOnUiThread(new Runnable()
					{
						@Override
						public void run()
						{
							switchButton.setEnabled(false);
							takePictureButton.setEnabled(false);
							videoButton.setEnabled(false);

							if (status == CameraHelper.VideoRecordingStatus.STARTING)
							{
								mCameraHelper.recordVideo();
							}
							else
							{
								mCameraHelper.stopVideo();
								mFaceDetectionTask.startThread();
							}
						}
					});
				}
				break;
				case RECORDING:
				{
					shouldDeleteRecordedFile = false;

					runOnUiThread(new Runnable()
					{
						@Override
						public void run()
						{
							videoButton.setEnabled(true);
							videoButton.setText(getString(R.string.stop_video));
						}
					});
					Toast.makeText(context, "Video STARTED!", Toast.LENGTH_SHORT).show();
				}
				break;
				case STOPPED:
				{
					if (!TextUtils.isEmpty(videoFile))
					{
						if (shouldDeleteRecordedFile)
						{
							try
							{
								if (new File(videoFile).delete())
								{
									Toast.makeText(context, "Delete the Recorded File", Toast.LENGTH_SHORT).show();
								}
							}
							catch (Exception e)
							{

							}
						}
						else
						{

						}
					}

					runOnUiThread(new Runnable()
					{
						@Override
						public void run()
						{
							switchButton.setEnabled(true);
							takePictureButton.setEnabled(true);
							videoButton.setEnabled(true);
							videoButton.setText(getString(R.string.record_video));
						}
					});
					Toast.makeText(context, "Video STOPPED!", Toast.LENGTH_SHORT).show();
				}
				break;
			}
		}
	};


	private final CameraHelper.OnPictureTakenListener mPictureTakenListener = new CameraHelper.OnPictureTakenListener()
	{
		@Override
		public void onShutter()
		{
			Log.d(TAG, "Shutter Callback!");
		}

		@TargetApi(Build.VERSION_CODES.LOLLIPOP)
		@Override
		public void onPictureTaken(final Image image)
		{
			Log.d(TAG, "Taken picture is here!");
			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					switchButton.setEnabled(true);
					videoButton.setEnabled(true);
					takePictureButton.setEnabled(true);
				}
			});

			ByteBuffer buffer = image.getPlanes()[0].getBuffer();
			byte[] bytes = new byte[buffer.capacity()];
			buffer.get(bytes);
			Bitmap picture = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
			FileOutputStream out = null;
			try
			{
				out = new FileOutputStream(new File(Utils.getNewImageFilePath()));
				picture.compress(Bitmap.CompressFormat.JPEG, 95, out);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			finally
			{
				try
				{
					if (out != null)
					{
						out.close();
					}
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}

		@Override
		public void onPictureTaken(final Bitmap picture)
		{
			Log.d(TAG, "Taken picture is here!");
			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					switchButton.setEnabled(true);
					videoButton.setEnabled(true);
					takePictureButton.setEnabled(true);
				}
			});
			FileOutputStream out = null;
			try
			{
				out = new FileOutputStream(new File(Utils.getNewImageFilePath()));
				picture.compress(Bitmap.CompressFormat.JPEG, 95, out);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			finally
			{
				try
				{
					if (out != null)
					{
						out.close();
					}
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}
	};

	private void requestPermissionThenOpenCamera()
	{
		if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
		{
			if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
			{
				mCameraHelper.startFrontCamera();
				cameraVersion.setText(mCameraHelper.isUsingFrontCamera() ? "Camera 2" : "Camera 1");
			}
			else
			{
				ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
			}
		}
		else
		{
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
	{
		if (requestCode == REQUEST_CAMERA_PERMISSION)
		{
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
			{
				requestPermissionThenOpenCamera();
			}
			else
			{
				Toast.makeText(Test2Activity.this, "CAMERA PERMISSION REQUIRED", Toast.LENGTH_LONG).show();
				finish();
			}
		}
		if (requestCode == REQUEST_STORAGE_PERMISSION)
		{
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
			{
				requestPermissionThenOpenCamera();
			}
			else
			{
				Toast.makeText(Test2Activity.this, "STORAGE PERMISSION REQUIRED", Toast.LENGTH_LONG).show();
			}
		}
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		mCameraHelper.onResume();
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		mCameraHelper.onPause();
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		mCameraHelper.onDestroy();
	}
}
