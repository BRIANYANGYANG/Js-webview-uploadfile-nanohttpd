package com.example.a0.alarmdisplayv1.creatfile;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.socks.library.KLog;
import com.squareup.okhttp.Request;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.Event;
import org.xutils.view.annotation.ViewInject;
import org.xutils.x;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import me.weyye.hipermission.HiPermission;
import me.weyye.hipermission.PermissionCallback;
import me.weyye.hipermission.PermissionItem;

import static com.example.a0.alarmdisplayv1.creatfile.R.id.uploadfile;

@ContentView(R.layout.activity_main)
public class MainActivity extends AppCompatActivity {
    final static String TAG = "MainActivity";
    private ExecutorService mExecutorService;
    //录音API
    private MediaRecorder mMediaRecorder;
    //录音开始时间与结束时间
    private long startTime, endTime;
    //录音所保存的文件
    private File mAudioFile;
    //录音文件保存位置
    private String mFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/YPFaudio/";
    //当前是否正在播放
    private volatile boolean isPlaying;
    //播放音频文件API
    private MediaPlayer mediaPlayer;

    private String deviceIP;//本机ip
    private int webServerPort = 8080;
    private String upLoadFileName;

    private String uploadFileServerIp;//目标上传服务器ip


    private Webserver webserver;

    @ViewInject(R.id.textview)
    TextView mTextView;
    @ViewInject(R.id.image1)
    ImageView QCodeImageView;

    @ViewInject(R.id.webview)
    WebView webView;

    @Event(value = R.id.btn_to_scan1)
    private void doScan(View view) {
        customScan();
        KLog.i("customScan");
    }

    @Event(value = R.id.btn_to_http)
    private void doHttp(View view) {
        httptest();
        KLog.i("doHttp");
    }

    @ViewInject(R.id.recordbtn)
    Button recordbtn;

    @Event(value = R.id.recordbtn_clickstart)
    private void recordClickStart(View view) {
        startRecord();
        Toast.makeText(getApplicationContext(), "开始录音", Toast.LENGTH_LONG);
        KLog.i("recordClickStart");
    }

    @Event(value = R.id.recordbtn_clickstop)
    private void recordClickStop(View view) {
        stopRecord();
        Toast.makeText(getApplicationContext(), "录音结束", Toast.LENGTH_LONG);

        KLog.i("recordClickStop");
    }

    @Event(value = R.id.androidtojs)
    private void androidToJs(View view) {
        KLog.i("androidToJs");
        KLog.i(Thread.currentThread());
        webView.loadUrl("javascript:androidToJs()");

    }

