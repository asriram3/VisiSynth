package hack.myapplication;

/**
 * @author Jose Davis Nidhin
 * Aditya Sriram
 */

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import java.io.IOException;
import java.util.List;

class Preview extends ViewGroup implements SurfaceHolder.Callback, Camera.PreviewCallback {
    private final String TAG = "Preview";

    imageProcess imgpro;
    Context ctx;

    SurfaceView mSurfaceView;
    SurfaceHolder mHolder;
    Size mPreviewSize;
    List<Size> mSupportedPreviewSizes;
    Camera mCamera;

    Preview(Context context, SurfaceView sv) {
        super(context);
        ctx = context;
        mSurfaceView = sv;
//        addView(mSurfaceView);

        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void setCamera(Camera camera) {
        mCamera = camera;
        if (mCamera != null) {
            mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
            requestLayout();

            // get Camera parameters
            Camera.Parameters params = mCamera.getParameters();

            List<String> focusModes = params.getSupportedFocusModes();
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                // set the focus mode
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                // set Camera parameters
            }
            mCamera.setParameters(params);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // We purposely disregard child measurements because act as a
        // wrapper to a SurfaceView that centers the camera preview instead
        // of stretching it.
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        setMeasuredDimension(width, height);

        if (mSupportedPreviewSizes != null) {
            mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width, height);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (changed && getChildCount() > 0) {
            final View child = getChildAt(0);

            final int width = r - l;
            final int height = b - t;

            int previewWidth = width;
            int previewHeight = height;
            if (mPreviewSize != null) {
                previewWidth = mPreviewSize.width;
                previewHeight = mPreviewSize.height;
            }

            // Center the child SurfaceView within the parent.
            if (width * previewHeight > height * previewWidth) {
                final int scaledChildWidth = previewWidth * height / previewHeight;
                child.layout((width - scaledChildWidth) / 2, 0,
                        (width + scaledChildWidth) / 2, height);
            } else {
                final int scaledChildHeight = previewHeight * width / previewWidth;
                child.layout(0, (height - scaledChildHeight) / 2,
                        width, (height + scaledChildHeight) / 2);
            }
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, acquire the camera and tell it where
        // to draw.
        imgpro = new imageProcess(ctx);
        try {
            if (mCamera != null) {
                mCamera.setPreviewCallback(this);
                mCamera.setPreviewDisplay(holder);

            }
        } catch (IOException exception) {
            Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return, so stop the preview.
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }


    private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    byte[] pic;
    int pic_size;
    Bitmap picframe;
    public void onPreviewFrame(byte[] data, Camera camera){
        Log.d("TAG", "frame1 "+data.length);
        Camera.Size previewSize = mCamera.getParameters().getPreviewSize();
        YuvImage yuvimage=new YuvImage(data, ImageFormat.NV21, previewSize.width, previewSize.height, null);

        // Convert to Bitmap
        final double [][] imgmat = imgpro.BufferedYUVImage2Mat(yuvimage.getYuvData(),
                yuvimage.getWidth(), yuvimage.getHeight(), 640, 480);

        List<Double> ld = imgpro.AnalyzeMat(imgmat, 0.6);

        String logline = "points:";
        for(Double p : ld)
            logline += " " + (1-p);
        Log.d("TAG", logline);
        double [] f = new double[ld.size()];
        for(int i = 0; i < f.length; i ++)
            f[i] = Math.pow(2.0, ld.get(i) * 2) * 440.0;
        play(f);
    }

    private final double duration = 0.1; // seconds
    private final int sampleRate = 8000;
    private final int numSamples = (int)(duration * sampleRate);
    private final double sample[] = new double[numSamples];


    byte[] genTone(double [] freq){

        byte generatedSnd[] = new byte[2 * numSamples];
        // fill out the array
        for (int i = 0; i < numSamples; ++i) {
            sample[i] = 0;
        }
        for (int c = 0; c < freq.length; c ++) {
            double ncycles = Math.round(duration * freq[c]);
            double freq_corrected = ncycles / duration;
            for (int i = 0; i < numSamples; ++i) {
                sample[i] += Math.sin(2 * Math.PI * i / (sampleRate / freq_corrected)) * 0.1;
            }
        }

        // convert to 16 bit pcm sound array
        // assumes the sample buffer is normalised.
        int idx = 0;
        for (final double dVal : sample) {
            // scale to maximum amplitude
            final short val = (short) ((dVal * 32767));
            // in 16 bit wav PCM, first byte is the low order byte
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);

        }
        return generatedSnd;
    }

    AudioTrack audioTrack;

//    public void playSound(int freq){
//        // Get a handler that can be used to post to the main thread
//        freqOfTone = freq;
//        Handler mainHandler = new Handler(ctx.getMainLooper());
//
//        Runnable myRunnable = new Runnable() {
//            @Override
//            public void run() {
//                Log.d("LMAO", ""+generatedSnd.length);
//                final AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
//                        sampleRate, AudioFormat.CHANNEL_OUT_MONO,
//                        AudioFormat.ENCODING_PCM_16BIT, generatedSnd.length,
//                        AudioTrack.MODE_STREAM);
//                audioTrack.write(generatedSnd, 0, generatedSnd.length);
//                audioTrack.play();
//
//            } // This is your code
//        };
//        mainHandler.post(myRunnable);
//    }
//    Handler handler = new Handler();

    public void play(final double [] freq){
        // Use a new tread as this can take a while
        byte[] generatedSnd = genTone(freq);
        if(audioTrack==null){
            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, 5*generatedSnd.length, AudioTrack.MODE_STREAM);
            audioTrack.write(generatedSnd, 0, generatedSnd.length);
            Thread thread = new Thread(new Runnable() {
                public void run() {
                    audioTrack.play();
                }
            });
            thread.start();
        }
        else {
            audioTrack.write(generatedSnd, 0, generatedSnd.length);
        }

    }

//    public static Bitmap bitmapFromArray(double[][] pixels2d){
//        int width = pixels2d.length;
//        int height = pixels2d[0].length;
//        int[] pixels = new int[width * height];
//        int pixelsIndex = 0;
//        for (int i = 0; i < width; i++)
//        {
//            for (int j = 0; j < height; j++)
//            {
//                int p = (int)pixels2d[i][j];
//                if(p>255){p=255;}
//                pixels[pixelsIndex] = p;
//                pixelsIndex ++;
//            }
//        }
//        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
//    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        if(mCamera != null) {
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewSize(640, 480);
            requestLayout();
            parameters.setJpegQuality(50);
            parameters.setPictureFormat(ImageFormat.JPEG);
            mCamera.setPreviewCallback(this);

            imgpro = new imageProcess(ctx);

            //set color efects to none
            parameters.setColorEffect(Camera.Parameters.EFFECT_NONE);

            //set antibanding to none
            if (parameters.getAntibanding() != null) {
                parameters.setAntibanding(Camera.Parameters.ANTIBANDING_OFF);
            }

            // set white ballance
            if (parameters.getWhiteBalance() != null) {
                parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_CLOUDY_DAYLIGHT);
            }

