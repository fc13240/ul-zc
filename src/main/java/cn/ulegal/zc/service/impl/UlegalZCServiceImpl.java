package cn.ulegal.zc.service.impl;

import cn.ulegal.zc.dao.UlegalZCDao;
import cn.ulegal.zc.model.CopyRightsModel;
import cn.ulegal.zc.service.UlegalZCService;
import cn.ulegal.zc.util.UlegalZCUtil;
import cn.ulegal.zc.vo.CopyRightsVo;
import com.google.gson.Gson;
import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.tencent.trustsql.sdk.TrustSDK;
import com.tencent.trustsql.sdk.util.SignStrUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by Joe on 2017/8/15.
 */
@Service
public class UlegalZCServiceImpl implements UlegalZCService {
    @Autowired
    UlegalZCDao ulegalZCDao;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    Gson gson;

    @Value("${zc.public.key}")
    String publicKey;

    @Value("${zc.private.key}")
    String privateKey;

    @Override
    public Boolean getCountByOrderId(String orderId) {
        int count = ulegalZCDao.getCountByOrderId(orderId);
        if (count == 0){
            return false;
        }
        return true;
    }

    @Override
    public CopyRightsVo saveOrderInfo(CopyRightsVo copyRightsVo) {
        String hashValue = getHashValue(copyRightsVo.getFilePath());
        copyRightsVo.setHashValue(hashValue);
        CopyRightsModel copyRightsModel = gson.fromJson(gson.toJson(copyRightsVo), CopyRightsModel.class);
        Boolean tranPdfResult = tranPdf(copyRightsVo);
        if (!tranPdfResult) {
            return null;
        }
        //区块链
        Boolean trustSqlResult = insertData(copyRightsModel);
        if (!trustSqlResult) {
            return null;
        }
        //邮件发送
        Boolean sendResult = UlegalZCUtil.sendEmail(copyRightsModel);
        if (!sendResult) {
            return null;
        }
        //数据存储
        Integer id = ulegalZCDao.saveSubmitInfo(copyRightsModel);
        if (id != 1) {
            return null;
        }
        return copyRightsVo;
    }

