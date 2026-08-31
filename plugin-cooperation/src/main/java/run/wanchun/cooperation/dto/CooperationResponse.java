package run.wanchun.cooperation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应体。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CooperationResponse {
    private boolean success;
    private String message;

    public static CooperationResponse ok(String message) {
        return new CooperationResponse(true, message);
    }

    public static CooperationResponse fail(String message) {
        return new CooperationResponse(false, message);
    }
}
