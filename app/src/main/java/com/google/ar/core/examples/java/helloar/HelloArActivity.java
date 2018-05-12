/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ar.core.examples.java.helloar;

import android.app.FragmentTransaction;
import android.net.Uri;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable.TrackingState;
import com.google.ar.core.examples.java.helloar.content.FunnyTileFragment;
import com.google.ar.core.examples.java.helloar.content.NewsTileFragment;
import com.google.ar.core.examples.java.helloar.rendering.BackgroundRenderer;
import com.google.ar.core.examples.java.helloar.rendering.ObjectRenderer;
import com.google.ar.core.examples.java.helloar.rendering.PlaneRenderer;
import com.google.ar.core.examples.java.helloar.rendering.PointCloudRenderer;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * This is a simple example that shows how to create an augmented reality (AR) application using the
 * ARCore API. The application will display any detected planes and will allow the user to tap on a
 * plane to place a 3d model of the Android robot.
 */
public class HelloArActivity extends AppCompatActivity implements GLSurfaceView.Renderer, NewsTileFragment.OnFragmentInteractionListener, FunnyTileFragment.OnFragmentInteractionListener {
    private static final String TAG = HelloArActivity.class.getSimpleName();
    private GLSurfaceView mSurfaceView;

    private Session mSession;
    private GestureDetector mGestureDetector;
    private Snackbar mMessageSnackbar;
    private DisplayRotationHelper mDisplayRotationHelper;

    private final BackgroundRenderer mBackgroundRenderer = new BackgroundRenderer();
    private final ObjectRenderer mVirtualObject = new ObjectRenderer();
    private final ObjectRenderer mVirtualFirstTile = new ObjectRenderer();
    private final ObjectRenderer mVirtualSecondTile = new ObjectRenderer();

    private final PlaneRenderer mPlaneRenderer = new PlaneRenderer();
    private final PointCloudRenderer mPointCloud = new PointCloudRenderer();

    private boolean isInitialPositionReceived = false;

    private final float[] mAnchorMatrix = new float[16];
    private Anchor initialPinboardAnchor;

    private final ArrayBlockingQueue<MotionEvent> mQueuedSingleTaps = new ArrayBlockingQueue<>(16);
    private ArrayBlockingQueue<MotionEvent> mQueuedLongPress = new ArrayBlockingQueue<>(16);

    private final ArrayList<Anchor> mAnchors = new ArrayList<>();

    private int viewWidth = 0;
    private int viewHeight = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSurfaceView = findViewById(R.id.surfaceview);
        mDisplayRotationHelper = new DisplayRotationHelper(/*context=*/ this);

