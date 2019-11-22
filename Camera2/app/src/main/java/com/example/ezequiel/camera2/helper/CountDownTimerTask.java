package com.example.ezequiel.camera2.helper;

import android.os.SystemClock;
import android.util.Log;

/**
 * Created by Ashish Das on 20/11/2019.
 */
public abstract class CountDownTimerTask implements Runnable
{
	private static final String TAG = "CountDownTimerTask";

	private Thread backgroundThread = null;
	private CountDownTimerHandler mCountDownTimerHandler;

	private final long mMillisInFuture;
	private final long mCountdownInterval;

	private volatile Boolean mIsRunning = false;

	public CountDownTimerTask(long millisInFuture, long countDownInterval)
	{
		mMillisInFuture = millisInFuture;
		mCountdownInterval = countDownInterval;
	}

	public Boolean isRunning()
	{
		return mIsRunning;
	}

	public synchronized void startThread()
	{
		if (!isRunning())
		{
			if(backgroundThread == null)
			{
				backgroundThread = new Thread(this);
				backgroundThread.start();
			}
			mIsRunning = true;
		}
	}

	public synchronized void stopThread()
	{
		if (isRunning())
		{
			mIsRunning = false;
			backgroundThread.interrupt();
		}
	}

	public synchronized final boolean start()
	{
		if (isRunning() && mCountDownTimerHandler != null)
		{
			mCountDownTimerHandler.start();

			return true;
		}

		return false;
	}

	public synchronized final boolean cancel()
	{
		if (mCountDownTimerHandler != null)
		{
			mCountDownTimerHandler.stop();

			return true;
		}

		return false;
	}

	public void run()
	{
		try
		{
			mCountDownTimerHandler = new CountDownTimerHandler();

			while (mIsRunning)
			{
				try
				{
					Thread.sleep(mCountDownTimerHandler.handle());
				}
				catch (Exception e)
				{

				}
			}
		}
		finally
		{
			mCountDownTimerHandler = null;
			backgroundThread = null;
		}
	}

	private class CountDownTimerHandler
	{
		private long mStopTimeInFuture;
		private boolean mCancelled = false;

		public synchronized void start()
		{
			mCancelled = false;
			if (mMillisInFuture <= 0)
			{
				onTick(mMillisInFuture);
				return;
			}

			mStopTimeInFuture = 0;
		}

		public synchronized void stop()
		{
			mCancelled = true;
			mStopTimeInFuture = 0;
		}

		public synchronized long handle()
		{
			if (mCancelled)
			{
				return mCountdownInterval;
			}

			if(mStopTimeInFuture <= 0)
			{
				mStopTimeInFuture = SystemClock.elapsedRealtime() + mMillisInFuture;
			}

			final long millisLeft = mStopTimeInFuture - SystemClock.elapsedRealtime();

			Log.d(TAG, "T" + mStopTimeInFuture+", C" + SystemClock.elapsedRealtime()+", L"+millisLeft);

			if (millisLeft <= 0)
			{
				onTick(millisLeft);
				return mCountdownInterval;
			}

			long lastTickStart = SystemClock.elapsedRealtime();
			onTick(millisLeft);

			// take into account user's onTick taking time to execute
			long lastTickDuration = SystemClock.elapsedRealtime() - lastTickStart;
			long delay;

			if (millisLeft < mCountdownInterval)
			{
				// just delay until done
				delay = millisLeft - lastTickDuration;

				// special case: user's onTick took more than interval to
				// complete, trigger onFinish without delay
				if (delay < 0)
				{
					delay = 0;
				}
			}
			else
			{
				delay = mCountdownInterval - lastTickDuration;

				// special case: user's onTick took more than interval to
				// complete, skip to next interval
				while (delay < 0)
				{
					delay += mCountdownInterval;
				}
			}
			return delay > mCountdownInterval ? mCountdownInterval : delay;
		}
	}

	/**
	 * Callback fired on regular interval.
	 *
	 * @param millisUntilFinished The amount of time until finished.
	 */
	public abstract void onTick(long millisUntilFinished);
}