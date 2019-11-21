package com.example.ezequiel.camera2.helper;

import com.example.ezequiel.camera2.others.Camera2Source;
import com.example.ezequiel.camera2.others.CameraSource;

public abstract class VideoRecordingHelper
		implements CameraSource.VideoStartCallback, CameraSource.VideoStopCallback, CameraSource.VideoErrorCallback, Camera2Source.VideoStartCallback, Camera2Source.VideoStopCallback, Camera2Source.VideoErrorCallback
{
	private CameraHelper.VideoRecordingStatus mVideoRecordingStatus = CameraHelper.VideoRecordingStatus.STOPPED;

	public boolean isRecording()
	{
		return mVideoRecordingStatus != CameraHelper.VideoRecordingStatus.STOPPED;
	}

	public void startRecording()
	{
		if (mVideoRecordingStatus == CameraHelper.VideoRecordingStatus.STOPPED)
		{
			setStatus(CameraHelper.VideoRecordingStatus.STARTING, "", null);
		}
	}

	public void stopRecording()
	{
		if (mVideoRecordingStatus == CameraHelper.VideoRecordingStatus.RECORDING)
		{
			setStatus(CameraHelper.VideoRecordingStatus.STOPPING, "", null);
		}
	}

	@Override
	public void onVideoStart()
	{
		setStatus(CameraHelper.VideoRecordingStatus.RECORDING, "", null);
	}

	@Override
	public void onVideoStop(final String videoFile)
	{
		setStatus(CameraHelper.VideoRecordingStatus.STOPPED, videoFile, null);
	}

	@Override
	public void onVideoError(final String error)
	{
		setStatus(CameraHelper.VideoRecordingStatus.STOPPED, "", new Exception(error));
	}

	private void setStatus(final CameraHelper.VideoRecordingStatus videoRecordingStatus, String videoFile, Exception error)
	{
		mVideoRecordingStatus = videoRecordingStatus;
		onStatusChange(mVideoRecordingStatus, videoFile, error);
	}

	abstract void onStatusChange(CameraHelper.VideoRecordingStatus videoRecordingStatus, String videoFile, Exception error);

	public abstract void recordVideo();

	public abstract void stopVideo();
}
