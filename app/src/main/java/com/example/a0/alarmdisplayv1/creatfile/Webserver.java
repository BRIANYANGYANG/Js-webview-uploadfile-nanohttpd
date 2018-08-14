package com.example.a0.alarmdisplayv1.creatfile;

import android.content.Context;
import android.os.Environment;

import com.socks.library.KLog;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by yangpengfei10 on 2018/5/19.
 */

public class Webserver extends NanoHTTPD {
    private Context context;
    final static String TAG = "Webserver";

    /**
     * Constructs an HTTP server on given port.
     *
     * @param port
     */
    public Webserver(Context context, int port) {
        super(port);
        this.context = context;

    }

    /**
     * Constructs an HTTP server on given hostname and port.
     *
     * @param hostname
     * @param port
     */
    public Webserver(String hostname, int port) {
        super(hostname, port);
    }

    @Override
    public Response serve(IHTTPSession session) {
        KLog.d(Thread.currentThread());
        KLog.d(session.getHeaders());
        KLog.d(session.getUri());
        KLog.d(session.getInputStream());
        KLog.d(session.getMethod());
        KLog.d(session.getRemoteIpAddress());
        KLog.d(session.getParameters());
        KLog.d(session.toString());

        Map<String, String> files = new HashMap<>();
        try {
            session.parseBody(files);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ResponseException e) {
            e.printStackTrace();
        }

        if (session.getUri().equals("/updatedata")) {
            KLog.i("updatedata");
            return responseUpdataDisP(session);
        } else if (session.getUri().equals("/uploadfile")) {
            KLog.i("uploadfile");

            return responseUploadFile(session, files);
        }

        return newFixedLengthResponse(getHTMLStringFromAssets(context, "settings.html"));

    }

    public static String getHTMLStringFromAssets(Context mContext, String fileName) {
        String string = "";
        StringBuilder stringBuilder = new StringBuilder();
        InputStream inputStream = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedInputStream = null;
//        File file = new File("file:///android_asset/settings.html");
        try {
            inputStream = mContext.getAssets().open(fileName);
            inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
            bufferedInputStream = new BufferedReader(inputStreamReader);
            String line = null;
            while ((line = bufferedInputStream.readLine()) != null) {
                stringBuilder.append(line + "\r\n");
            }

            inputStreamReader.close();
            bufferedInputStream.close();

        } catch (Exception e) {
            e.printStackTrace();
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (inputStreamReader != null) {
                    inputStreamReader.close();
                }
                if (bufferedInputStream != null) {
                    bufferedInputStream.close();
                }
            } catch (Exception e1) {
                e1.printStackTrace();
            }

        }

        return stringBuilder.toString();
    }

    /**
     * 服务端接收代码
     * 接收来自okhttp上传的文件
     *
     * @param session
     * @param files
     * @return
     */
    private Response responseUploadFile(IHTTPSession session, Map<String, String> files) {
        KLog.i("****************response UploadFile****************");
        Map<String, String> params = session.getParms();

        for (Map.Entry<String, String> entry : params.entrySet()) {
            String paramsKey = entry.getKey();
            KLog.i(TAG, paramsKey);
            if (paramsKey.contains("recfile")) {
                String tmpFilePath = files.get(paramsKey);
                String fileName = paramsKey;
                KLog.i(TAG, tmpFilePath);
                KLog.i(TAG, fileName);

                File tmpFile = new File(tmpFilePath);
                File  targetFile = new File(Environment.getExternalStorageDirectory() + "/" + fileName);
                KLog.i(TAG, tmpFile.getAbsolutePath());
                KLog.i(TAG, targetFile.getAbsolutePath());

                customBufferStreamCopy(tmpFile, targetFile);

            }


        }


        return newFixedLengthResponse(getHTMLStringFromAssets(context, "ok.html"));
    }

    private String savaFile(DataInputStream dis) {
        String fileurl = "/sdcard/接收到的文件/";


        return "1";

    }

    private Response responseUpdataDisP(IHTTPSession session) {
        KLog.i("responseUpdataDisP");

        String data1 = getParams(session, "data1");
        String data2 = getParams(session, "data2");
        KLog.d("data1=" + data1);
        KLog.d("data2=" + data2);

        try {
            data1 = new String(data1.getBytes(), 0, data1.length(), "utf-8");
            data2 = new String(data2.getBytes(), 0, data2.length(), "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        KLog.i(data1);
        KLog.i(data2);

        updataListener.updata(data1 + data2);


        return newFixedLengthResponse(getHTMLStringFromAssets(context, "ok.html"));


    }

    public static String getParams(IHTTPSession session, String key) {
        if (session.getParameters().get(key) != null) {
            return decodePercent(session.getParameters().get(key).get(0));
        }
        return null;
    }

    private UpdataListener updataListener;

    public void setUpdataListener (UpdataListener udl) {
        this.updataListener = udl;
    }

    public interface UpdataListener {
        void updata(String data);
    }

    private static void customBufferStreamCopy(File source, File target) {
        InputStream fis = null;
        OutputStream fos = null;
        try {
            fis = new FileInputStream(source);
            fos = new FileOutputStream(target);
            byte[] buf = new byte[4096];
            int i;
            while ((i = fis.read(buf)) != -1) {
                fos.write(buf, 0, i);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fis.close();
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}


