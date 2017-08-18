package cn.ulegal.zc.dao.impl;

import cn.ulegal.zc.dao.UlegalZCDao;
import cn.ulegal.zc.model.CopyRightsModel;
import com.google.gson.Gson;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * Created by Joe on 2017/8/15.
 */
@Repository
public class UlegalZCDaoImpl<T> implements UlegalZCDao<T> {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    Gson gson;

    @Override
    public Integer getCountByOrderId(String orderId) {
        String sql = " select count(*) from copyrights" + " where order_id = '" +orderId + "';";
        int count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count;
    }

    @Override
    public CopyRightsModel getCountByHashValue(String hashValue) {
        String sql = " select * from copyrights" + " where hash_value = '" +hashValue + "';";
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
        CopyRightsModel model = null;
        if(CollectionUtils.isNotEmpty(list)) {
            model = gson.fromJson(gson.toJson(list.get(0)), CopyRightsModel.class);
        }
        return model;
    }

    @Override
    public Integer saveSubmitInfo(T model) {
        SimpleJdbcInsert insert = new SimpleJdbcInsert(jdbcTemplate);
        insert.withTableName("copyrights");
        SqlParameterSource source = new BeanPropertySqlParameterSource(model);
        Number newId = insert.execute(source);
        return newId.intValue();
    }

    @Override
    public Integer updateCuringId(CopyRightsModel copyRightsModel) {
        String sql = "UPDATE copyrights SET curing_key = '" + copyRightsModel.getCuringKey() + "' WHERE order_id = '" + copyRightsModel.getOrderId() + "';";
        Integer result = jdbcTemplate.update(sql);
        return result;
    }
}
