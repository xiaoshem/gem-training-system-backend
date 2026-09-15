package cn.org.alan.exam.model.dto.payment;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AlipayTradeQueryResult {
    private boolean success;
    private String orderNo;
    private String transactionNo;
    private String tradeStatus;
    private BigDecimal totalAmount;
    private String message;

    public boolean isPaid() {
        return success && ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus));
    }
}