        // Set up tap listener.
        mGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                onSingleTap(e);
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                mQueuedLongPress.offer(e);
            }

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }
        });

        mSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return mGestureDetector.onTouchEvent(event);
            }
        });


        // Set up renderer.
        mSurfaceView.setPreserveEGLContextOnPause(true);
        mSurfaceView.setEGLContextClientVersion(2);
        mSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Alpha used for plane blending.
        mSurfaceView.setRenderer(this);
        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        Exception exception = null;
        String message = null;
        try {
            mSession = new Session(/* context= */ this);
        } catch (UnavailableArcoreNotInstalledException e) {
            message = "Please install ARCore";
            exception = e;
        } catch (UnavailableApkTooOldException e) {
            message = "Please update ARCore";
            exception = e;
        } catch (UnavailableSdkTooOldException e) {
            message = "Please update this app";
            exception = e;
        } catch (Exception e) {
            message = "This device does not support AR";
            exception = e;
        }

        if (message != null) {
            showSnackbarMessage(message, true);
            Log.e(TAG, "Exception creating session", exception);
            return;
        }

        // Create default config and check if supported.
        Config config = new Config(mSession);
        if (!mSession.isSupported(config)) {
            showSnackbarMessage("This device does not support AR", true);
        }
        mSession.configure(config);


    }

    @Override
    protected void onResume() {
        super.onResume();

        // ARCore requires camera permissions to operate. If we did not yet obtain runtime
        // permission on Android M and above, now is a good time to ask the user for it.
        if (CameraPermissionHelper.hasCameraPermission(this)) {
            if (mSession != null) {
               // showLoadingMessage();
                // Note that order matters - see the note in onPause(), the reverse applies here.
                mSession.resume();
            }
            isInitialPositionReceived = false;
            mSurfaceView.onResume();
            mDisplayRotationHelper.onResume();
        } else {
            CameraPermissionHelper.requestCameraPermission(this);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Note that the order matters - GLSurfaceView is paused first so that it does not try
        // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
        // still call mSession.update() and get a SessionPausedException.
        mDisplayRotationHelper.onPause();
        mSurfaceView.onPause();
        isInitialPositionReceived = false;
        if (mSession != null) {
            mSession.pause();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(this,
                "Camera permission is needed to run this application", Toast.LENGTH_LONG).show();
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(this);
            }
            finish();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // Standard Android full-screen functionality.
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void onSingleTap(MotionEvent e) {
        // Queue tap if there is space. Tap is lost if queue is full.
        mQueuedSingleTaps.offer(e);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        // Create the texture and pass it to ARCore session to be filled during update().
        mBackgroundRenderer.createOnGlThread(/*context=*/ this);
        if (mSession != null) {
            mSession.setCameraTextureName(mBackgroundRenderer.getTextureId());
        }

        // Prepare the other rendering objects.
        try {
            mVirtualObject.createOnGlThread(/*context=*/this, "pinboard5.obj", "6443928-large-corkboard-texture-or-background--Stock-Photo.jpg");
            mVirtualObject.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);

            mVirtualFirstTile.createOnGlThread(this, "newsTileNew.obj","newsTile.jpg");
            mVirtualFirstTile.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);

            mVirtualSecondTile.createOnGlThread(this, "funnyTileNew2.obj", "funnyTile.jpg");
            mVirtualSecondTile.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);

        } catch (IOException e) {
            Log.e(TAG, "Failed to read obj file");
        }
        try {
            mPlaneRenderer.createOnGlThread(/*context=*/this, "trigrid.png");
        } catch (IOException e) {
            Log.e(TAG, "Failed to read plane texture");
        }
        mPointCloud.createOnGlThread(/*context=*/this);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mDisplayRotationHelper.onSurfaceChanged(width, height);
        GLES20.glViewport(0, 0, width, height);
        viewWidth = width;
        viewHeight = height;
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // Clear screen to notify driver it should not load any pixels from previous frame.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (mSession == null) {
            return;
        }
        // Notify ARCore session that the view size changed so that the perspective matrix and
        // the video background can be properly adjusted.
        mDisplayRotationHelper.updateSessionIfNeeded(mSession);


        try {
            // Obtain the current frame from ARSession. When the configuration is set to
            // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
            // camera framerate.
            Frame frame = mSession.update();
            Camera camera = frame.getCamera();

            // Handle taps. Handling only one tap per frame, as taps are usually low frequency
            // compared to frame rate.
            MotionEvent tap = mQueuedSingleTaps.poll();


            if (tap != null && camera.getTrackingState() == TrackingState.TRACKING) {
                Log.d("Test1", "----------");

                if (checkIfHit(mVirtualFirstTile,tap,1)) {
                    NewsTileFragment fragment = NewsTileFragment.newInstance(null,null);
                    FragmentTransaction transaction = getFragmentManager().beginTransaction();
                    transaction.replace(R.id.surface_layout, fragment).addToBackStack(null);
                    transaction.commit();

                } else if (checkIfHit(mVirtualSecondTile,tap,2)){
                    FunnyTileFragment fragment = FunnyTileFragment.newInstance(null,null);
                    FragmentTransaction transaction = getFragmentManager().beginTransaction();
                    transaction.replace(R.id.surface_layout, fragment).addToBackStack(null);
                    transaction.commit();
                }
            }
            mBackgroundRenderer.draw(frame);

            // If not tracking, don't draw 3d objects.
            if (camera.getTrackingState() == TrackingState.PAUSED) {
                return;
            }

            // Get projection matrix.
            float[] projmtx = new float[16];
            camera.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f);

            // Get camera matrix and draw.
            float[] viewmtx = new float[16];
            camera.getViewMatrix(viewmtx, 0);

            // Compute lighting from average intensity of the image.
            final float lightIntensity = frame.getLightEstimate().getPixelIntensity();

            // Visualize tracked points.
            PointCloud pointCloud = frame.acquirePointCloud();
            mPointCloud.update(pointCloud);
            mPointCloud.draw(viewmtx, projmtx);

            pointCloud.release();

            if (!isInitialPositionReceived && initialPinboardAnchor == null) {
                Anchor fixAnchor = mSession.createAnchor(
                        frame.getCamera().getPose()
                                .compose(Pose.makeTranslation(0,0,-1.5f))
                                .extractTranslation());
                mAnchors.add(fixAnchor);
                initialPinboardAnchor = fixAnchor;
                isInitialPositionReceived = true;
            }

            float scaleFactor = 1.0f;
            for (Anchor anchor : mAnchors) {
                if (anchor.getTrackingState() != TrackingState.TRACKING) {
                    continue;
                }
                // Get the current pose of an Anchor in world space. The Anchor pose is updated
                // during calls to session.update() as ARCore refines its estimate of the world.
                anchor.getPose().toMatrix(mAnchorMatrix, 0);

                // Update and draw the model and its shadow.

                mVirtualObject.updateModelMatrix(mAnchorMatrix, scaleFactor);
                mVirtualFirstTile.updateModelMatrix(mAnchorMatrix, scaleFactor);
                mVirtualSecondTile.updateModelMatrix(mAnchorMatrix, scaleFactor);
                mVirtualObject.draw(viewmtx, projmtx, lightIntensity);
                mVirtualFirstTile.draw(viewmtx, projmtx, lightIntensity);
                mVirtualSecondTile.draw(viewmtx, projmtx, lightIntensity);
            }

        } catch (Throwable t) {
            // Avoid crashing the application due to unhandled exceptions.
            Log.e(TAG, "Exception on the OpenGL thread", t);
        }
    }

    private void showSnackbarMessage(String message, boolean finishOnDismiss) {
        mMessageSnackbar = Snackbar.make(
            HelloArActivity.this.findViewById(android.R.id.content),
            message, Snackbar.LENGTH_INDEFINITE);
        mMessageSnackbar.getView().setBackgroundColor(0xbf323232);
        if (finishOnDismiss) {
            mMessageSnackbar.setAction(
                "Dismiss",
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mMessageSnackbar.dismiss();
                    }
                });
            mMessageSnackbar.addCallback(
                new BaseTransientBottomBar.BaseCallback<Snackbar>() {
                    @Override
                    public void onDismissed(Snackbar transientBottomBar, int event) {
                        super.onDismissed(transientBottomBar, event);
                        finish();
                    }
                });
        }
        mMessageSnackbar.show();
    }

    private Boolean checkIfHit(ObjectRenderer renderer, MotionEvent event, final int cubeIndex){
        if(isMVPMatrixHitMotionEvent2(renderer,event)) {
            // long press hit a tile, show content menu for the tile
            Log.d("Test1", "TILE: " + cubeIndex + " HIT <3");

            final String tileIndex = "" + cubeIndex;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), tileIndex, Toast.LENGTH_SHORT).show();
                }
            });
            return true;
       } else {
            final String tileIndex = "" + cubeIndex;
            Log.d("Test1", "TILE: " + tileIndex + " NOT HIT");
            return false;
        }

    }
    private boolean isMVPMatrixHitMotionEvent2(ObjectRenderer object, MotionEvent event){

        float[] mvpMatrix = object.getmModelViewProjectionMatrix();

        Log.d("Test", "------START COMPARING------");
        Log.d("Test", "MVP Matrix: " + mvpMatrix.toString());

        for (int i = 0; i < object.triangles.size(); i++)
        {
            ObjectRenderer.Triangle triangle = object.triangles.get(i);

            float[] point1 = new float[4];
            float[] point2 = new float[4];
            float[] point3 = new float[4];

            point1[0] = triangle.vertex1[0];
            point1[1] = triangle.vertex1[1];
            point1[2] = triangle.vertex1[2];
            point1[3] = 0;

            point2[0] = triangle.vertex2[0];
            point2[1] = triangle.vertex2[1];
            point2[2] = triangle.vertex2[2];
            point2[3] = 0;

            point3[0] = triangle.vertex3[0];
            point3[1] = triangle.vertex3[1];
            point3[2] = triangle.vertex3[2];
            point3[3] = 0;

            Log.d("Test", "Point1: " + point1[0] + " " + point1[1]+ " " + point1[2]+ " " + point1[3]);
            Log.d("Test", "Point2: " + point2[0] + " " + point2[1]+ " " + point2[2]+ " " + point2[3]);
            Log.d("Test", "Point3: " + point3[0] + " " + point3[1]+ " " + point3[2]+ " " + point3[3]);

            float[] calculatedPoint1 = new float[4];
            float[] calculatedPoint2 = new float[4];
            float[] calculatedPoint3 = new float[4];

            Matrix.multiplyMV(calculatedPoint1, 0, mvpMatrix, 0, point1, 0);
            Matrix.multiplyMV(calculatedPoint2, 0, mvpMatrix, 0, point2, 0);
            Matrix.multiplyMV(calculatedPoint3, 0, mvpMatrix, 0, point3, 0);

            float[] point12D = {calculatedPoint1[0], calculatedPoint1[1]};
            float[] point22D = {calculatedPoint2[0], calculatedPoint2[1]};
            float[] point32D = {calculatedPoint3[0], calculatedPoint3[1]};

            Log.d("Test", "Point12D: " + point12D[0] + " " + point12D[1]);
            Log.d("Test", "Point22D: " + point22D[0] + " " + point22D[1]);
            Log.d("Test", "Point32D: " + point32D[0] + " " + point32D[1]);

            // Get Touchposition
            float[] touchPosition = {event.getX(), event.getY()};
            float[] convertedTouchPosition = convertToFrame(touchPosition, mSurfaceView.getWidth(), mSurfaceView.getHeight());

            Log.d("Test", "ConvertedTouch: " + convertedTouchPosition[0] + " " + convertedTouchPosition[1]);

            boolean b1,b2,b3;

            b1 = sign(convertedTouchPosition, point12D, point22D) < 0.0f;
            b2 = sign(convertedTouchPosition, point22D, point32D) < 0.0f;
            b3 = sign(convertedTouchPosition, point32D, point12D) < 0.0f;

            Log.d("Test", "b1: " + b1);
            Log.d("Test", "b2: " + b2);
            Log.d("Test", "b3: " + b3);

            Log.d("Test", "------END COMPARING------");

            if ((b1 == b2) && (b2 == b3)) {
                Log.d("Test", "TOTAL   ---> TRUE");
                return true;
            } else {
                Log.d("Test", "TOTAL  ---> FALSE");
            }
        }

        return false;
    }

    public float sign(float[] point1, float[] point2, float[] point3) {
        return (point1[0] - point3[0]) * (point2[1] - point3[1]) - (point2[0] - point3[0]) * (point1[1] - point3[1]);
    }

    private boolean isMVPMatrixHitMotionEvent(ObjectRenderer object, MotionEvent event){

        float[] mvpMatrix = object.getmModelViewProjectionMatrix();

        Log.d("Test", "------START COMPARING------");
        Log.d("Test", "MVP Matrix: " + mvpMatrix.toString());

        for (int i = 0; i < object.triangles.size(); i++)
        {
            ObjectRenderer.Triangle triangle = object.triangles.get(i);

            float[] point1 = new float[4];
            float[] point2 = new float[4];
            float[] point3 = new float[4];

            point1[0] = triangle.vertex1[0];
            point1[1] = triangle.vertex1[1];
            point1[2] = triangle.vertex1[2];
            point1[3] = 0;

            point2[0] = triangle.vertex2[0];
            point2[1] = triangle.vertex2[1];
            point2[2] = triangle.vertex2[2];
            point2[3] = 0;

            point3[0] = triangle.vertex3[0];
            point3[1] = triangle.vertex3[1];
            point3[2] = triangle.vertex3[2];
            point3[3] = 0;

            Log.d("Test", "Point1: " + point1[0] + " " + point1[1]+ " " + point1[2]+ " " + point1[3]);
            Log.d("Test", "Point2: " + point2[0] + " " + point2[1]+ " " + point2[2]+ " " + point2[3]);
            Log.d("Test", "Point3: " + point3[0] + " " + point3[1]+ " " + point3[2]+ " " + point3[3]);

            float[] calculatedPoint1 = new float[4];
            float[] calculatedPoint2 = new float[4];
            float[] calculatedPoint3 = new float[4];

            Matrix.multiplyMV(calculatedPoint1, 0, mvpMatrix, 0, point1, 0);
            Matrix.multiplyMV(calculatedPoint2, 0, mvpMatrix, 0, point2, 0);
            Matrix.multiplyMV(calculatedPoint3, 0, mvpMatrix, 0, point3, 0);

            float[] point12D = {calculatedPoint1[0], calculatedPoint1[1]};
            float[] point22D = {calculatedPoint2[0], calculatedPoint2[1]};
            float[] point32D = {calculatedPoint3[0], calculatedPoint3[1]};

            Log.d("Test", "Point12D: " + point12D[0] + " " + point12D[1]);
            Log.d("Test", "Point22D: " + point22D[0] + " " + point22D[1]);
            Log.d("Test", "Point32D: " + point32D[0] + " " + point32D[1]);

            // Get Touchposition
            float[] touchPosition = {event.getX(), event.getY()};
            float[] convertedTouchPosition = convertToFrame(touchPosition, mSurfaceView.getWidth(), mSurfaceView.getHeight());

            Log.d("Test", "ConvertedTouch: " + convertedTouchPosition[0] + " " + convertedTouchPosition[1]);

            float totalArea = calcTriangleArea(point12D, point22D, point32D);

            float area1 = calcTriangleArea(convertedTouchPosition, point22D, point32D);
            float area2 = calcTriangleArea(convertedTouchPosition, point12D, point32D);
            float area3 = calcTriangleArea(convertedTouchPosition, point12D, point22D);

            Log.d("Test", "TotalArea: " + totalArea);
            Log.d("Test", "area1: " + area1);
            Log.d("Test", "area2: " + area2);
            Log.d("Test", "area3: " + area3);

            Log.d("Test", "------END COMPARING------");

            if ((area1 + area2 + area3) <= totalArea) {
                Log.d("Test", "TOTAL AREA  ---> TRUE");
                return true;
            } else {
                Log.d("Test", "TOTAL AREA ---> FALSE");

            }

        }

        return false;
    }

    float calcTriangleArea(float[] point1, float[] point2, float[] point3) {
        float det = 0;
        det = (((point1[0] + 5.f) - (point3[0] + 5.f)) * ((point2[1] + 5.f) - (point3[1] + 5.f))) - (((point2[0] + 5.f) - (point3[0] + 5.f)) * ((point1[1] + 5.f) - (point3[1] + 5.f)));
        return (det / 2.0f);
    }
    float calcTriangleAreaGeneral(float[] point1, float[] point2, float[] point3) {
        float det = 0;
        det = ((point1[0] - point3[0]) * (point2[1] - point3[1])) - ((point2[0] - point3[0]) * (point1[1] - point3[1]));
        return (det / 2.0f);
    }

    float[] convertToFrame(float[] touchPositon, float width, float height) {
        float[] result = new float[2];

        result[0] = (touchPositon[0] / (width / 2.f)) - 1.f;
        result[1] = ((touchPositon[1] / (height / 2.0f)) - 1.f) * -1.f;

        return result;
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }
}
