package cn.ulegal.zc.dao.impl;

import cn.ulegal.zc.dao.UlegalZCDao;
import cn.ulegal.zc.model.CopyRightsModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

/**
 * Created by Joe on 2017/8/15.
 */
@Repository
public class UlegalZCDaoImpl<T> implements UlegalZCDao<T> {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Override
    public Integer getCountByOrderId(String orderId) {
        String sql = " select count(*) from copyrights" + " where order_id = '" +orderId + "';";
        int count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count;
    }

    @Override
    public Integer saveSubmitInfo(T model) {
        SimpleJdbcInsert insert = new SimpleJdbcInsert(jdbcTemplate);
        insert.withTableName("copyrights");
        SqlParameterSource source = new BeanPropertySqlParameterSource(model);
        Number newId = insert.execute(source);
        return newId.intValue();
    }
}