            //set flash
            if (parameters.getFlashMode() != null) {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            }

            //set zoom
            if (parameters.isZoomSupported()) {
                parameters.setZoom(0);
            }

            //set focus mode
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);

            mCamera.setDisplayOrientation(90);

            mCamera.setParameters(parameters);
            mCamera.startPreview();
        }
    }

}

//imageProcess.Normalize(imgmat);
//        byte [] imgbyte = new byte[640 * 480 * 4];
//        for(int i = 0; i < 640; i ++)
//            for(int j = 0; j < 480; j ++) {
//                imgbyte[i * 4 + j * 640 * 4] = (byte)0;
//                imgbyte[(i * 4 + j * 640 * 4)+1] = (byte)imgmat[i][j];
//                imgbyte[(i * 4 + j * 640 * 4)+2] = (byte)imgmat[i][j];
//                imgbyte[(i * 4 + j * 640 * 4)+3] = (byte)imgmat[i][j];
//            }
//        final Bitmap bmp = BitmapFactory.decodeByteArray(imgbyte, 0, imgbyte.length);
//
//        final Bitmap bmp = bitmapFromArray(imgmat);
//        final ImageView imgView = (ImageView)findViewById(R.id.imageView);
//
//        if(bmp!=null && imgView!=null)
//            new Handler(Looper.getMainLooper()).post(new Runnable() {
//            @Override
//            public void run() {
//                Log.d("UI thread", "I am the UI thread");
//                Log.d("UI thread", "" + bmp.getByteCount());
//                imgView.setImageBitmap(bmp);
//            }
//        });