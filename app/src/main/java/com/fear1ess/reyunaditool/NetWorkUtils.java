package com.fear1ess.reyunaditool;

import android.content.res.Resources;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class NetWorkUtils {

    public interface NetWorkCallback {
        void onDownloadPkgNameGetSuccess(String name);
        void onDownloadPkgNameGetfailed();
        void onDownloadBytesUpdate (String name, int bytesDownloaded);
    }

    private static NetWorkCallback callback;

    public static void setNetWorkCallback(NetWorkCallback cb) {
        callback = cb;
    }

    private static Response get(String urlStr,HashMap<String,String> headers,OutputStream os){
        if(os == null){
            os = new ByteArrayOutputStream();
        }
        Response res = null;
        if(urlStr.startsWith("https://")) res = httpsReq(urlStr,headers,null,os);
        else if(urlStr.startsWith("http://")) res = httpReq(urlStr,headers,null,os);
        try {
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }

    private static Response post(String urlStr,HashMap<String,String> headers,byte[] data,OutputStream os){
        if(os == null){
            os = new ByteArrayOutputStream();
        }
        Response res = null;
        if(urlStr.startsWith("https://")) res = httpsReq(urlStr,headers,data,os);
        else if(urlStr.startsWith("http://")) res = httpReq(urlStr,headers,data,os);
        try {
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }

    public static Response get(String urlStr,HashMap<String,String> headers){
        return get(urlStr,headers,null);
    }

    public static Response post(String urlStr,HashMap<String,String> headers,byte[] data){
        return post(urlStr,headers,data,null);
    }

    public static Response download(String urlStr,HashMap<String,String> headers,byte[] data,String downloadPath) {
        File fi = new File(downloadPath);
        try {
            if(!fi.exists()) fi.createNewFile();
            FileOutputStream fos = null;
            fos = new FileOutputStream(fi);
            if(data == null) return get(urlStr,headers,fos);
            else return post(urlStr,headers,data,fos);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static Response httpReq(String urlStr, HashMap<String,String> headers, byte[] postData,OutputStream os) {
        boolean isDownload = false;
        if(os instanceof FileOutputStream) {
            isDownload = true;
        }
        Response res = null;
        try {
            URL getAppInfoUrl = new URL(urlStr);
            HttpURLConnection huc = (HttpURLConnection) getAppInfoUrl.openConnection();
            if(postData == null) huc.setRequestMethod("GET");
            else huc.setRequestMethod("POST");
            if(headers != null){
                for(Map.Entry<String,String> entry:headers.entrySet()){
                    huc.setRequestProperty(entry.getKey(),entry.getValue());
                }
            }
            huc.setConnectTimeout(5000);
            huc.connect();
            if(postData != null){
                OutputStream out = huc.getOutputStream();
                out.write(postData);
                out.flush();
            }
            String fileName = huc.getHeaderField("Content-Disposition");
            if(fileName == null){
                callback.onDownloadPkgNameGetfailed();
            }
            String pkgName = fileName.replace("attachment; filename=","")
                    .replace(".apk", "");
            if(isDownload){
                callback.onDownloadPkgNameGetSuccess(fileName);
            }
            InputStream is = huc.getInputStream();
            byte[] data = new byte[4096];
            int len = 0;
            int totalBytes = 0;
            while((len = is.read(data)) != -1){
                os.write(data,0,len);
                totalBytes += len;
                if(isDownload){
                    if(fileName != null) {
                        callback.onDownloadBytesUpdate(fileName, totalBytes);
                    }
                }
            }
            os.flush();
            byte[] resData = null;
            if(os instanceof ByteArrayOutputStream) {
                resData = ((ByteArrayOutputStream) os).toByteArray();
            }
            res = new Response(huc.getResponseCode(),huc.getResponseMessage(),resData,fileName);
            os.close();
            is.close();
            huc.disconnect();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }


    private static Response httpsReq(String urlStr, HashMap<String,String> headers,byte[] postData,OutputStream os){
        URL getAppInfoUrl = null;
        Response res = null;
        try {
            getAppInfoUrl = new URL(urlStr);
            HttpsURLConnection huc = (HttpsURLConnection) getAppInfoUrl.openConnection();
            if(postData == null) huc.setRequestMethod("GET");
            else huc.setRequestMethod("POST");
            if(headers != null){
                for(Map.Entry<String,String> entry:headers.entrySet()){
                    huc.setRequestProperty(entry.getKey(),entry.getValue());
                }
            }
            huc.setConnectTimeout(5000);
            huc.setHostnameVerifier(new TrustAllHostnamesVerifier());
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null,new TrustManager[]{new TrustAllCertificateManager()},new SecureRandom());
            huc.setSSLSocketFactory(sc.getSocketFactory());
            huc.connect();
            if(postData != null){
                OutputStream out = huc.getOutputStream();
                out.write(postData);
                out.flush();
            }
            InputStream is = huc.getInputStream();
            byte[] data = new byte[1024];
           // ByteArrayOutputStream bo = new ByteArrayOutputStream();
            int len = 0;
            while((len = is.read(data)) != -1){
                os.write(data,0,len);
            }
            os.flush();
            byte[] resData = null;
            if(os instanceof ByteArrayOutputStream) {
                resData = ((ByteArrayOutputStream)os).toByteArray();
            }
            String fileName = huc.getHeaderField("Content-Disposition");
            res = new Response(huc.getResponseCode(),huc.getResponseMessage(),resData,fileName);
            os.close();
            is.close();
            huc.disconnect();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
        return res;
    }


    public static class Response{
        public int resCode;
        public String resMessage;
        public byte[] resData;
        public String resFileName;
        public Response(int code,String msg,byte[] data,String fileName){
            resCode = code;
            resMessage = msg;
            resData = data;
            resFileName = fileName;
        }
    }

    public static class TrustAllHostnamesVerifier implements HostnameVerifier{
        @Override
        public boolean verify(String s, SSLSession sslSession) {
            return true;
        }
    }

    public static class TrustAllCertificateManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }
}
