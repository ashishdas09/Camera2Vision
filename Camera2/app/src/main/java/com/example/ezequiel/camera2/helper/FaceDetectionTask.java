package com.example.ezequiel.camera2.helper;

import android.util.Log;

import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.Landmark;

/**
 * Created by Ashish Das on 20/11/2019.
 */
public abstract class FaceDetectionTask extends CountDownTimerTask
{
	private static final String TAG = "FaceDetectionTask";

	private volatile Face mFace = null;
	private volatile Boolean isActive = false;
	private volatile Boolean inProcess = false;

	public FaceDetectionTask(final long millisInFuture, final long countDownInterval)
	{
		super(millisInFuture, countDownInterval);
		startThread();
	}

	@Override
	public synchronized void startThread()
	{
		restart();
		super.startThread();
	}

	@Override
	public synchronized void stopThread()
	{
		restart();
		super.stopThread();
	}

	public synchronized void restart(Face face)
	{
		if(!inProcess && face != null && mFace != null && face.getId() == mFace.getId())
		{
			restart();
		}
	}

	public synchronized void restart()
	{
		if(!inProcess)
		{
			mFace = null;

			_cancel();
		}
	}

	public synchronized void start(Face face)
	{
		if(face != null && !inProcess)
		{
			if (mFace == null || mFace.getId() > face.getId() || !(isActive || inProcess))
			{
				inProcess = true;

				if (isActive && mFace != null)
				{
					if (face.getId() == mFace.getId())
					{
						inProcess = false;
						return;
					}
					else
					{
						_cancel();
					}
				}

				mFace = face;
				inProcess = false;

				if (!isActive)
				{
					isActive = this.start();
				}
			}
		}
	}

	private synchronized void _cancel()
	{
		inProcess = false;

		if (isActive)
		{
			isActive = !this.cancel();
		}
	}

	@Override
	public void onTick(final long millisUntilFinished)
	{
		notifyFaceDetection(mFace, millisUntilFinished);
	}

	public abstract void onFaceDetection(Face face, long millisUntilFinished);

	private void notifyFaceDetection(Face face, long millisUntilFinished)
	{
		onFaceDetection(face, millisUntilFinished);
	}
}
