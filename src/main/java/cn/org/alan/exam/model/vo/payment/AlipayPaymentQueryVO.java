package cn.org.alan.exam.model.vo.payment;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AlipayPaymentQueryVO {
    private String orderNo;
    private String orderStatus;
    private String gatewayStatus;
    private String message;
}
