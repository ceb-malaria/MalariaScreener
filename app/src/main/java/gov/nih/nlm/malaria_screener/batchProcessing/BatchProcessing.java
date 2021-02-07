package gov.nih.nlm.malaria_screener.batchProcessing;

import androidx.appcompat.app.AppCompatActivity;
import gov.nih.nlm.malaria_screener.R;
import gov.nih.nlm.malaria_screener.custom.Utils.UtilsCustom;
import gov.nih.nlm.malaria_screener.imageProcessing.SVM_Classifier;
import gov.nih.nlm.malaria_screener.imageProcessing.TensorFlowClassifier;
import gov.nih.nlm.malaria_screener.imageProcessing.ThickSmearProcessor;
import gov.nih.nlm.malaria_screener.imageProcessing.ThinSmearProcessor;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class BatchProcessing extends AppCompatActivity {

    private static final String TAG = "MyDebug";

    public static final int REQUEST_SELECT_FOLDER = 11;

    public static final int REQUEST_SELECT_FOLDER_THICK = 12;

    private Activity context;
    private ProgressDialog inProgress;

    int TN = 0;
    int TP = 0;
    int FN = 0;
    int FP = 0;

    float RV = 6; //resize value

    public static int totalCount = 0; // this is temp variable to record total parasite/infected for the patient

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_batch_processing);

        context = this;

        UtilsCustom.Th = 0.5;
        UtilsCustom.Th_thick = 0.5;
        // read pre-trained SVM data structure & TF deep learning model // put read SVM data structure & TF model here to reduce processing time
        readSVMHandler.sendEmptyMessage(0);

        Button selectFolder = findViewById(R.id.selectFolder_button);
        selectFolder.setOnClickListener(
                new Button.OnClickListener() {
                    public void onClick(View v) {

                        Intent sfIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                        startActivityForResult(sfIntent, REQUEST_SELECT_FOLDER);
                    }
                }
        );

        Button selectFolder_thick = findViewById(R.id.selectFolder_thick_button);
        selectFolder_thick.setOnClickListener(
                new Button.OnClickListener() {
                    public void onClick(View v) {

                        Intent sfIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                        startActivityForResult(sfIntent, REQUEST_SELECT_FOLDER_THICK);
                    }
                }
        );
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        // when return from Gallery event
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_SELECT_FOLDER && resultCode == Activity.RESULT_OK){

            // get dir name
            String rawStr = data.getData().getPath();
            String dirStr = rawStr.substring(rawStr.lastIndexOf(":") + 1);

            final String dirPathStr = Environment.getExternalStorageDirectory().toString() + "/" + dirStr;

            File dir_file = new File(dirPathStr);
            final File[] slideListing = dir_file.listFiles(); // list all images of the dir

            String str1 = getResources().getString(R.string.image_process1);
            String str2 = getResources().getString(R.string.image_process2);
            // show in progress sign
            inProgress = ProgressDialog.show(this, str1, str2, false, false);

            Runnable r = new Runnable() {
                @Override
                public void run() {

                    int image_num = 0;

                    for (File slideFile : slideListing) {

                        totalCount = 0;

                        Log.d(TAG, "slideFile: " + slideFile);

                        final File[] imageListing = slideFile.listFiles(); // list all images of the dir

                        for (File imgFile : imageListing) {

                            //Log.d(TAG, "imgFile: " + imgFile);

                            process_image(imgFile);

                            //writeLogFile_imageList(imgFile.toString(), "thin");

                            image_num +=1;
                        }

                        //get total image number & positive image number
                        /*int imageNum = 0;
                        if (!UtilsCustom.pos_confs_im.isEmpty()){
                            imageNum = UtilsCustom.pos_confs_im.size();
                        }
                        int imageTotal = imageListing.length;

                        float slide_conf = get_slide_conf(imageTotal);

                        // get slide true label
                        int slideLabel = 0;
                        if (slideFile.toString().contains("positive")){
                            slideLabel = 1;
                        } else if (slideFile.toString().contains("negative")){
                            slideLabel = 0;
                        }
                        Log.d(TAG, "slideLabel: " + slideLabel);*/

                        //writeLogFile(slideFile.toString(), slideLabel, slide_conf, imageNum, imageTotal);
                    }

                    Log.d(TAG, "image_num: " + image_num);

                    inProgress.dismiss();
                }
            };

            Thread thread = new Thread(r);
            thread.start();

        } else if (requestCode == REQUEST_SELECT_FOLDER_THICK && resultCode == Activity.RESULT_OK) {

            // get dir name
            String rawStr = data.getData().getPath();
            String dirStr = rawStr.substring(rawStr.lastIndexOf(":") + 1);

            final String dirPathStr = Environment.getExternalStorageDirectory().toString() + "/" + dirStr;

            File dir_file = new File(dirPathStr);
            final File[] slideListing = dir_file.listFiles(); // list all images of the dir

            String str1 = getResources().getString(R.string.image_process1);
            String str2 = getResources().getString(R.string.image_process2);
            // show in progress sign
            inProgress = ProgressDialog.show(this, str1, str2, false, false);

            Runnable r = new Runnable() {
                @Override
                public void run() {

                    int image_num = 0;

                    for (File slideFile : slideListing) {

                        Log.d(TAG, "slideFile: " + slideFile);

                        final File[] imageListing = slideFile.listFiles(); // list all images of the dir

                        for (File imgFile : imageListing) {

                            //Log.d(TAG, "imgFile: " + imgFile);

                            process_image_thick(imgFile);

                            writeLogFile_imageList(imgFile.toString(), "thick");

                            image_num +=1;
                        }

                        //get total image number & positive image number
                        /*int imageNum = 0;
                        if (!UtilsCustom.pos_confs_im.isEmpty()){
                            imageNum = UtilsCustom.pos_confs_im.size();
                        }
                            int imageTotal = imageListing.length;

                        float slide_conf = get_slide_conf(imageTotal);

                        // get slide true label
                        int slideLabel = 0;
                        if (slideFile.toString().contains("positive")){
                            slideLabel = 1;
                        } else if (slideFile.toString().contains("negative")){
                            slideLabel = 0;
                        }
                        Log.d(TAG, "slideLabel: " + slideLabel);*/

                        //writeLogFile(slideFile.toString(), slideLabel, slide_conf, imageNum, imageTotal);
                    }

                    Log.d(TAG, "image_num: " + image_num);

                    inProgress.dismiss();
                }
            };

            Thread thread = new Thread(r);
            thread.start();

            /*final String dirPathStr = Environment.getExternalStorageDirectory().toString() + "/" + dirStr;
            Log.d(TAG, "dirPathStr: " + dirPathStr);

            int slideLabel = 0;
            String slideLabelStr = null;
            if (dirStr.contains("positive")){
                slideLabel = 1;
                slideLabelStr = "positive";
            } else if (dirStr.contains("negative")){
                slideLabel = 0;
                slideLabelStr = "negative";
            }
            Log.d(TAG, "slideLabel: " + slideLabel);

            File dir_file = new File(dirPathStr);
            final File[] imageListing = dir_file.listFiles(); // list all images of the dir

            String str1 = getResources().getString(R.string.image_process1);
            String str2 = getResources().getString(R.string.image_process2);
            // show in progress sign
            inProgress = ProgressDialog.show(this, str1, str2, false, false);

            final String finalSlideLabelStr = slideLabelStr;
            final int finalSlideLabel = slideLabel;
            Runnable r = new Runnable() {
                @Override
                public void run() {

                    for (File imgFile : imageListing) {

                        Log.d(TAG, "imgFile: " + imgFile);

                        process_image(imgFile, finalSlideLabelStr, dirPathStr);
                    }

                    inProgress.dismiss();

                    int pred_label = get_slide_pred();

                    if (finalSlideLabel == 0 && pred_label == 0){
                        TN += 1;
                    } else if(finalSlideLabel == 1 && pred_label == 1){
                        TP += 1;
                    } else if(finalSlideLabel == 1 && pred_label == 0){
                        FN += 1;
                    } else if(finalSlideLabel == 0 && pred_label == 1){
                        FP += 1;
                    }

                    Log.d(TAG, "TN: " + TN + ", TP: " + TP + ", FN: " + FN + ", FP: " + FP);
                }
            };

            Thread imgprocessThread = new Thread(r);
            imgprocessThread.start();*/

        }
    }

    private Mat resizeImage() {

        float ori_height = 2988;
        float ori_width = 5312;
        float cur_height = UtilsCustom.oriSizeMat.height();
        float cur_width = UtilsCustom.oriSizeMat.width();

        float scaleFactor = (float) Math.sqrt((ori_height * ori_width) / (cur_height * cur_width));  // size ratio between 5312/2988 and current images

        RV = 6 / scaleFactor; // SF for the current images
        //Log.d(TAG, "RV: " + RV);

        int width = (int) ((float) UtilsCustom.oriSizeMat.cols() / RV);
        int height = (int) ((float) UtilsCustom.oriSizeMat.rows() / RV);

        Mat resizedMat = new Mat();
        Imgproc.resize(UtilsCustom.oriSizeMat, resizedMat, new Size(width, height), 0, 0, Imgproc.INTER_CUBIC);

        return resizedMat;
    }

    private void process_image(File file){

        UtilsCustom.oriSizeMat = Imgcodecs.imread(file.toString(), Imgcodecs.CV_LOAD_IMAGE_COLOR);
        Imgproc.cvtColor(UtilsCustom.oriSizeMat, UtilsCustom.oriSizeMat, Imgproc.COLOR_BGR2RGB);

        Log.d(TAG, "oriSizeMat: " + UtilsCustom.oriSizeMat);

        //resize image
        Mat resizedMat = resizeImage();

        long startTime_w = System.currentTimeMillis();

        ThinSmearProcessor thinSmearProcessor = new ThinSmearProcessor(getApplicationContext());
        int[] res = thinSmearProcessor.processImage(resizedMat, 0, RV, false, file);

        long endTime_w = System.currentTimeMillis();
        long totalTime_w = endTime_w - startTime_w;
        Log.d(TAG, "One image time: " + totalTime_w);

        Log.d(TAG, "infectedCount: " + res[0]);

        totalCount += res[0];

        // resize result image
        /*int width = (int) ((float) UtilsCustom.canvasBitmap.getWidth() / 6);
        int height = (int) ((float) UtilsCustom.canvasBitmap.getHeight() / 6);
        UtilsCustom.canvasBitmap = Bitmap.createScaledBitmap(UtilsCustom.canvasBitmap, width, height, false);*/

        //SaveResultImage(file.toString(), slideLabel, dirPathStr);

    }

    //private void process_image(File file, String slideLabel, String dirPathStr){
    private void process_image_thick(File file){

        UtilsCustom.oriSizeMat = Imgcodecs.imread(file.toString(), Imgcodecs.CV_LOAD_IMAGE_COLOR);

        Imgproc.cvtColor(UtilsCustom.oriSizeMat, UtilsCustom.oriSizeMat, Imgproc.COLOR_BGR2RGB);

        Log.d(TAG, "oriSizeMat: " + UtilsCustom.oriSizeMat);

        long startTime_w = System.currentTimeMillis();

        ThickSmearProcessor thickSmearProcessor = new ThickSmearProcessor(getApplicationContext(), UtilsCustom.oriSizeMat);
        int[] res = thickSmearProcessor.processImage(file);

        long endTime_w = System.currentTimeMillis();
        long totalTime_w = endTime_w - startTime_w;
        Log.d(TAG, "One image time: " + totalTime_w);

        Log.d(TAG, "parasiteCount: " + res[0]);

        // resize result image
        /*int width = (int) ((float) UtilsCustom.canvasBitmap.getWidth() / 6);
        int height = (int) ((float) UtilsCustom.canvasBitmap.getHeight() / 6);
        UtilsCustom.canvasBitmap = Bitmap.createScaledBitmap(UtilsCustom.canvasBitmap, width, height, false);*/

        //SaveResultImage(file.toString(), slideLabel, dirPathStr);

    }

    // save result image
    private void SaveResultImage(String string, String slideLabel, String dirPathStr) {

        // get slide name
        String slideName = dirPathStr.substring(dirPathStr.lastIndexOf("/") + 1);

        File direct = new File(Environment.getExternalStorageDirectory(), "Test/" + slideLabel + "/" + slideName);

        if (!direct.exists()) {
            direct.mkdirs();
        }

        // get image name
        String imgStr = string.substring(string.lastIndexOf("/") + 1);
        int endIndex = imgStr.lastIndexOf(".");
        String imageName = imgStr.substring(0, endIndex);

        Log.d(TAG, "slideName: " + slideName);
        Log.d(TAG, "imageName: " + imageName);

        File file = new File(new File(Environment.getExternalStorageDirectory(), "Test"),  slideLabel + "/" + slideName + "/" + imageName + "_result.png");

        if (file.exists()) {
            file.delete();
        }

        try {
            FileOutputStream out = new FileOutputStream(file);
            UtilsCustom.canvasBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        UtilsCustom.canvasBitmap.recycle();
        UtilsCustom.oriSizeMat.release();
    }

    /*
     *   Calculate the confidence for current slide. Reset variables.
     * */
    private float get_slide_conf(int imTotal){

        float slide_conf = 0;

        if (!UtilsCustom.pos_confs_im.isEmpty()) {
            for (float conf : UtilsCustom.pos_confs_im) {
                Log.d(TAG, "image confidence: " + conf);
                slide_conf += conf;
            }

            slide_conf = slide_conf / (float) imTotal;

            UtilsCustom.pos_confs_im.clear();
            //Log.d(TAG, "UtilsCustom.confs_im size: " + UtilsCustom.pos_confs_im.size());
        }

        Log.d(TAG, "slide_conf: " + slide_conf);

        return slide_conf;

    }

    // load pre-trained classifier models
    private Handler readSVMHandler = new Handler() {

        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            Runnable r = new Runnable() {
                @Override
                public void run() {

                    long startTime_w = System.currentTimeMillis();

                    // load TF model
                    try {

                        // TF Lite code
                        /*UtilsCustom.tensorFlowClassifier_thin_lite = Classifier_Lite.create(context, Classifier_Lite.Model.FLOAT_THINSMEAR, Classifier_Lite.Device.CPU, 4);
                        UtilsCustom.tensorFlowClassifier_thick_lite = Classifier_Lite.create(context, Classifier_Lite.Model.FLOAT_THICKSMEAR, Classifier_Lite.Device.CPU, 4);*/


                        // thin smear
                        //String modelNameStr_thin = "malaria_thinsmear_44.h5.pb";
                        String modelNameStr_thin = "malaria_thinsmear_44_retrainSudan_20P_4000C_separate.pb";
                        int TF_input_size_thin = 44;
                        //String inputLayerNameStr_thin = "conv2d_20_input";
                        String inputLayerNameStr_thin = "conv2d_1_input";
                        //String outputLayerNameStr_thin = "output_node0";
                        String outputLayerNameStr_thin = "dense_1/Softmax";

                        UtilsCustom.tensorFlowClassifier_thin = TensorFlowClassifier.create(context.getAssets(), modelNameStr_thin, TF_input_size_thin, TF_input_size_thin, inputLayerNameStr_thin, outputLayerNameStr_thin);
                        //UtilsCustom.tensorFlowClassifier_thin = TensorFlowClassifier.create(context.getAssets(), "malaria_thinsmear.h5.pb", UtilsCustom.TF_input_size, "input_2", "output_node0");
                        //UtilsCustom.tfClassifier_lite = TFClassifier_Lite.create(context.getAssets(), "thinSmear_100_quantized.tflite", UtilsCustom.TF_input_size);

                        //thick smear
                        //String modelNameStr_thick = "ThickSmear_3LayerConv_200P_retrainSudan_20P.pb";
                        String modelNameStr_thick = "ThickSmearModel.h5.pb";
                        int TF_input_size_thick = 44;
                        String inputLayerNameStr_thick = "conv2d_1_input";
                        String outputLayerNameStr_thick = "output_node0";
                        //String outputLayerNameStr_thick = "dense_2/Softmax";

                        UtilsCustom.tensorFlowClassifier_thick = TensorFlowClassifier.create(context.getAssets(), modelNameStr_thick, TF_input_size_thick, TF_input_size_thick, inputLayerNameStr_thick, outputLayerNameStr_thick);

                        //UtilsCustom.tensorFlowClassifier_thick = TensorFlowClassifier.create(context.getAssets(), "ThickSmearModel_7LayerConv.h5.pb", UtilsCustom.TF_input_size, "conv2d_1_input", "output_node0");


                        //for blur detection
                        //UtilsCustom.tensorFlowClassifier_fMeasure_thin = TensorFlowClassifier.create(context.getAssets(), "ThinSmear_7LayerConv_fMeasure_115x85.h5.pb", UtilsCustom.TF_input_width, UtilsCustom.TF_input_height, "conv2d_1_input", "output_node0");


                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    //load SVM model
                    UtilsCustom.svm_classifier = SVM_Classifier.create(context);

                    int classNum = 2;
                    for (int index = 0; index < classNum; index++) {
                        UtilsCustom.svm_classifier.readSVMTextFile(index);
                    }

                    long endTime_w = System.currentTimeMillis();
                    long totalTime_w = endTime_w - startTime_w;
                    Log.d(TAG, "Read Classifier Time: " + totalTime_w);

                }
            };

            Thread readSVMThread = new Thread(r);
            readSVMThread.start();

        }
    };

    private void writeLogFile(String string, int slideLabel, float slideConf, int imageNum, int imageTotal) {

        File textFile = null;

        try {
            textFile = createTextFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (textFile != null) {
            FileOutputStream outText = null;

            try {

                outText = new FileOutputStream(textFile, true);

                if (textFile.length() == 0) {
                    outText.write(("ImageName,y_true,y_score,ImageNum/Total,parasiteTotal").getBytes());
                    outText.write(("\n").getBytes());
                }

                // get slide name
                String slideNameStr = string.substring(string.lastIndexOf("/") + 1);

                outText.write((slideNameStr + "," + slideLabel + "," + slideConf + "," + imageNum + "/" + imageTotal + "," + totalCount).getBytes());
                //outText.write((imageName + "," + WB + "," + processingTime).getBytes());
                outText.write(("\n").getBytes());

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (outText != null) {
                        outText.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private File createTextFile() throws IOException {

        File imgFile = new File(Environment.getExternalStorageDirectory(), "p_level_thin_0.5_retrain_20P.txt");
        if (!imgFile.exists()) {
            imgFile.createNewFile();
        }

        return imgFile;
    }

    // write image list
    private void writeLogFile_imageList(String im_path, String type) {

        File textFile = null;

        try {
            textFile = createTextFile_imageList(type);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (textFile != null) {
            FileOutputStream outText = null;

            try {

                outText = new FileOutputStream(textFile, true);

                if (textFile.length() == 0) {
                    outText.write(("SlideName,ImageName,SlideLabel").getBytes());
                    outText.write(("\n").getBytes());
                }

                // get slide name
                String[] parts = im_path.split("/");
                Log.d(TAG, "slideName: " + parts[parts.length-2]);
                String slideNameStr = parts[parts.length-2];

                // get image name
                String imgStr = im_path.substring(im_path.lastIndexOf("/") + 1);
                int endIndex = imgStr.lastIndexOf(".");
                String imageName = imgStr.substring(0, endIndex);
                Log.d(TAG, "imageName: " + imageName);

                // get slide true label
                int slideLabel = 0;
                if (im_path.contains("positive")){
                    slideLabel = 1;
                } else if (im_path.contains("negative")){
                    slideLabel = 0;
                }

                outText.write((slideNameStr + "," + imageName + "," + slideLabel).getBytes());
                //outText.write((imageName + "," + WB + "," + processingTime).getBytes());
                outText.write(("\n").getBytes());

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (outText != null) {
                        outText.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private File createTextFile_imageList(String smearTypeStr) throws IOException {

        File imgFile = new File(Environment.getExternalStorageDirectory(), "imageList_" + smearTypeStr + "_for_20P.txt");
        if (!imgFile.exists()) {
            imgFile.createNewFile();
        }

        return imgFile;
    }


}
