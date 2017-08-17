package cn.ulegal.zc.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.gson.annotations.SerializedName;

import java.util.Date;

/**
 * Created by Joe on 2017/8/14.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CopyRightsModel {
    @SerializedName(value="order_id", alternate="orderId")
    String orderId;
    String source;
    String owner;
    @SerializedName(value="id_card_no", alternate="idCardNo")
    String idCardNo;
    @SerializedName(value="hash_value", alternate="hashValue")
    String hashValue;
    Long timestamp = new Date().getTime();
    Integer status;
    @SerializedName(value="curing_key", alternate="curingKey")
    String curingKey;
    @SerializedName(value="file_path", alternate="filePath")
    String filePath;
    String title;
    String email;
    String mobile;

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getIdCardNo() {
        return idCardNo;
    }

    public void setIdCardNo(String idCardNo) {
        this.idCardNo = idCardNo;
    }

    public String getHashValue() {
        return hashValue;
    }

    public void setHashValue(String hashValue) {
        this.hashValue = hashValue;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getCuringKey() {
        return curingKey;
    }

    public void setCuringKey(String curingKey) {
        this.curingKey = curingKey;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }
}