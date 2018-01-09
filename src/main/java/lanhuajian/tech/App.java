package lanhuajian.tech;

import lanhuajian.tech.aliyun_ddns.DnsClient;
import lanhuajian.tech.aliyun_ddns.Utils;
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
        System.out.println("begin fetch domain(" + domainName + ") record list...");
        JSONObject domainNameInfo = dnsClient.getDomainNameInfo(domainName);

        JSONObject firstRecord = domainNameInfo.getJSONObject("DomainRecords").getJSONArray("Record").getJSONObject(0);
        System.out.println("first domain record: " + firstRecord);

        String internetIp = Utils.getInternetIp();
        System.out.println("fetch internet ip: " + internetIp);

        String recordValue = firstRecord.getString("Value");

        if (!internetIp.equals(recordValue)) {
            System.out.println("begin update domain record, old value: " + recordValue + ", new value: " + internetIp);
            JSONObject updateDnsResult = dnsClient.updateDns(firstRecord, internetIp);
            System.out.println("update domain record finished");
            System.exit(666);
        } else {
            System.out.println("domain record value is same as internet ip, will not modify record");
            System.exit(0);
        }
    }

    private static Properties getConfig() throws IOException {
        FileInputStream fileInputStream = null;
        try {
            Properties properties = new Properties();
            fileInputStream = new FileInputStream("config.properties");
            properties.load(fileInputStream);
            return properties;
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("lost config.properties", e);
        } catch (Throwable t) {
            throw new IllegalStateException("loading config.properties fail", t);
        } finally {
            if (fileInputStream != null) {
                fileInputStream.close();
            }
        }
    }
}
