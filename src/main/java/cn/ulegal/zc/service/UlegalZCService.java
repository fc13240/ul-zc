package cn.ulegal.zc.service;

import cn.ulegal.zc.model.CopyRightsModel;
import cn.ulegal.zc.vo.CopyRightsVo;

/**
 * Created by Joe on 2017/8/15.
 */
public interface UlegalZCService {
    Boolean getCountByOrderId(String orderId);
    CopyRightsVo saveOrderInfo(CopyRightsVo copyRightsVo);
}
