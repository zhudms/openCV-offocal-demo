package org.opencv.samples.facedetect;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.Logger;

public class FdActivity extends Activity implements CvCameraViewListener2 {

    private static final String TAG = "asd";
    private static final Scalar FACE_RECT_COLOR = new Scalar(255, 0, 0, 255);
    public static final int JAVA_DETECTOR = 0;
    public static final int NATIVE_DETECTOR = 1;

    private MenuItem mItemFace50;
    private MenuItem mItemFace40;
    private MenuItem mItemFace30;
    private MenuItem mItemFace20;
    private MenuItem mItemType;

    private Mat mRgba;
    private Mat mGray;
    private File mCascadeFile;
    private CascadeClassifier mJavaDetector;
    private DetectionBasedTracker mNativeDetector;

    private int mDetectorType = JAVA_DETECTOR;
    private String[] mDetectorName;

    private float mRelativeFaceSize = 0.2f;
    private int mAbsoluteFaceSize = 0;

    private CameraBridgeViewBase mOpenCvCameraView;


    static {
        System.loadLibrary("opencv_java3");
    }


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");

                    // Load native library after(!) OpenCV initialization
//                    System.loadLibrary("detection_based_tracker");

//                    try {
//                        // load cascade file from application resources
//                        InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
//                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
//                        mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
//                        FileOutputStream os = new FileOutputStream(mCascadeFile);
//
//                        byte[] buffer = new byte[4096];
//                        int bytesRead;
//                        while ((bytesRead = is.read(buffer)) != -1) {
//                            os.write(buffer, 0, bytesRead);
//                        }
//                        is.close();
//                        os.close();
//
//                        mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
//                        if (mJavaDetector.empty()) {
//                            Log.e(TAG, "Failed to load cascade classifier");
//                            mJavaDetector = null;
//                        } else
//                            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());
//
//                        mNativeDetector = new DetectionBasedTracker(mCascadeFile.getAbsolutePath(), 0);
//
//                        cascadeDir.delete();
//
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                        Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
//                    }

                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };
    private int surfaceViewW;
    private int surfaceViewH;

    public FdActivity() {
        mDetectorName = new String[2];
        mDetectorName[JAVA_DETECTOR] = "Java";
        mDetectorName[NATIVE_DETECTOR] = "Native (tracking)";

        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
//        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.face_detect_surface_view);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.fd_activity_surface_view);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        System.loadLibrary("detection_based_tracker");
        try {
            // load cascade file from application resources
            InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);

            mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
            FileOutputStream os = new FileOutputStream(mCascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
            if (mJavaDetector.empty()) {
                Log.e(TAG, "Failed to load cascade classifier");
                mJavaDetector = null;
            } else
                Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());

            mNativeDetector = new DetectionBasedTracker(mCascadeFile.getAbsolutePath(), 0);

            cascadeDir.delete();

        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
        }

        EventBus.getDefault().register(this);

        Logger.addLogAdapter(new AndroidLogAdapter());
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
//        if (!OpenCVLoader.initDebug()) {
//            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
//            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
//        } else {
//            Log.d(TAG, "OpenCV library found inside package. Using it!");
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
//        }
    }

    public void onDestroy() {
        super.onDestroy();
        mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();

        surfaceViewW = width;
        surfaceViewH = height;

    }

    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
    }

    class TempFaces {
        int x;
        int y;
        int width;
        int height;
    }

    TempFaces face = new TempFaces();

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

        Log.d(TAG, "onCameraFrame: onframeonframe");

        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();


        if (mAbsoluteFaceSize == 0) {
            int height = mGray.rows();
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            }
            mNativeDetector.setMinFaceSize(mAbsoluteFaceSize);
        }

        MatOfRect faces = new MatOfRect();

        if (mDetectorType == JAVA_DETECTOR) {
            if (mJavaDetector != null)
                mJavaDetector.detectMultiScale(mGray, faces, 1.1, 2, 2, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
                        new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());

            Log.d(TAG, "onCameraFrame: mAbsoluteFaceSize=" + mAbsoluteFaceSize);
        } else if (mDetectorType == NATIVE_DETECTOR) {
            if (mNativeDetector != null)//应该是在这之前旋转。。。这该不会自己写矩阵转换吧。。。。
                mNativeDetector.detect(mGray, faces);
        } else {
            Log.e(TAG, "Detection method is not selected!");
        }

        Rect[] facesArray = faces.toArray();
        for (int i = 0; i < facesArray.length; i++) {
            Logger.d("face rect=" + i);
            Imgproc.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(), FACE_RECT_COLOR, 18);
            Logger.d("face rect=" +
                    facesArray[i].tl().x + File.separator + facesArray[i].tl().y + File.separator +
                    facesArray[i].br().x + File.separator + facesArray[i].br().y + File.separator);
