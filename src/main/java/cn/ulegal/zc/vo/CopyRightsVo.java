package cn.ulegal.zc.vo;

import cn.ulegal.zc.model.CopyRightsModel;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Joe on 2017/8/16.
 *  zc信息提交vo类
 */
public class CopyRightsVo extends CopyRightsModel{
    @SerializedName(value="file_name", alternate="fileName")
    String fileName;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
