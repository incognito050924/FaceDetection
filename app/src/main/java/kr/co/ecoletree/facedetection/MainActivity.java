package kr.co.ecoletree.facedetection;

import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity
        implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "Face Detection";

    private final Semaphore cameraFrameAcquirer = new Semaphore(1);
    private Handler mHandler;
    private long registeredAt;
    private final long LATENCY = TimeUnit.SECONDS.toMillis(10); // 10초 동안 얼굴 검출 안 되면 사진 캡쳐
    private final int CAPTURE_TOKEN = 6208415; // Message Queue 용 Callbacks` Token
    private final Runnable delayCapture = new Runnable() {
        @Override
        public void run() {
            // latency 만큼 대기 <== UIThread 에서 sleep 불가. 새로운 Thread에서 sleep -> handler에 post 하는 방식으로 사용해야 할 듯...
//            try { TimeUnit.SECONDS.sleep(10); }
//            catch (InterruptedException e) { Thread.currentThread().interrupt(); }

//            SystemClock.sleep(LATENCY);

            // 대기 후 사진 캡쳐.
            Toast.makeText(getApplicationContext(), "테스트::캡쳐됨.\nElapsed: " + TimeUnit.MILLISECONDS.toSeconds(SystemClock.uptimeMillis() - registeredAt) + " Sec(s).", Toast.LENGTH_LONG).show();
            // saveCapture(); // 사진 캡쳐

            // 다음 루프를 위해 다시 MessageQueue에 등록
            //mHandler.postAtTime(this, CAPTURE_TOKEN, SystemClock.uptimeMillis());
            registeredAt = SystemClock.uptimeMillis();
            mHandler.postDelayed(this, LATENCY);
        }
    };

    private CameraBridgeViewBase mOpenCvCameraView;
    private Mat matInput;
    private Mat matResult;

    private final String CASCADE_FILE_NAME_1 = "haarcascade_profileface.xml";
    private final String CASCADE_FILE_NAME_2 = "haarcascade_frontalface_alt2.xml";
    public long cascadeClassifier_face_1 = 0;
    public long cascadeClassifier_face_2 = 0;

    private final File IMAGE_DIR = new File(Environment.getExternalStorageDirectory() + "/captures/");

    public native long loadCascade(final String cascadeFileName );
    public native int detect1(final long cascadeClassifier_face, final long matAddrInput, final long matAddrResult);
    public native int detect2(final long cascadeClassifier_1, final long cascadeClassifier_2, final long matAddrInput, final long matAddrResult);

    static {
        System.loadLibrary("opencv_java3");
        System.loadLibrary("native-lib");
    }

    private void copyFile(final String filename) {
        final String baseDir = Environment.getExternalStorageDirectory().getPath();
        final String pathDir = baseDir + File.separator + filename;

        final AssetManager assetManager = this.getAssets();

        try (
                InputStream inputStream = assetManager.open(filename);
                OutputStream outputStream = new FileOutputStream(pathDir)
        ) {
            Log.d( TAG, "copyFile :: 다음 경로로 파일복사 "+ pathDir);

            final byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            outputStream.flush();
        } catch (IOException e) {
            Log.d(TAG, "copyFile :: 파일 복사 중 예외 발생 "+e.toString() );
        }

    }

    private void read_cascade_file() {
        copyFile(CASCADE_FILE_NAME_1);
        copyFile(CASCADE_FILE_NAME_2);

        Log.d(TAG, "read_cascade_file:");
        cascadeClassifier_face_1 = loadCascade(CASCADE_FILE_NAME_1);
        cascadeClassifier_face_2 = loadCascade(CASCADE_FILE_NAME_2);
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(final int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    mOpenCvCameraView.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //퍼미션 상태 확인
            if (!hasPermissions(PERMISSIONS)) {
                //퍼미션 허가 안 되어있다면 사용자에게 요청
                requestPermissions(PERMISSIONS, PERMISSIONS_REQUEST_CODE);
            } else {
                read_cascade_file();
            }
        } else {
            read_cascade_file();
        }

        mOpenCvCameraView = (CameraBridgeViewBase)findViewById(R.id.activity_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setCameraIndex(1); // front-camera(1),  back-camera(0)
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);

        //initializeHandler(); // Handler를 별도의 Thread 에서 실행.
        mHandler = new Handler();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();

        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "onResume :: Internal OpenCV library not found.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "onResum :: OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();

        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        // 최초 태스크 메시지 등록
//        mHandler.postAtTime(delayCapture, CAPTURE_TOKEN, SystemClock.uptimeMillis());
        mHandler.postDelayed(delayCapture, LATENCY);
    }

    @Override
    public void onCameraViewStopped() {
        // 등록 대기 중인 태스크 메시지 제거
        //mHandler.removeCallbacksAndMessages(CAPTURE_TOKEN);
        mHandler.removeCallbacks(delayCapture);
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        boolean isExpired = false;
        try {
            cameraFrameAcquirer.acquire();

            matInput = inputFrame.rgba();

            if (matResult == null) {
                matResult = new Mat(matInput.rows(), matInput.cols(), matInput.type());
            }

            Core.flip(matInput, matInput, 1);
            int detectedNum = detect2(cascadeClassifier_face_1, cascadeClassifier_face_2, matInput.getNativeObjAddr(), matResult.getNativeObjAddr());

            if (detectedNum > 0) {
                // 얼굴 검출 시 MessageQueue에 등록된 Runnable 갱신(딜레이 초기화 시키기 위해)
//                mHandler.removeCallbacks(delayCapture, CAPTURE_TOKEN);
//                mHandler.postAtTime(delayCapture, CAPTURE_TOKEN, SystemClock.uptimeMillis());
                mHandler.removeCallbacks(delayCapture); // 기존에 등록되어 대기 중인 Runnable 제거
                registeredAt = SystemClock.uptimeMillis();
                mHandler.postDelayed(delayCapture, LATENCY); // 새로운 Runnable 등록
            }

            isExpired = true;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (isExpired) cameraFrameAcquirer.release();
        }

        return matResult;
    }

    private boolean existsImageDir() {
        return IMAGE_DIR.mkdirs();
    }

    private String genFilename(final String ext) {
        final SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.KOREA);
        return df.format(new Date()) + "." + ext;
    }

    /**
     * 이미지 저장.
     */
    private void saveCapture() {
        if (existsImageDir()) {
            final File img = new File(IMAGE_DIR, genFilename("png"));

            Imgproc.cvtColor(matResult, matResult, Imgproc.COLOR_BGR2RGB, 4);
            Imgcodecs.imwrite(img.getPath(), matResult);

            // 미디어 라이브러리에 이미지 파일 추가
            final Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            mediaScanIntent.setData(Uri.fromFile(img));
            sendBroadcast(mediaScanIntent);
        }
    }


    // 퍼미션 관련
    static final int PERMISSIONS_REQUEST_CODE = 1000;
    final String[] PERMISSIONS  = {
        "android.permission.CAMERA"
        , "android.permission.WRITE_EXTERNAL_STORAGE"
    };


    private boolean hasPermissions(final String[] permissions) {
        //스트링 배열에 있는 퍼미션들의 허가 상태 여부 확인
        for (String perms : permissions){
            if (ContextCompat.checkSelfPermission(this, perms) == PackageManager.PERMISSION_DENIED)
                return false;
        }
        return true;
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch(requestCode){

            case PERMISSIONS_REQUEST_CODE:
                if (grantResults.length > 0) {
                    boolean cameraPermissionAccepted = grantResults[0]
                            == PackageManager.PERMISSION_GRANTED;

                    boolean writePermissionAccepted = grantResults[1]
                            == PackageManager.PERMISSION_GRANTED;

                    if (!cameraPermissionAccepted || !writePermissionAccepted) {
                        showDialogForPermission("앱을 실행하려면 퍼미션을 허가하셔야합니다.");
                        return;
                    } else {
                        read_cascade_file();
                    }
                }
            break;
        }
    }


    @TargetApi(Build.VERSION_CODES.M)
    private void showDialogForPermission(String msg) {

        AlertDialog.Builder builder = new AlertDialog.Builder( MainActivity.this);
        builder.setTitle("알림");
        builder.setMessage(msg);
        builder.setCancelable(false);
        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id){
                requestPermissions(PERMISSIONS, PERMISSIONS_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                finish();
            }
        });
        builder.create().show();
    }


}