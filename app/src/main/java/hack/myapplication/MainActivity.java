package hack.myapplication;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    Camera camera;
    private static final String TAG = "MainActivity";
    Preview preview;
    ImageView imgv;
    Activity act;
    Context ctx;
    TextView txt;
    long a;



    private int findFrontFacingCamera() {
        // Search for the front facing camera
        int numberOfCameras = Camera.getNumberOfCameras();
        int cameraId = 0;
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                cameraId = i;
                Log.d(TAG, "Front: " + i);
                break;
            }
        }
        return cameraId;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ctx = this;
        act = this;
        preview = new Preview(this,(SurfaceView)findViewById(R.id.surfaceView));
        preview.setLayoutParams(new LinearLayout.LayoutParams(0, 0));
        ((RelativeLayout)findViewById(R.id.layout)).addView(preview);
        preview.setKeepScreenOn(true);

        imgv = (ImageView)findViewById(R.id.imageView);

        txt = (TextView)findViewById(R.id.txtv);

        a=0;
        txt.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                a = System.currentTimeMillis();
                camera.takePicture(null, null, jpegCallback);
            }
        });

        Toast.makeText(ctx, getString(R.string.take_photo_help), Toast.LENGTH_LONG).show();

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });
    }

    @Override
    protected void onPause(){
        if(camera!=null){
            camera.stopPreview();
            preview.setCamera(null);
            camera.release();
            camera=null;
        }
        super.onPause();
    }

    @Override
    protected  void onResume(){
        super.onResume();
        int numCams = Camera.getNumberOfCameras();
        if(numCams > 0){
            try{
                Log.d(TAG, "Trying to open camera...");
                if(camera != null) {
                    camera.stopPreview();
                    camera.release();
                    Log.d(TAG, "Released");
                }
                camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
                camera.startPreview();
                preview.setCamera(camera);
            } catch (RuntimeException ex){
                Toast.makeText(ctx, getString(R.string.camera_not_found), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void resetCam() {
        camera.startPreview();
        preview.setCamera(camera);
    }


    private void refreshGallery(File file) {
        Intent mediaScanIntent = new Intent( Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(Uri.fromFile(file));
        sendBroadcast(mediaScanIntent);
    }

    ShutterCallback shutterCallback = new ShutterCallback() {
        public void onShutter() {
            //			 Log.d(TAG, "onShutter'd");
        }
    };

    PictureCallback rawCallback = new PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            //			 Log.d(TAG, "onPictureTaken - raw");
            Toast.makeText(ctx, "RAW CALLBACK", Toast.LENGTH_LONG).show();
//            String str = "" + data.length;
//            Log.d(TAG, str);
        }
    };

    PictureCallback jpegCallback = new PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            //new SaveImageTask().execute(data);
            long b = System.currentTimeMillis()-a;

            Log.d(TAG, "onPictureTaken - jpeg");
            String str = "Data: " + data.length;
            Log.d(TAG, str);
            // Convert to JPG
            Camera.Size previewSize = camera.getParameters().getPreviewSize();
            YuvImage yuvimage=new YuvImage(data, ImageFormat.NV21, previewSize.width, previewSize.height, null);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            yuvimage.compressToJpeg(new Rect(0, 0, previewSize.width, previewSize.height), 100, baos);
            byte[] jdata = baos.toByteArray();

            // Convert to Bitmap
            Bitmap bmp = BitmapFactory.decodeByteArray(jdata, 0, jdata.length);

            str = "BMP: " + bmp.getPixel(35, 84);
            Log.d(TAG, str);


            str = "Time: " + b;
            Log.d(TAG, str);
            resetCam();
        }
    };

    private class SaveImageTask extends AsyncTask<byte[], Void, Void> {

        @Override
        protected Void doInBackground(byte[]... data) {
            FileOutputStream outStream = null;

            // Write to SD Card
            try {
                File sdCard = Environment.getExternalStorageDirectory();
                File dir = new File (sdCard.getAbsolutePath() + "/camtest");
                dir.mkdirs();

                String fileName = String.format("%d.jpg", System.currentTimeMillis());
                File outFile = new File(dir, fileName);

                outStream = new FileOutputStream(outFile);
                outStream.write(data[0]);
                outStream.flush();
                outStream.close();

                Log.d(TAG, "onPictureTaken - wrote bytes: " + data.length + " to " + outFile.getAbsolutePath());

                refreshGallery(outFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
            }
            return null;
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}