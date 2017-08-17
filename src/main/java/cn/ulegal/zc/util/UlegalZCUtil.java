package cn.ulegal.zc.util;

import cn.ulegal.zc.model.CopyRightsModel;
import cn.ulegal.zc.vo.MessageVo;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Created by Joe on 2017/7/20.
 * 参数转换....
 */
public class UlegalZCUtil {

    /**
     * 项目文件根路径
     * @return
     */
    public static String rootPath() {
        return System.getProperty("user.dir");
    }

    /**
     * 业务文件路径
     * 格式: ../module/2017/7/....
     * @param module
     * @return
     */
    public static String modulePath(String module) {
        LocalDate localDate = LocalDate.now();
        String path = module + File.separator + localDate.getYear() + File.separator + localDate.getMonthValue();
        File dateDir = new File(path);
        if (!dateDir.exists()) {
            dateDir.mkdirs();
        }
        return path;
    }

    /**
     * 完整路径
     * @param module
     * @return
     */
    public static String completePath(String module) {
        return rootPath() + File.separator + modulePath(module);
    }


    public static final String TIME_YYYY_MM_DD_HH_MM_SS  = "yyyy-MM-dd HH:mm:ss";
    public static final String TIME_YYYY_MM_DD  = "yyyy-MM-dd";

    /**
     * 时间戳转日期时间
     * @param currentTime 时间戳
     * @return
     */
    public static String conTimeTime(Long currentTime) {
        SimpleDateFormat format =  new SimpleDateFormat(TIME_YYYY_MM_DD_HH_MM_SS);
        return format.format(currentTime);
    }


    public static String getSHA256Checksum(String filename) throws Exception {
        byte[] b = createChecksum(filename);
        String result = "";
        for (int i=0; i < b.length; i++) {
            result += Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring(1);//加0x100是因为有的b[i]的十六进制只有1位
        }
        return result;
    }

    public static byte[] createChecksum(String filename) throws Exception {
        InputStream fis =  new FileInputStream(filename);          //将流类型字符串转换为String类型字符串
        byte[] buffer = new byte[1024];
        MessageDigest complete = MessageDigest.getInstance("SHA-256"); //如果想使用SHA-1或SHA-256，则传入SHA-1,SHA-256
        int numRead;
        do {
            numRead = fis.read(buffer);    //从文件读到buffer，最多装满buffer
            if (numRead > 0) {
                complete.update(buffer, 0, numRead);  //用读到的字节进行SHA256的计算，第二个参数是偏移量
            }
        } while (numRead != -1);
        fis.close();
        return complete.digest();
    }

    public static RestTemplate getRestTemplateFactory() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setReadTimeout(5000);
        requestFactory.setConnectTimeout(5000);

        // 添加 转换器
        List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
        messageConverters.add(new StringHttpMessageConverter(Charset.forName("UTF-8")));
        messageConverters.add(new FormHttpMessageConverter());
        messageConverters.add(new MappingJackson2HttpMessageConverter());

        RestTemplate restTemplate = new RestTemplate(messageConverters);
        restTemplate.setRequestFactory(requestFactory);
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler());

        return restTemplate;
    }

    public static MessageVo getMessageVo() {
        MessageVo messageVo = new MessageVo();
        messageVo.setCode(400);
        messageVo.setTimeStamp(System.currentTimeMillis());
        return messageVo;
    }

    /**
     *  通过阿里云发送邮件
     * @param copyRightsModel
     * @return
     */
    public static Boolean sendEmail(CopyRightsModel copyRightsModel) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.host", "smtpdm.aliyun.com");
        props.put("mail.smtp.port", 25);
        // 发件人的账号
        props.put("mail.user", "no-reply@dm.smartlion.cn");
        // 访问SMTP服务时需要提供的密码
        props.put("mail.password", "ULegal0121");
        // 构建授权信息，用于进行SMTP进行身份验证
        Authenticator authenticator = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                // 用户名、密码
                String userName = props.getProperty("mail.user");
                String password = props.getProperty("mail.password");
                return new PasswordAuthentication(userName, password);
            }
        };
        // 使用环境属性和授权信息，创建邮件会话
        Session mailSession = Session.getInstance(props, authenticator);
//        mailSession.setDebug(true);
        // 创建邮件消息
        MimeMessage message = new MimeMessage(mailSession);
        try {
            String nick = "";
            nick = javax.mail.internet.MimeUtility.encodeText("慧狮科技");
            // 设置发件人
            InternetAddress from = new InternetAddress(nick + " <" + props.getProperty("mail.user") + ">");
            message.setFrom(from);
            Address[] a = new Address[1];
            a[0] = new InternetAddress("Ulegal@dm.smartlion.cn");
            message.setReplyTo(a);
            // 设置收件人
            InternetAddress to = new InternetAddress(copyRightsModel.getEmail());
            message.setRecipient(MimeMessage.RecipientType.TO, to);
            // 设置邮件标题
            message.setSubject("作品版权固化证明");

            //添加附件部分
            //邮件内容部分1---文本内容
            MimeBodyPart body0 = new MimeBodyPart(); //邮件中的文字部分
            body0.setContent("<p>您的作品（电子数据）已申请由中国仲裁云固化存证，予以登记，证书已生成，请从附件下载证书。</p>","text/html;charset=utf-8");

            //邮件内容部分2---附件1
            MimeBodyPart body1 = new MimeBodyPart(); //附件1
            body1.setDataHandler( new DataHandler( new FileDataSource(UlegalZCUtil.rootPath() + File.separator + "pdf" + File.separator + copyRightsModel.getOrderId() + ".pdf")) );//./代表项目根目录下

            body1.setFileName( MimeUtility.encodeText("作品版权固化证明.pdf") );//中文附件名，解决乱码

            //把上面的3部分组装在一起，设置到msg中
            MimeMultipart mm = new MimeMultipart();
            mm.addBodyPart(body0);
            mm.addBodyPart(body1);
            message.setContent(mm);

            // 设置邮件的内容体
//            message.setContent("题在我使用postman来上传图片时候，死活都没过。。显示这个，问题在哪呢？", "text/html;charset=UTF-8");
            // 发送邮件
            Transport.send(message);
        }
        catch (Exception e) {
            String err = e.getMessage();
            // 在这里处理message内容， 格式是固定的
            System.out.println(err);
            return false;
        }
        return true;
    }
}