    @Override
    public CopyRightsModel checkFile(File file) {
        CopyRightsModel model = null;
        InputStream fis = null;          //将流类型字符串转换为String类型字符串
        try {
            fis = new FileInputStream(file);

            byte[] buffer = new byte[1024];
            MessageDigest complete = MessageDigest.getInstance("SHA-256"); //如果想使用SHA-1或SHA-256，则传入SHA-1,SHA-256
            int numRead;
            do {
                numRead = fis.read(buffer);    //从文件读到buffer，最多装满buffer
                if (numRead > 0) {
                    complete.update(buffer, 0, numRead);  //用读到的字节进行SHA256的计算，第二个参数是偏移量
                }
            } while (numRead != -1);

            byte[] b = complete.digest();;
            String result = "";
            for (int i=0; i < b.length; i++) {
                result += Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring(1);//加0x100是因为有的b[i]的十六进制只有1位
            }
            model = ulegalZCDao.getCountByHashValue(result);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return model;
    }

    /** 电子证据固化
     *
     * @return
     */
    @Override
    @Async
    public void curingEvidence(CopyRightsVo copyRightsVo) {
        String url = "https://api.ulegal.cn:9091/evidenceCuring";
        Map data = new HashMap();
        data.put("owner", copyRightsVo.getOwner());
        data.put("title", copyRightsVo.getTitle());
        data.put("source", copyRightsVo.getSource());
        data.put("hashValue", copyRightsVo.getHashValue());
        Map result = restTemplate.postForObject(url, data, Map.class);
        String curingKey = result.get("curingKey") == null ? null : result.get("curingKey").toString();
        if (null != curingKey) {
            copyRightsVo.setCuringKey(curingKey);
            CopyRightsModel copyRightsModel = gson.fromJson(gson.toJson(copyRightsVo), CopyRightsModel.class);
            ulegalZCDao.updateCuringId(copyRightsModel);
        }
    }

    /**
     * 根据项目地址计算文件hash值
     * @param path
     * @return
     */
    private String getHashValue(String path) {
        String rootPath = UlegalZCUtil.rootPath();
        String filePath = rootPath + path;
        String hashValue = "";
        try {
            hashValue = UlegalZCUtil.getSHA256Checksum(filePath);
            File file = new File(filePath);
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hashValue;
    }

    /**
     * 区块链插入数据
     * @param copyRightsModel
     * @return
     * @throws Exception
     */
    private Boolean insertData(CopyRightsModel copyRightsModel) {
        String url = "https://open.trustsql.qq.com/cgi-bin/v1.0/trustsql_iss_append.cgi";
        copyRightsModel.setFilePath(null);
        Long timeStamp = copyRightsModel.getTimestamp();
        Map content = gson.fromJson(gson.toJson(copyRightsModel), Map.class);
        content.put("timestamp", timeStamp.toString());
        if (StringUtils.isEmpty(content.get("idCardNo").toString())) {
            content.remove("idCardNo");
        }
        Map data = getAppendData(content);
        String address = null;
        String resutlData = "";
        try {
            address = TrustSDK.generateAddrByPubkey(publicKey);
            data.put("address", address);
            String sign = TrustSDK.generateSign(data.get("info_key").toString(), data.get("info_version").toString(), Integer.valueOf(data.get("state").toString()), data.get("content").toString(),
                    data.get("notes").toString(), data.get("commit_time").toString(), privateKey);
            data.put("sign", sign);
            data.put("public_key", publicKey);
            String dataString = SignStrUtil.mapToKeyValueStr(data);
            System.out.println(dataString);
            String mchSign = TrustSDK.signString(privateKey, dataString.getBytes("UTF-8"));
            data.put("mch_sign", mchSign);
            data.put("content", gson.fromJson(data.get("content").toString(), Map.class));
            data.put("notes", gson.fromJson(data.get("notes").toString(), Map.class));
            System.out.println(gson.toJson(data));
            System.out.println(data.get("mch_sign"));
            resutlData = restTemplate.postForObject(url, data, String.class);
            System.out.println(resutlData);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     *  传入内容，组装发送数据
     * @param content
     * @return
     */
    private Map getAppendData(Map content) {
        Map<String, Object> data = new TreeMap<String, Object>();
        data.put("version", "1.0");
        data.put("sign_type", "ECDSA");
        data.put("mch_id", "gbec74d8aa11accfa");
        Date date = new Date();
        data.put("info_key", content.get("orderId"));
        data.put("info_version", 1);
        data.put("state", 1);
        data.put("content", gson.toJson(content));
        Map notes = new HashMap();
        notes.put("type", "copyRight");
        data.put("notes", gson.toJson(notes));
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
        String cmmitTime = simpleDateFormat.format(date);
        data.put("commit_time", cmmitTime);
        return  data;
    }

    /**
     *  读取外部pdf模板文件生成目标pdf文件(文件夹及模板需提前创建)
     * @param copyRightsVo
     * @return
     */
    private Boolean tranPdf(CopyRightsVo copyRightsVo) {
        String filePath = UlegalZCUtil.rootPath() + File.separator + "pdf" + File.separator + "template.pdf";
        String toPath = UlegalZCUtil.rootPath() + File.separator + "pdf" + File.separator + copyRightsVo.getOrderId() + ".pdf";
        try {
            PdfDocument pdfDoc = new PdfDocument(new PdfReader(filePath), new PdfWriter(toPath));
            PdfAcroForm pdfAcroForm = PdfAcroForm.getAcroForm(pdfDoc, true);
            pdfAcroForm.getField("fileName").setValue(copyRightsVo.getFileName());
            pdfAcroForm.getField("applicant").setValue(copyRightsVo.getOwner());
            Date date = new Date();
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
            String cmmitTime = simpleDateFormat.format(date);
            pdfAcroForm.getField("time").setValue(cmmitTime);
            pdfAcroForm.getField("hashValue").setValue(copyRightsVo.getHashValue());
            pdfAcroForm.flattenFields();
            pdfDoc.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
