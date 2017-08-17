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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
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
//        Map curingResult = curingEvidence(copyRightsModel);
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

    /** 电子证据固化
     *
     * @return
     */
    private Map curingEvidence(CopyRightsModel copyRightsModel) {
        String url = "https://api.ulegal.cn:9091/evidenceCuring";
        Map data = new HashMap();
        data.put("owner", copyRightsModel.getOwner());
        data.put("title", copyRightsModel.getTitle());
        data.put("source", copyRightsModel.getSource());
        data.put("hashValue", copyRightsModel.getHashValue());
        Map result = restTemplate.postForObject(url, data, Map.class);
        return result;
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
        Map content = gson.fromJson(gson.toJson(copyRightsModel), Map.class);
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
        data.put("info_key", content.get("order_id"));
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
