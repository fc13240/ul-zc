package cn.ulegal.zc.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Created by Joe on 2017/8/15.
 *  接口统一返回vo类
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageVo {
    // 状态
    public Integer code;
    // 时间戳
    public Long timeStamp;
    // 数据
    public Object data;

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public Long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