    @Event(value = uploadfile)
    private void uploadFile(View view) {
        KLog.i("uploadFile");
        KLog.i(Thread.currentThread());
        KLog.i("uploadFile", mAudioFile.getAbsolutePath());

        File file = new File(mAudioFile.getAbsolutePath());
        if (!file.exists()) {
            KLog.e("file not exists");
        }

        String uploadUrl = "http://" + uploadFileServerIp + "/uploadfile";
        KLog.i(uploadUrl);
        OkHttpUtils.post()
                .addFile("recfile" + mAudioFile.getName(), "fliename=" + mAudioFile.getName(), file)
                .url(uploadUrl)
                .build()
                .execute(new StringCallback() {
                    @Override
                    public void onError(Request request, Exception e) {
                        KLog.e(e.toString());
                    }

                    @Override
                    public void onResponse(String response) {
                        KLog.i(response);
                        KLog.i(Thread.currentThread());

                        Toast.makeText(getApplicationContext(), "上传完成OK " + mAudioFile.getName(), Toast.LENGTH_LONG);

                        AlertDialog.Builder build = new AlertDialog.Builder(MainActivity.this);
                        build.setTitle("上传完成" + mAudioFile.getName())
                                .setPositiveButton("ok", null)
                                .show();

                    }
                });



    }


//    @Event(value = R.id.recordbtn, type = View.OnTouchListener.class)
//    private boolean doRecord(View view, MotionEvent motionEvent) {
//        KLog.i("doRecord");
//        switch (motionEvent.getAction()) {
//            case MotionEvent.ACTION_DOWN:
//                KLog.i("action_down");
//                startRecord();
//                break;
//
//            //松开操作
//            case MotionEvent.ACTION_CANCEL:
//            case MotionEvent.ACTION_UP:
//                stopRecord();
//                break;
//
//            default:
//                break;
//        }
//
//        return true;
//
//
//    }


    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1){
                String data = (String) msg.obj;
                KLog.i("**********************************************" + "/n" + data + "/n" + "********************************");
                mTextView.setText(data);
                Toast.makeText(getApplicationContext(), "收到信息" + data, Toast.LENGTH_LONG).show();

            } else if (msg.what == 2) {
                KLog.i("start  REC by js");
                Toast.makeText(getApplicationContext(), "开始录音 by js", Toast.LENGTH_LONG).show();
            } else if (msg.what == 3) {
                KLog.i("stop REC by js");
                Toast.makeText(getApplicationContext(), "结束录音 by js", Toast.LENGTH_LONG).show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        x.view().inject(this);
        //录音及播放要使用单线程操作
        mExecutorService = Executors.newSingleThreadExecutor();

        recordbtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                KLog.i("doRecord");
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        KLog.i("action_down");
                        startRecord();
                        break;

                    //松开操作
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_UP:
                        stopRecord();
                        break;

                    default:
                        break;
                }
                return true;
            }
        });

        InitWebview();

        //android 6.0权限申请
        List<PermissionItem> permissionItems = new ArrayList<PermissionItem>();
        permissionItems.add(new PermissionItem(Manifest.permission.CAMERA));
        permissionItems.add(new PermissionItem(Manifest.permission.WRITE_EXTERNAL_STORAGE));
        permissionItems.add(new PermissionItem(Manifest.permission.READ_EXTERNAL_STORAGE));
        permissionItems.add(new PermissionItem(Manifest.permission.INTERNET));
        permissionItems.add(new PermissionItem(Manifest.permission.RECORD_AUDIO));

        HiPermission.create(this)
                .permissions(permissionItems)
                .checkMutiPermission(new PermissionCallback() {
                    @Override
                    public void onClose() {
                        KLog.d(TAG, "onClose");
                    }

                    @Override
                    public void onFinish() {
                        KLog.d(TAG, "onFinish");
                    }

                    @Override
                    public void onDeny(String permission, int position) {
                        KLog.d(TAG, "onDeny");
                    }

                    @Override
                    public void onGuarantee(String permission, int position) {
                        KLog.d(TAG, "onGuarantee");
                    }
                });

        webserver = new Webserver(getApplicationContext(), webServerPort);
        if (webserver.isAlive()) {
            webserver.stop();
        }

        try {
            Thread.sleep(1*1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            webserver.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        webserver.setUpdataListener(new Webserver.UpdataListener() {
            @Override
            public void updata(String data) {
                if (handler != null) {
                    Message msg = new Message();
                    msg.what = 1;
                    msg.obj = data;
                    handler.sendMessage(msg);
                }
            }
        });


//        生成二维码
        deviceIP = getIP(getApplicationContext());
        String qCodeValue = deviceIP + ":" + webServerPort;
        KLog.i(TAG, "qCodeValue=" + qCodeValue);
        Bitmap a = encodeAsBitmap(qCodeValue);
        QCodeImageView.setImageBitmap(a);


    }

    private void InitWebview() {
        webView.loadUrl("file:///android_asset/test.html");
//        webView.loadUrl("file:///android_asset/h5.html");
        WebSettings webSettings  = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new JavaScriptinterface(), "android");





    }

    class JavaScriptinterface {
        @JavascriptInterface
        public void getResult(String str) {
            KLog.i("js",  str);
            KLog.i(Thread.currentThread());
            if (str.equals("jsToAndriodStartREC")) {
                KLog.i("startRecord by js");
                startRecord();
                handler.sendEmptyMessage(2);
            } else if (str.equals("jsToAndriodStopREC")) {
                KLog.i("stopRecord by js");
                stopRecord();
                handler.sendEmptyMessage(3);

            }

        }
    }

    public Bitmap encodeAsBitmap(String str) {
        Bitmap bitmap = null;
        BitMatrix result = null;
        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();

        try {
            result = multiFormatWriter.encode(str, BarcodeFormat.QR_CODE, 200, 200);
            // 使用 ZXing Android Embedded 要写的代码
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            bitmap = barcodeEncoder.createBitmap(result);
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }

        return bitmap;
    }

    // 你也可以使用简单的扫描功能，但是一般扫描的样式和行为都是可以自定义的，这里就写关于自定义的代码了
// 你可以把这个方法作为一个点击事件
    public void customScan() {
        new IntentIntegrator(this)
                .setOrientationLocked(false)
                .setCaptureActivity(CustomScanActivity.class) // 设置自定义的activity是CustomActivity
                .initiateScan(); // 初始化扫描
    }

    @Override
