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

	public void restart(Face face)
	{
		if(!inProcess && face != null && mFace != null && face.getId() == mFace.getId())
		{
			restart();
		}
	}

	public void restart()
	{
		if(!inProcess)
		{
			mFace = null;

			_cancel();
		}
	}

	public void start(Face face)
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

	private void _cancel()
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
		Log.i(TAG, "FaceId: " + (mFace != null ? mFace.getId() : -1) + ", seconds remaining: " + millisUntilFinished / 1000);
	}

	@Override
	public void onFinish()
	{
		inProcess = true;
		notifyFaceDetection(mFace);
		inProcess = false;
	}

	public abstract void onFaceDetection(Face face);

	private void notifyFaceDetection(Face face)
	{
		if (isFace(face))
		{
			onFaceDetection(face);
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
}
