package lanhuajian.tech.aliyun_ddns;

import net.sf.json.JSONObject;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DnsClient {
    private String regionId;
    private String accessKeyId;
    private String secret;

    private static final String ALIDNS_URL = "http://alidns.aliyuncs.com";

    public DnsClient(String regionId, String accessKeyId, String secret) {
        this.regionId = regionId;
        this.accessKeyId = accessKeyId;
        this.secret = secret;
    }

    public JSONObject getDomainNameInfo(String domainName) throws IOException, InvalidKeyException {
        try {
            Map<String, String> params = getCommonParams("DescribeDomainRecords");
            params.put("DomainName", domainName);

            addSignature(params);

            String respStr = Utils.requestHttp(ALIDNS_URL, params);

            return JSONObject.fromString(respStr);
        } catch (Throwable t) {
            throw new IllegalStateException("fetch domain record list fail, domainName: " + domainName, t);
        }
    }

    public JSONObject updateDns(JSONObject jsonObject, String ip) throws IOException {
        try {
            Map<String, String> params = getCommonParams("UpdateDomainRecord");
            params.put("Type", jsonObject.getString("Type"));
            params.put("RR", jsonObject.getString("RR"));
            params.put("RecordId", jsonObject.getString("RecordId"));
            params.put("Value", ip);

            addSignature(params);

            String respStr = Utils.requestHttp(ALIDNS_URL, params);
            return JSONObject.fromString(respStr);
        } catch (Throwable t) {
            throw new IllegalStateException("update domain record fail, ip: " + ip + ", record: " + jsonObject, t);
        }
    }

    private void addSignature(Map<String, String> params) throws InvalidKeyException {
        String strToSign = Utils.composeStringToSign("GET", params);
        String signature = Utils.signString(strToSign, secret + "&");
        params.put("Signature", signature);
    }

    private Map<String, String> getCommonParams(String action) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("SignatureVersion", "1.0");
        params.put("Action", action);
        params.put("Format", "JSON");
        params.put("Version", "2015-01-09");
        params.put("AccessKeyId", accessKeyId);
        params.put("SignatureMethod", "HMAC-SHA1");
        params.put("RegionId", regionId);
        params.put("Timestamp", Utils.getISO8601Time(null));
        params.put("SignatureNonce", UUID.randomUUID().toString());

        return params;
    }
}
