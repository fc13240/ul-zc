package cn.ulegal.zc.dao;

/**
 * Created by Joe on 2017/8/15.
 */
public interface UlegalZCDao<T> {
    Integer getCountByOrderId(String orderId);
    Integer saveSubmitInfo(T model);
}
