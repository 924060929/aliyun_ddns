package lanhuajian.tech;

import lanhuajian.tech.aliyun_ddns.DnsClient;
import lanhuajian.tech.aliyun_ddns.Utils;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

/**
 * 阿里云DDNS
 */
public class App {
    public static void main(String[] args) throws Throwable {

        Properties properties = getConfig();

        DnsClient dnsClient = new DnsClient(
            properties.getProperty("regionId"),
            properties.getProperty("accessKeyId"),
            properties.getProperty("secret")
        );

        String domainName = properties.getProperty("domainName");
        System.out.println("Begin fetch domain(" + domainName + ") record list...");
        JSONObject domainNameInfo = dnsClient.getDomainNameInfo(domainName);

        String rrPatern = properties.getProperty("recordPattern");

        JSONObject aRecord = findARecord(domainNameInfo, rrPatern);
        System.out.println("Use pattern '" + rrPatern + "' found domain record: " + aRecord);

        String internetIp = Utils.getInternetIp();
        System.out.println("Fetch internet ip: " + internetIp);

        String recordValue = aRecord.getString("Value");

        if (!internetIp.equals(recordValue)) {
            System.out.println("Begin update domain record, old value: " + recordValue + ", new value: " + internetIp);
            JSONObject updateDnsResult = dnsClient.updateDns(aRecord, internetIp);
            System.out.println("Update domain record finished");
            System.exit(66);
        } else {
            System.out.println("Domain record value is same as internet ip, will not modify record");
            System.exit(0);
        }
    }

    private static JSONObject findARecord(JSONObject domainNameInfo, String pattern) {
        JSONArray records = domainNameInfo.getJSONObject("DomainRecords").getJSONArray("Record");
        for (int i = 0; i < records.length(); i++) {
            JSONObject record = records.getJSONObject(i);
            String type = record.getString("Type");
            String rr = record.getString("RR");
            if ("A".equals(type) && rr.matches(pattern)) {
                return record;
            }
        }
        throw new IllegalStateException("Can not find A record by pattern: " + pattern);
    }

    private static Properties getConfig() throws IOException {
        FileInputStream fileInputStream = null;
        try {
            Properties properties = new Properties();
            fileInputStream = new FileInputStream("config.properties");
            properties.load(fileInputStream);
            if (Utils.isBlank(properties.getProperty("recordPattern"))) {
                properties.put("recordPattern", "[\\s\\S]*");
            }
            return properties;
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("Lost config.properties", e);
        } catch (Throwable t) {
            throw new IllegalStateException("Loading config.properties fail", t);
        } finally {
            if (fileInputStream != null) {
                fileInputStream.close();
            }
        }
    }
}
