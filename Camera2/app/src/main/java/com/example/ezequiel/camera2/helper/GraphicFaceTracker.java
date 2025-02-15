package com.example.ezequiel.camera2.helper;

import com.example.ezequiel.camera2.others.FaceGraphic;
import com.example.ezequiel.camera2.others.GraphicOverlay;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

/**
 * Created by Ashish Das on 20/11/2019.
 */
public class GraphicFaceTracker extends Tracker<Face>
{
	private GraphicOverlay mOverlay;
	private FaceGraphic mFaceGraphic;

	public GraphicFaceTracker(GraphicOverlay overlay, FaceDetectionTask faceDetectionTask)
	{
		mOverlay = overlay;
		mOverlay.setFaceDetectionTask(faceDetectionTask);
		mFaceGraphic = new FaceGraphic(overlay);

	}

	/**
	 * Start tracking the detected face instance within the face overlay.
	 */
	@Override
	public void onNewItem(int faceId, Face item)
	{
		mFaceGraphic.setId(faceId);
	}

	/**
	 * Update the position/characteristics of the face within the overlay.
	 */
	@Override
	public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face)
	{
		mOverlay.add(mFaceGraphic);
		mFaceGraphic.updateFace(face);
	}

	/**
	 * Hide the graphic when the corresponding face was not detected.  This can happen for
	 * intermediate frames temporarily (e.g., if the face was momentarily blocked from
	 * view).
	 */
	@Override
	public void onMissing(FaceDetector.Detections<Face> detectionResults)
	{
		mOverlay.remove(mFaceGraphic);
	}

	/**
	 * Called when the face is assumed to be gone for good. Remove the graphic annotation from
	 * the overlay.
	 */
	@Override
	public void onDone()
	{
		mOverlay.remove(mFaceGraphic);
	}
}
