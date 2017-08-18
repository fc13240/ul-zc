package cn.ulegal.zc.dao;

import cn.ulegal.zc.model.CopyRightsModel;

/**
 * Created by Joe on 2017/8/15.
 */
public interface UlegalZCDao<T> {
    Integer getCountByOrderId(String orderId);
    Integer saveSubmitInfo(T model);
    CopyRightsModel getCountByHashValue(String hashValue);
    Integer updateCuringId(CopyRightsModel copyRightsModel);
}