// 通过 onActivityResult的方法获取 扫描回来的 值
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult intentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (intentResult != null) {
            if (intentResult.getContents() == null) {
                Toast.makeText(this, "内容为空", Toast.LENGTH_LONG).show();
            } else {
                // ScanResult 为 获取到的字符串
                String ScanResult = intentResult.getContents();
                uploadFileServerIp = ScanResult;
                KLog.i(TAG, "uploadFileServerIp=" + uploadFileServerIp);
                Toast.makeText(this, "扫描成功" + ScanResult, Toast.LENGTH_LONG).show();


            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void httptest(){
        new Thread(new Runnable() {
            @Override
            public void run() {

                KLog.i(TAG, "http get start test ");
                try {
                    OkHttpUtils
                            .get()
                            .url("http://10.12.113.36:8080" + "/updatedata")
                            .addParams("data1", "data111")
                            .addParams("data2", "data2")
                            .build()
                            .execute();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

//                OkHttpClient okHttpClient = new OkHttpClient();
//                Request request = new Request.Builder()
//                        .url("http://10.12.114.16:8080/updatedata")
//                        .build();
//                Call call  = okHttpClient.newCall(request);
//                Response response = null;
//                try {
//                    response = okHttpClient.newCall(request).execute();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//
//                if (!response.isSuccessful())
//                    KLog.e("Unexpected code " + response);
//                Headers responseHeaders = response.headers();
//                for (int i = 0; i < responseHeaders.size(); i++) {
//                    System.out.println(responseHeaders.name(i) + ": " + responseHeaders.value(i));
//                }
//                try {
//                    KLog.i(response.body().string());
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
        }).start();



    }

    /**
     * @description 开始进行录音
     * @author ldm
     * @time 2017/2/9 9:18
     */
    private void startRecord() {
//        start_tv.setText(R.string.stop_by_up);
//        start_tv.setBackgroundResource(R.drawable.bg_gray_round);
        KLog.i("startRecord");

        //异步任务执行录音操作
        mExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                //播放前释放资源
                releaseRecorder();
                //执行录音操作
                recordOperation();
            }
        });
    }

    /**
     * @description 录音失败处理
     * @author ldm
     * @time 2017/2/9 9:35
     */
    private void recordFail() {
        mAudioFile = null;
        KLog.d("recordFail");
    }

    /**
     * @description 录音操作
     * @author ldm
     * @time 2017/2/9 9:34
     */
    private void recordOperation() {
        KLog.i("recordOperation");
        //创建MediaRecorder对象
        mMediaRecorder = new MediaRecorder();
        //创建录音文件,.m4a为MPEG-4音频标准的文件的扩展名
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
        String dateString = sdf.format(new Date());
        mAudioFile = new File(mFilePath + dateString + ".m4a");
        KLog.i(TAG, mAudioFile.getAbsolutePath());
        //创建父文件夹
        mAudioFile.getParentFile().mkdirs();
        try {
            //创建文件
            mAudioFile.createNewFile();
            //配置mMediaRecorder相应参数
            //从麦克风采集声音数据
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            //设置保存文件格式为MP4
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            //设置采样频率,44100是所有安卓设备都支持的频率,频率越高，音质越好，当然文件越大
            mMediaRecorder.setAudioSamplingRate(44100);
            //设置声音数据编码格式,音频通用格式是AAC
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            //设置编码频率
            mMediaRecorder.setAudioEncodingBitRate(192000);
            //设置录音保存的文件
            mMediaRecorder.setOutputFile(mAudioFile.getAbsolutePath());
            //开始录音
            mMediaRecorder.prepare();
            mMediaRecorder.start();
            //记录开始录音时间
            startTime = System.currentTimeMillis();
        } catch (Exception e) {
            e.printStackTrace();
            recordFail();
        }
    }

    /**
     * @description 结束录音操作
     * @author ldm
     * @time 2017/2/9 9:18
     */
    private void stopRecord() {
//        start_tv.setText(R.string.speak_by_press);
//        start_tv.setBackgroundResource(R.drawable.bg_white_round);

        KLog.i("stopRecord");

        //停止录音
        mMediaRecorder.stop();
        //记录停止时间
        endTime = System.currentTimeMillis();
        //录音时间处理，比如只有大于2秒的录音才算成功
        int time = (int) ((endTime - startTime) / 1000);
        if (time >= 3) {
            //录音成功,添加数据
//            FileBean bean = new FileBean();
//            bean.setFile(mAudioFile);
//            bean.setFileLength(time);
//            dataList.add(bean);
//            //录音成功,发Message
//            mHandler.sendEmptyMessage(Constant.RECORD_SUCCESS);
            KLog.i("RECORD_SUCCESS");

        } else {
            mAudioFile = null;
//            mHandler.sendEmptyMessage(Constant.RECORD_TOO_SHORT);
            KLog.e("RECORD_fail");
        }
        //录音完成释放资源
        releaseRecorder();
    }

    /**
     * @description 翻放录音相关资源
     * @author ldm
     * @time 2017/2/9 9:33
     */
    private void releaseRecorder() {
        if (null != mMediaRecorder) {
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //页面销毁，线程要关闭
        mExecutorService.shutdownNow();
    }

    public static String getIP(Context context){

        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();)
                {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && (inetAddress instanceof Inet4Address))
                    {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        }
        catch (SocketException ex){
            ex.printStackTrace();
        }
        return null;
    }

}
