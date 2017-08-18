package cn.ulegal.zc.controller;

import cn.ulegal.zc.model.CopyRightsModel;
import cn.ulegal.zc.service.UlegalZCService;
import cn.ulegal.zc.util.UlegalZCUtil;
import cn.ulegal.zc.vo.CopyRightsVo;
import cn.ulegal.zc.vo.MessageVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.Part;
import javax.validation.Valid;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Joe on 2017/8/14.
 */
@RestController
@RequestMapping(value="/ulegal")
public class UlegalZCController {

    @Autowired
    private UlegalZCService ulegalZCService;

    /**
     *  订单号验证
     * @param orderId
     * @return
     */
    @GetMapping(value="/validateOrderId/{orderId}")
    public MessageVo validateOrder(@Valid @PathVariable String orderId) {
        MessageVo messageVo = UlegalZCUtil.getMessageVo();
        // 查询订单号是否存在
        boolean result = ulegalZCService.getCountByOrderId(orderId);
        Map data = new HashMap();
        if (!result) {
            messageVo.setCode(200);
        }
        data.put("exist", result);
        messageVo.setData(data);
        return messageVo;
    }

    /**
     *  文件上传
     * @param file
     * @param dirPath
     * @return
     */
    @PostMapping(value="/uploadFile")
    public MessageVo uploadFile(@RequestParam("upfile") Part file, @RequestParam(value="dirPath", required=false) String dirPath) {
        MessageVo messageVo = UlegalZCUtil.getMessageVo();
        String fileName = file.getSubmittedFileName();
        //项目路径
        String dir = UlegalZCUtil.modulePath(dirPath == null ? "zcFile" : dirPath);
        File dateDir = new File(dir);
        if (!dateDir.exists()) {
            dateDir.mkdirs();
        }
        String prefix = fileName.substring(fileName.lastIndexOf(".")+1);
        String fName = UUID.randomUUID().toString().replace("-", "");
        String filePath = File.separator + dir + File.separator + fName + "." + prefix;
        InputStream inputStream = null;
        Map data = new HashMap();
        try {
            inputStream = file.getInputStream();
            System.out.println(UlegalZCUtil.rootPath() + filePath);
            Files.copy(inputStream, Paths.get(UlegalZCUtil.rootPath() + filePath));
            messageVo.setCode(200);
            data.put("fileName", fileName);
            data.put("filePath", filePath);
            messageVo.setData(data);
        } catch (IOException e) {
            e.printStackTrace();
            messageVo.setData("failure");
        }
        return messageVo;
    }

    /**
     *  文件验证
     * @param upFile
     * @return
     */
    @PostMapping(value="/checkFile")
    public MessageVo checkFile(@RequestParam("upfile") MultipartFile upFile) {
        File file = null;
        try {
            file=File.createTempFile(upFile.getName(), null);
            upFile.transferTo(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        MessageVo messageVo = UlegalZCUtil.getMessageVo();
        CopyRightsModel copyRightsModel = ulegalZCService.checkFile(file);
        Map data = new HashMap();
        if (null != copyRightsModel) {
            messageVo.setCode(200);
            data.put("owner", copyRightsModel.getOwner());
            data.put("title", copyRightsModel.getTitle());
            data.put("hashValue", copyRightsModel.getHashValue());
            data.put("timestamp", copyRightsModel.getTimestamp());
        } else {
            data.put("message", "File does not exist");
        }
        messageVo.setData(data);
        return messageVo;
    }

    /**
     *  信息处理及保存
     * @param copyRightsVo
     * @return
     */
    @PostMapping(value="/submitInfo")
    public MessageVo submitInfo(@RequestBody CopyRightsVo copyRightsVo) {
        MessageVo messageVo = UlegalZCUtil.getMessageVo();
        CopyRightsVo result = ulegalZCService.saveOrderInfo(copyRightsVo);
        ulegalZCService.curingEvidence(result);
        if (null != result) {
            messageVo.setCode(200);
            messageVo.setData(result);
        } else {
            Map data = new HashMap();
            data.put("save", "failure");
            messageVo.setData(data);
        }
        return messageVo;
    }
}
