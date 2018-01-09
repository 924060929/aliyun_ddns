package lanhuajian.tech.aliyun_ddns;

import net.sf.json.JSONObject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Callable;

public class Utils {
    private static final String AGLORITHM_NAME = "HmacSHA1";
    private static final String SEPARATOR = "&";
    private static final List<Callable<String>> IP_SUPPLIERS = new ArrayList();

    static {

        IP_SUPPLIERS.add(new Callable<String>() {
            public String call() throws Exception {
                return requestHttp("http://www.3322.org/dyndns/getip", null);
            }
        });

        IP_SUPPLIERS.add(new Callable<String>() {
            public String call() throws Exception {
                String resp = requestHttp("http://httpbin.org/ip", null);
                return JSONObject.fromString(resp).getString("origin");
            }
        });

        IP_SUPPLIERS.add(new Callable<String>() {
            public String call() throws Exception {
                return requestHttp("http://ip.42.pl/raw", null);
            }
        });


        IP_SUPPLIERS.add(new Callable<String>() {
            public String call() throws Exception {
                String resp = requestHttp("http://api.ipify.org/?format=json", null);
                return JSONObject.fromString(resp).getString("ip");
            }
        });
    }

    public static String getInternetIp() {
        Iterator<Callable<String>> it = IP_SUPPLIERS.iterator();
        Throwable lastException = null;
        while (it.hasNext()) {
            try {
                String ip = it.next().call();
                if (!isBlank(ip)) {
                    return ip.trim();
                }
            } catch (Throwable t) {
                lastException = t;
            }
        }
        throw new IllegalStateException("Get internet ip fail", lastException);
    }

    public static String requestHttp(String url, Map<String, String> param) throws IOException {
        String realUrl = buildUrl(url, param);
        HttpURLConnection urlConnection = (HttpURLConnection) new URL(realUrl).openConnection();

        InputStream responseInputStream = null;
        try {
            urlConnection.setRequestMethod("GET");
            urlConnection.setConnectTimeout(5000);

            responseInputStream = urlConnection.getInputStream();
            return stream2Str(responseInputStream);
        } catch (Throwable t) {
            InputStream errorStream = urlConnection.getErrorStream();
            String errorBody = stream2Str(errorStream);
            int code = urlConnection.getResponseCode();
            String msg = urlConnection.getResponseMessage();
            throw new IllegalStateException("HTTP code " + code + "(" + msg + "): " + errorBody);
        } finally {
            urlConnection.disconnect();
        }
    }

    public static String stream2Str(InputStream inputStream) throws IOException {
        return reader2Str(new InputStreamReader(inputStream, "UTF-8"));
    }

    public static String reader2Str(Reader reader) throws IOException {
        StringBuilder response = new StringBuilder();
        char[] buffer = new char[1024];
        int readSize = 0;
        while ((readSize = reader.read(buffer)) != -1) {
            response.append(buffer, 0, readSize);
        }
        return response.toString();
    }

    public static String getISO8601Time(Date date) {
        Date nowDate = date;
        if (null == date) {
            nowDate = new Date();
        }

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        df.setTimeZone(new SimpleTimeZone(0, "GMT"));
        return df.format(nowDate);
    }

    public static String signString(String source, String accessSecret)
            throws InvalidKeyException, IllegalStateException {
        try {
            Mac mac = Mac.getInstance(AGLORITHM_NAME);
            mac.init(new SecretKeySpec(
                    accessSecret.getBytes(AcsURLEncoder.URL_ENCODING),AGLORITHM_NAME));
            byte[] signData = mac.doFinal(source.getBytes(AcsURLEncoder.URL_ENCODING));
            return Base64Helper.encode(signData);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("HMAC-SHA1 not supported.");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 not supported.");
        }

    }

    public static String composeStringToSign(String method, Map<String, String> queries) {
        String[] sortedKeys = queries.keySet().toArray(new String[]{});
        Arrays.sort(sortedKeys);
        StringBuilder canonicalizedQueryString = new StringBuilder();
        try {
            for(String key : sortedKeys) {
                canonicalizedQueryString.append("&")
                        .append(AcsURLEncoder.percentEncode(key)).append("=")
                        .append(AcsURLEncoder.percentEncode(queries.get(key)));
            }

            StringBuilder stringToSign = new StringBuilder();
            stringToSign.append(method.toString());
            stringToSign.append(SEPARATOR);
            stringToSign.append(AcsURLEncoder.percentEncode("/"));
            stringToSign.append(SEPARATOR);
            stringToSign.append(AcsURLEncoder.percentEncode(
                    canonicalizedQueryString.toString().substring(1)));

            return stringToSign.toString();
        } catch (UnsupportedEncodingException exp) {
            throw new RuntimeException("UTF-8 encoding is not supported.");
        }

    }

    public static String buildUrl(String url, Map<String, String> params) throws UnsupportedEncodingException {
        if (params == null) {
            return url;
        }
        StringBuilder newUrl = new StringBuilder(url);
        newUrl.append("?");
        boolean isFirst = true;
        for (Map.Entry<String, String> kv : params.entrySet()) {
            if (!isFirst) {
                newUrl.append("&");
            }
            newUrl.append(URLEncoder.encode(kv.getKey(), "UTF-8")).append("=").append(URLEncoder.encode(kv.getValue(), "UTF-8"));
            isFirst = false;
        }

        return newUrl.toString();
    }

    public static boolean isBlank(String str) {
        return str == null || str.length() == 0 || str.trim().length() == 0;
    }
}
