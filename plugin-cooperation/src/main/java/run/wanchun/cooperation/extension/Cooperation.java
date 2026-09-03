package run.wanchun.cooperation.extension;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;

/**
 * 合作咨询 Extension，用于持久化表单提交记录。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@GVK(group = "api.cooperation.vivid.run",
    version = "v1alpha1",
    kind = "Cooperation",
    plural = "cooperations",
    singular = "cooperation")
public class Cooperation extends AbstractExtension {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private CooperationSpec spec;

    @Data
    public static class CooperationSpec {
        private String company;
        private String contact;
        private String phone;
        private String type;
        private String typeLabel;
        private String message;
        private String website;
        private String sourceUrl;
        private String userAgent;
        private String timestamp;
        private String ip;
        @Schema(description = "是否已处理", defaultValue = "false")
        private Boolean handled = false;
    }
}
