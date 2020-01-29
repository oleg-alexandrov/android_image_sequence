package android_image_sequence.android_image_sequence;

import android.os.Environment;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.Runnable;
import java.lang.Thread;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PictureCallback;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

public class AndroidImageSequence extends Activity  {
    private Button capture, switchCamera;
    private Context myContext;
    private LinearLayout cameraPreview;
    private Thread pictureThread;
    private int numPics;
    
    // These will be used by the picture thread
    public Camera mCamera;
    public CameraPreview mPreview;
    public  PictureCallback mPicture;

    // Make these volatile so that the compiler does not optimize them away
    // (not sure about that, but it is safer that way).
    public volatile boolean inUse;
    public volatile boolean doStart;
    public volatile boolean doStop;
    
    private static String getPictureDir() {
        File sdDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return sdDir.toString();
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        myContext = this;
            
        pictureThread = new Thread(new PictureThread(this)); 
        pictureThread.start();
        initialize();
    }
        
    private int findFrontFacingCamera() {
        int cameraId = -1;
        // Search for the front facing camera
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            CameraInfo info = new CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
                cameraId = i;
                break;
            }
        }
        return cameraId;
    }

    public void onResume() {
        super.onResume();
        
        if (!hasCamera(myContext)) {
            Toast toast = Toast.makeText(myContext, "Sorry, your phone does not have a camera!", Toast.LENGTH_LONG);
            toast.show();
            finish();
        }
        
        releaseCamera();
        
        if (mCamera == null) {
            //if the front facing camera does not exist
            if (findFrontFacingCamera() < 0) {
                Toast.makeText(this, "No front facing camera found.", Toast.LENGTH_LONG).show();
                switchCamera.setVisibility(View.GONE);
            }			
            mCamera = Camera.open(findFrontFacingCamera());
            mPicture = getPictureCallback();
            mPreview.refreshCamera(mCamera);
        }
    }

    public void initialize() {
        inUse   = false;
        doStart = false;
        doStop  = false;
        numPics = 0;
        
        cameraPreview = (LinearLayout) findViewById(R.id.camera_preview);
        mPreview = new CameraPreview(myContext, mCamera);
        cameraPreview.addView(mPreview);
            
        capture = (Button) findViewById(R.id.button_capture);
        capture.setOnClickListener(captureListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
    }

    private boolean hasCamera(Context context) {
        // Check if the device has camera
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            return true;
        } else {
            return false;
        }
    }

    private PictureCallback getPictureCallback() {
        PictureCallback picture = new PictureCallback() {

                @Override
                    public void onPictureTaken(byte[] data, Camera camera) {

                    // Make a new picture file
                    File pictureFile = getOutputMediaFile();
				
                    if (pictureFile == null) {
                        return;
                    }
                    
                    try {
                        //write the file
                        FileOutputStream fos = new FileOutputStream(pictureFile);
                        fos.write(data);
                        fos.close();
                        Toast toast = Toast.makeText(myContext, "Picture saved: " + pictureFile,
                                                     Toast.LENGTH_LONG);
                        toast.show();
                        
                    } catch (FileNotFoundException e) {
                    } catch (IOException e) {
                    }
                    
                    // Protect variables used in the other thread
                    synchronized(this){
                        
                        numPics = numPics + 1;

                        // Stop after taking the desired number of pictures
                        if (numPics > 10) {
                            doStop = true;
                        }
                        
                        mPreview.refreshCamera(mCamera);

                        // Flag that we are done processing the picture
                        inUse = false;
                    }
                }
            };
        return picture;
    }

    OnClickListener captureListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                synchronized(this){
                    doStart = true;
                }
            }
        };
    
    // make picture and save to a folder
    private static File getOutputMediaFile() {
        //make a new file directory inside the "sdcard" folder
        File mediaStorageDir = new File(getPictureDir());
            
        if (!mediaStorageDir.exists()) {
            //if you cannot make this folder return
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }
            
        //take the current timestamp
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        //and make a media file
        File mediaFile = new File(mediaStorageDir.getPath() + File.separator
                                  + "IMG_" + timeStamp + ".jpg");

        return mediaFile;
    }
    
    private void releaseCamera() {
        // stop and release camera
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }
}