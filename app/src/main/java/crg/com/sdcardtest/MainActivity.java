package crg.com.sdcardtest;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
//    private static final String  VOCODER_DOWNLOAD_URL="http://192.168.13.23:8088/NeoCloudService/download_upgrade_file_image/vocoder_upgrade.bin";
//    private static final String  BP_DOWNLOAD_URL="http://192.168.13.23:8088/NeoCloudService/download_upgrade_file_image/bp_upgrade.hex";
    private static final String  VOCODER_DOWNLOAD_URL="http://10.0.0.239:8088/NeoCloudService/download_upgrade_file_image/vocoder_upgrade.bin";
    private static final String  BP_DOWNLOAD_URL="http://10.0.0.239:8088/NeoCloudService/download_upgrade_file_image/bp_upgrade.hex";
    private static final String  DOWNLOAD_UPGRADE_FILE_PATH=Environment.getExternalStorageDirectory().getPath()+"/bp_upgrade_image";

    public static final String BP_NAME = "bp";
    public static final String VOCODER_NAME = "vocoder";

    private Button mLteButton;
    private Button mBpButton;
    private Button mVocoderButton;
    private Button mDownloadUpgradeButton;
    private Button mButton;
    public static final int SUCCESS = 10;
    public static final int FAIL = 11;
    private String downloadFailReason;
//    DownloadUpgradeFile downloadUpgradeFile = null;
    @Override
    public void onClick(View view) {
        File bpDestFile = new File(DOWNLOAD_UPGRADE_FILE_PATH + "/bp_upgrade.hex");
        File vocoderDestFile = new File(DOWNLOAD_UPGRADE_FILE_PATH + "/vocoder_upgrade.bin");
        if (bpDestFile.exists()){
            Toast.makeText(MainActivity.this, "BP模块升级文件已经存在", Toast.LENGTH_SHORT).show();
        }else {
            DownloadUpgradeFile downloadUpgradeFile = new DownloadUpgradeFile();
//            downloadUpgradeFile.execute(BP_DOWNLOAD_URL, BP_NAME);
            downloadUpgradeFile.executeOnExecutor(Executors.newCachedThreadPool(), BP_DOWNLOAD_URL, BP_NAME);
        }
        if (vocoderDestFile.exists()){
            Toast.makeText(MainActivity.this, "声码器升级文件已经存在", Toast.LENGTH_SHORT).show();
        }else {
            DownloadUpgradeFile downloadUpgradeFile = new DownloadUpgradeFile();
//            downloadUpgradeFile.execute(BP_DOWNLOAD_URL, VOCODER_NAME);
            downloadUpgradeFile.executeOnExecutor(Executors.newCachedThreadPool(), VOCODER_DOWNLOAD_URL, VOCODER_NAME);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        String sdcardgetPath = Environment.getExternalStorageDirectory().getPath();
        String sdcardgetAbsolutePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        Log.d("sdcardgetPath", "getPath()+++sdcardgetPath: " + sdcardgetPath);
        Log.d("sdcardgetAbsolutePath", "getPath()+++sdcardgetAbsolutePath: " + sdcardgetAbsolutePath);
        mButton = (Button) findViewById(R.id.button);
        mButton.setOnClickListener(this);
//        URL url = new URL();
    }

    public class DownloadUpgradeFile extends AsyncTask<String, Integer, Integer> {
        HttpURLConnection httpURLConnection = null;
        public boolean isBpDownload = false;
        private ProgressDialog mDownloadProgressDialog;
        @Override
        protected void onPreExecute() {
            initDownloadProgressDialog();
            mDownloadProgressDialog.show();
        }

        @Override
        protected Integer doInBackground(String... params) {
            if (params[1].equals(BP_NAME)){
                isBpDownload = true;
            }
            URL url = null;
            int responseCode = 0;
            try {
                url = new URL(params[0]);
                httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setReadTimeout(10000);
                httpURLConnection.setConnectTimeout(15000);
                httpURLConnection.setDoInput(true);
                httpURLConnection.connect();
                responseCode = httpURLConnection.getResponseCode();
            } catch (Exception e) {
                e.printStackTrace();
                downloadFailReason = e.toString();
            }

            switch (responseCode) {
                case HttpURLConnection.HTTP_OK:
                    BufferedInputStream bufferedInputStream = null;
                    BufferedOutputStream bufferedOutputStream = null;
                    try {
                        InputStream inputStream = httpURLConnection.getInputStream();
                        int count = httpURLConnection.getContentLength();
                        int progress = 0;
                        File destFile = new File(DOWNLOAD_UPGRADE_FILE_PATH);
                        if (!destFile.exists()) {
                            destFile.mkdirs();
                        }
                        if (params[1] == BP_NAME) {
                            bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(new File(destFile, "bp_upgrade.hex")));
                        } else {
                            bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(new File(destFile, "vocoder_upgrade.bin")));
                        }
                        bufferedInputStream = new BufferedInputStream(inputStream);
                        byte[] buffer = new byte[2048];
                        int len = 0;
                        while ((len = bufferedInputStream.read(buffer)) != -1) {
                            bufferedOutputStream.write(buffer, 0, len);
                            bufferedOutputStream.flush();
                            progress += len;
                            if (count == 0) {
                                publishProgress(-1);
                            } else {
                                int curProgress = (int) (((float) progress / count) * 100);
                                publishProgress(curProgress);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (bufferedOutputStream != null) {
                            try {
                                bufferedOutputStream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        if (bufferedInputStream != null) {
                            try {
                                bufferedInputStream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    break;

                default:
                    return responseCode;
            }
            return SUCCESS;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
                Log.d("", "====values[0]: " + values[0]);
                mDownloadProgressDialog.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (result == SUCCESS){
                    mDownloadProgressDialog.dismiss();
            } else {
                if (result == 0){
                    mDownloadProgressDialog.setMessage("下载失败 reason is: " + downloadFailReason);
                } else {
                    mDownloadProgressDialog.setMessage("下载失败 reason is: " + result);
                }
            }
        }

        private void initDownloadProgressDialog() {
            if (mDownloadProgressDialog != null) {
                mDownloadProgressDialog = null;
            }
            mDownloadProgressDialog = new ProgressDialog(MainActivity.this);
            mDownloadProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            if (isBpDownload){
                mDownloadProgressDialog.setTitle("下载BP模块升级文件");
                mDownloadProgressDialog.setMessage("正在下载BP模块升级文件,请稍等.....");
            } else {
                mDownloadProgressDialog.setTitle("下载声码器升级文件");
                mDownloadProgressDialog.setMessage("正在下载声码器升级文件,请稍等.....");
            }
            mDownloadProgressDialog.setCancelable(true);
            mDownloadProgressDialog.setIndeterminate(false);
            mDownloadProgressDialog.setProgress(-1);
        }
    }
}
