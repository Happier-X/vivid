package run.wanchun.cooperation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 合作表单请求体，与前端契约保持一致。
 */
@Data
public class CooperationRequest {

    public static final String PHONE_RE = "^(1[3-9]\\d{9}|0\\d{2,3}-?\\d{7,8})$";

    @NotBlank(message = "公司名称为必填")
    @Size(min = 2, max = 50, message = "公司名称长度需在 2-50 字符之间")
    private String company;

    @NotBlank(message = "联系人为必填")
    @Size(min = 2, max = 20, message = "联系人长度需在 2-20 字符之间")
    private String contact;

    @NotBlank(message = "联系电话为必填")
    @Pattern(regexp = PHONE_RE, message = "联系电话格式不正确")
    private String phone;

    @NotBlank(message = "合作类型为必填")
    private String type;

    private String typeLabel;

    @Size(max = 500, message = "合作意向说明不能超过 500 字符")
    private String message;

    /** 蜜罐字段，正常用户应为空 */
    private String website;

    private String sourceUrl;
    private String userAgent;
    private String timestamp;
}
