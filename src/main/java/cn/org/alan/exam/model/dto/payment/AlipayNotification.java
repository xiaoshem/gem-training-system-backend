package cn.org.alan.exam.model.dto.payment;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AlipayNotification {
    private String orderNo;
    private String transactionNo;
    private String tradeStatus;
    private BigDecimal totalAmount;

    public boolean isPaid() {
        return "TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus);
    }
}
