package com.example.finalproject_itstep.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.finalproject_itstep.R;
import com.example.finalproject_itstep.util.TessTwoOCR;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.InstallCallbackInterface;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class CamActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    public static final String TESS_DATA = "/tessdata";
    private static final String TAG = "CamActivity";

    static {
        if (!(OpenCVLoader.initDebug())) {
            Log.d(TAG, "  OpenCVLoader.initDebug(), working.");
        } else {
            Log.d(TAG, "  OpenCVLoader.initDebug(), not working.");
        }
    }

    JavaCameraView javaCameraView;
    Button btnReadText;
    TextView plateText;
    Bitmap bitmapToOCR;
    private Mat mRgba;
    private Mat mGray;
    private Mat img_roi;

    TessTwoOCR mTessOCR;

    private File mCascadeFile;
    private CascadeClassifier mJavaDetector;
    private static final Scalar PLATE_RECT_COLOR = new Scalar(0, 255, 0, 255);

    private float mRelativePlateSize = 0.3f;
    private int mAbsolutePlateSize = 0;
    private double scaleInt = 1.1;
    boolean clicked = false;

    BaseLoaderCallback mLoaderCallBack = new BaseLoaderCallback(this) {

        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case BaseLoaderCallback.SUCCESS:
                    Log.i(TAG, "Loader interface success");

                    // загрузка файла haarcascad'a из ресурсов
                    try {
                        InputStream is = getResources().openRawResource(R.raw.haarcascade_car_plate);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        mCascadeFile = new File(cascadeDir, "haarcascade_car_plate.xml");
                        FileOutputStream os = new FileOutputStream(mCascadeFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        mJavaDetector = new CascadeClassifier(
                                mCascadeFile.getAbsolutePath());
                        if (mJavaDetector.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier");
                            mJavaDetector = null;
                        } else {
                            Log.i(TAG, "Loaded cascade classifier from "
                                    + mCascadeFile.getAbsolutePath());
                        }

                        cascadeDir.delete();

                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                    }
                    javaCameraView.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }

        @Override
        public void onPackageInstall(final int operation, final InstallCallbackInterface callback) {
            switch (operation) {
                case InstallCallbackInterface.NEW_INSTALLATION: {
                    break;
                }
                default: {
                    super.onPackageInstall(operation, callback);
                    break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cam);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        prepareTessData();
        javaCameraView = findViewById(R.id.camView);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setCvCameraViewListener(this);
        mTessOCR = new TessTwoOCR(this);
        plateText = findViewById(R.id.plate_detected_text);
        javaCameraView.setMaxFrameSize(800, 600);
        btnReadText = findViewById(R.id.read_plate_btn);


        btnReadText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AsyncOcrTask task = new AsyncOcrTask();
                task.execute();
            }
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent me) {
        clicked = true;
        return super.dispatchTouchEvent(me);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (javaCameraView != null) {
            javaCameraView.disableView();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallBack)) {
            Log.e(TAG, "  OpenCVLoader.initAsync(), not working.");
            mLoaderCallBack.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        } else {
            Log.d(TAG, "  OpenCVLoader.initAsync(), working.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallBack);
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (javaCameraView != null) {
            javaCameraView.disableView();
        }

    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        javaCameraView.disableView();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();// for HAAR Cascade classifier does no matter colors

        if (mAbsolutePlateSize == 0) {
            int height = mGray.rows();
            if (Math.round(height * 0.05f) > 0) {
                mAbsolutePlateSize = Math.round(height * 0.05f);
            }
        }

        MatOfRect plates = new MatOfRect();// plates object

        if (mJavaDetector != null)
            mJavaDetector.detectMultiScale(mGray, plates, 1.1, 4,
                    2,  // TODO: objdetect.CV_HAAR_SCALE_IMAGE

                    new Size(mAbsolutePlateSize * 3, mAbsolutePlateSize),
                    new Size());

        final Rect[] plateArray = plates.toArray();
        for (Rect rect : plateArray) {
            Imgproc.rectangle(mRgba, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height),
                    PLATE_RECT_COLOR, 3);

        }
        return mRgba;

    }

    private void prepareTessData() {
        try {
            File dir = getExternalFilesDir(TESS_DATA);
            if (!dir.exists()) {
                if (!dir.mkdir()) {
                    Toast.makeText(getApplicationContext(), "Папка" + dir.getPath() + "Не созданна", Toast.LENGTH_SHORT).show();
                }
            }
            String fileList[] = getAssets().list("tessdata");

            for (String fileName : fileList) {
                String pathToDataFile = dir + "/" + fileName;
                if (!(new File(pathToDataFile)).exists()) {
                    InputStream in = getAssets().open("tessdata/" + fileName);
                    OutputStream out = new FileOutputStream(pathToDataFile);
                    byte[] buff = new byte[4096];
                    int len;
                    while ((len = in.read(buff)) != -1) {
                        out.write(buff, 0, len);
                    }
                    in.close();
                    out.close();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }



    private class AsyncOcrTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            String temp = "Что-то пошло не так !";
            bitmapToOCR = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.ARGB_4444);
            try {
                bitmapToOCR = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.ARGB_4444);
                Utils.matToBitmap(mRgba, bitmapToOCR);

                int w = bitmapToOCR.getWidth();
                int h = bitmapToOCR.getHeight();

                Matrix mtx = new Matrix();
                mtx.postRotate(0);
                bitmapToOCR = Bitmap.createBitmap(bitmapToOCR, 0, 0, w, h, mtx, false);

                temp = mTessOCR.getOCRResult(bitmapToOCR);
            } catch (Exception ex) {
                Log.d("Exception", ex.getMessage());
            }
            return temp;
        }


        @Override
        protected void onPostExecute(String foundText) {
            if (foundText == null) {
                return;
            }
            plateText.setText(foundText);
        }
    }


}
