package com.example.imagepro;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class CameraActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2{
    private static final String TAG="MainActivity";



    private String output;
    private Mat mRgba;
    private Mat mGray;
    private CameraBridgeViewBase mOpenCvCameraView;


    private ImageView translate_button;
    private ImageView take_picture_button;
    private ImageView show_image_button;

    private ImageView current_image;
    private TextView textView;


    private TextRecognizer textRecognizer;
    private String Camera_or_recognizeText="camera";

    //the bitmap is store the preiviewing image
    private Bitmap bitmap=null;

    private BaseLoaderCallback mLoaderCallback =new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status){
                case LoaderCallbackInterface
                        .SUCCESS:{
                    Log.i(TAG,"OpenCv Is loaded");
                    mOpenCvCameraView.enableView();
                }
                default:
                {
                    super.onManagerConnected(status);

                }
                break;
            }
        }
    };

    public CameraActivity(){
        Log.i(TAG,"Instantiated new "+this.getClass());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        int MY_PERMISSIONS_REQUEST_CAMERA=0;
        // if camera permission is not given it will ask for it on device
        if (ContextCompat.checkSelfPermission(CameraActivity.this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(CameraActivity.this, new String[] {Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA);
        }

        setContentView(R.layout.activity_camera);

        mOpenCvCameraView=(CameraBridgeViewBase) findViewById(R.id.frame_Surface);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);



        textView=findViewById(R.id.textview);
        //initially textview is kept invisible

        textView.setVisibility(View.GONE);
        current_image=findViewById(R.id.current_image);
        current_image.setVisibility(View.GONE);

        translate_button=findViewById(R.id.translate);
        show_image_button=findViewById(R.id.image_button);
        take_picture_button=findViewById(R.id.take_picture_button);


        //load text recognition model
        textRecognizer = TextRecognition.getClient( new DevanagariTextRecognizerOptions.Builder().build());

        //capture image function
/*        if we are using camera and clicking the take_picturebutton
          we do the following :-
          1.Chnage the buttoncolor to Gray
          2.Convert current mRGBA frame to bitmap and store it
          3.mRGBA is define at the top
          we are rotating the frame   to get portrait mode image
*
*/
        take_picture_button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(motionEvent.getAction()==MotionEvent.ACTION_DOWN){
                    return true;
                }
                if(motionEvent.getAction()==MotionEvent.ACTION_UP){

                    if(Camera_or_recognizeText=="camera"){
                        take_picture_button.setColorFilter(Color.DKGRAY);
                        Mat a = mGray.t();
                        Core.flip(a,mRgba,1);
                        a.release();
                        bitmap=Bitmap.createBitmap(mRgba.cols(),mRgba.rows(),Bitmap.Config.ARGB_8888);
                        Utils.matToBitmap(mRgba,bitmap);


                        //when take_picture_button is clicked and we have saved the bitmap , we stop the camerview
                        mOpenCvCameraView.disableView();

                        //now change the string to recogniser mode
                        Camera_or_recognizeText="recognizeText";
                    }

                    return true;
                }

                return false;
            }
        });


        //translate image button

        // when the translate button is pressed change color of the  button


        translate_button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(motionEvent.getAction()==MotionEvent.ACTION_DOWN){
                   translate_button.setColorFilter(Color.DKGRAY);
                    return true;
                }
                if(motionEvent.getAction()==MotionEvent.ACTION_UP){
                    translate_button.setColorFilter(Color.WHITE);
                    if(Camera_or_recognizeText=="recognizeText")
                    {
                        //if the Camera_or_recognizeText is recognizeText that means we have to
                        //take the image fromthe bitmap and pass it for translation
                        //Before that we have to convert bitmap to InputImage used by ML kit

                        textView.setVisibility(View.VISIBLE);

                        InputImage image =InputImage.fromBitmap(bitmap,0);

                        //recognise text

                        Task<Text> result = textRecognizer.process(image)
                                .addOnSuccessListener(new OnSuccessListener<Text>() {
                                    @Override
                                    public void onSuccess(Text text) {
                                        // onSuccess , add text to textview
                                        textView.setText(text.getText());
                                        output=text.getText();
                                        Log.d("CameraActivity","Out"+output);
                                        Intent intent = new Intent(CameraActivity.this,MainActivity.class);
                                        intent.putExtra("text",output);
                                        setResult(RESULT_OK, intent);
                                        finish();

                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {

                                    }
                                });
                    }
                    else{
                        Toast.makeText(CameraActivity.this,"Please take a photo",Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }

                 return false;
            }
        });



        //show_image button


        show_image_button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(motionEvent.getAction()==MotionEvent.ACTION_DOWN){
                    show_image_button.setColorFilter(Color.DKGRAY);
                    return true;
                }
                if(motionEvent.getAction()==MotionEvent.ACTION_UP){
                    show_image_button.setColorFilter(Color.WHITE);

                    if(Camera_or_recognizeText=="recognizeText")
                    {

                    }else{
                        Toast.makeText(CameraActivity.this,"Please take a photo",Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }

                return false;
            }
        });





    }



    @Override
    protected void onResume() {
        super.onResume();
        if (OpenCVLoader.initDebug()){
            //if load success
            Log.d(TAG,"Opencv initialization is done");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        else{
            //if not loaded
            Log.d(TAG,"Opencv is not loaded. try again");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0,this,mLoaderCallback);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mOpenCvCameraView !=null){
            mOpenCvCameraView.disableView();
        }
    }

    public void onDestroy(){
        super.onDestroy();
        if(mOpenCvCameraView !=null){
            mOpenCvCameraView.disableView();
        }

    }

    public void onCameraViewStarted(int width ,int height){
        mRgba=new Mat(height,width, CvType.CV_8UC4);
        mGray =new Mat(height,width,CvType.CV_8UC1);
    }
    public void onCameraViewStopped(){
        mRgba.release();
    }
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame){
        mRgba=inputFrame.rgba();
        mGray=inputFrame.gray();

        return mRgba;

    }

}