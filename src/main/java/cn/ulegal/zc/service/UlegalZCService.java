package cn.ulegal.zc.service;

import cn.ulegal.zc.model.CopyRightsModel;
import cn.ulegal.zc.vo.CopyRightsVo;

import java.io.File;

/**
 * Created by Joe on 2017/8/15.
 */
public interface UlegalZCService {
    CopyRightsModel checkFile(File file);
    Boolean getCountByOrderId(String orderId);
    CopyRightsVo saveOrderInfo(CopyRightsVo copyRightsVo);
    void curingEvidence(CopyRightsVo copyRightsVo);
}