//            if (i == 0) {
//                face.x = facesArray[0].x;
//                face.y = facesArray[0].y;
//                face.width = facesArray[0].width;
//                face.height = facesArray[0].height;
//
//            }

        }

//        有识别到人脸时
if (facesArray!=null&&facesArray.length!=0){
    Bitmap mCacheBitmap = Bitmap.createBitmap(surfaceViewW, surfaceViewH, Bitmap.Config.ARGB_8888);
//        Bitmap tempBitmap = Bitmap.createBitmap(faces.width(), faces.height(), Bitmap.Config.ARGB_8888);

    boolean bmpValid = true;
    if (mRgba != null) {
        try {
            Utils.matToBitmap(mRgba, mCacheBitmap);
//                tempBitmap = Bitmap.createBitmap(mCacheBitmap, face.x, face.y, face.width, face.height);


        } catch (Exception e) {

            Log.e(TAG, "Bitmap type: " + mCacheBitmap.getWidth() + "*" + mCacheBitmap.getHeight());
            Log.e(TAG, "Utils.matToBitmap() throws an exception: " + e.getMessage());
            bmpValid = false;
        }
    }

    EventBus.getDefault().post(new OnCallBack(mCacheBitmap));
//        EventBus.getDefault().post(new OnCallBack(tempBitmap));
}


        return mRgba;
    }

    @Subscribe
    public void onEvent(OnCallBack callBack) throws IOException {

        String rootPath = Environment.getExternalStorageDirectory().getAbsolutePath();

        String pathPrent = new StringBuffer(rootPath).append("/hehe/").toString();
        String path = new StringBuffer(pathPrent).append(System.currentTimeMillis()).append(".jpg").toString();
        File file = new File(path);
        File parent = file.getParentFile();
        if (!parent.exists() || parent.isFile()) {
            parent.mkdirs();
        }

        Bitmap mCacheBitmap = callBack.mBitmap;
        try {
            FileOutputStream stream = new FileOutputStream(path);
//            byte[] bytes = new byte[1024];
            mCacheBitmap.compress(Bitmap.CompressFormat.JPEG, 50, stream);
            stream.flush();
            stream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

//        TextView imageView= ((TextView) findViewById(R.id.image));
//        if (imageView!=null&&mCacheBitmap!=null){
//            Bitmap temp=Bitmap.createBitmap(mCacheBitmap,0,0,50,50);
//
////            imageView.setBackgroundColor(Color.RED);
//            TextView textView=new TextView(FdActivity.this);
//            LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(300,300);
//            textView.setLayoutParams(params);
////            ((ViewGroup) getCurrentFocus().getParent()).addView(textView);
//            textView.setBackgroundColor(Color.RED);
//        }
    }
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        Log.i(TAG, "called onCreateOptionsMenu");
//        mItemFace50 = menu.add("Face size 50%");
//        mItemFace40 = menu.add("Face size 40%");
//        mItemFace30 = menu.add("Face size 30%");
//        mItemFace20 = menu.add("Face size 20%");
//        mItemType   = menu.add(mDetectorName[mDetectorType]);
//        return true;
//    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
//        if (item == mItemFace50)
//            setMinFaceSize(0.5f);
//        else if (item == mItemFace40)
//            setMinFaceSize(0.4f);
//        else if (item == mItemFace30)
//            setMinFaceSize(0.3f);
//        else if (item == mItemFace20)
//            setMinFaceSize(0.2f);
//        else if (item == mItemType) {
//            int tmpDetectorType = (mDetectorType + 1) % mDetectorName.length;
//            item.setTitle(mDetectorName[tmpDetectorType]);
//            setDetectorType(tmpDetectorType);
//        }
//        return true;
//    }

    private void setMinFaceSize(float faceSize) {
        mRelativeFaceSize = faceSize;
        mAbsoluteFaceSize = 0;
    }

    private void setDetectorType(int type) {
        if (mDetectorType != type) {
            mDetectorType = type;

            if (type == NATIVE_DETECTOR) {
                Log.i(TAG, "Detection Based Tracker enabled");
                mNativeDetector.start();
            } else {
                Log.i(TAG, "Cascade detector enabled");
                mNativeDetector.stop();
            }
        }
    }
}
