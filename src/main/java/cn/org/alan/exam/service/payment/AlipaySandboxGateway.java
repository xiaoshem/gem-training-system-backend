package cn.org.alan.exam.service.payment;

import cn.org.alan.exam.common.exception.ServiceRuntimeException;
import cn.org.alan.exam.config.PaymentProperties;
import cn.org.alan.exam.model.dto.payment.AlipayNotification;
import cn.org.alan.exam.model.dto.payment.AlipayTradeQueryResult;
import cn.org.alan.exam.model.entity.PaymentOrder;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Map;

@Component
public class AlipaySandboxGateway {
    @Resource
    private PaymentProperties paymentProperties;

    public String createPagePay(PaymentOrder order, String subject) {
        ensureAvailable();
        try {
            AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
            AlipayTradePagePayModel model = new AlipayTradePagePayModel();
            model.setOutTradeNo(order.getOrderNo());
            model.setTotalAmount(order.getAmount().setScale(2).toPlainString());
            model.setSubject(limit(subject, 256));
            model.setBody("人工宝石产业人才培训缴费");
            model.setProductCode("FAST_INSTANT_TRADE_PAY");
            model.setTimeoutExpress("30m");
            request.setBizModel(model);

            PaymentProperties.AlipaySandbox properties = properties();
            if (hasText(properties.getNotifyUrl())) {
                request.setNotifyUrl(properties.getNotifyUrl().trim());
            }
            if (hasText(properties.getReturnUrl())) {
                request.setReturnUrl(properties.getReturnUrl().trim());
            }

            AlipayTradePagePayResponse response = client().pageExecute(request);
            if (!response.isSuccess() || !hasText(response.getBody())) {
                throw new ServiceRuntimeException(errorMessage("创建支付宝沙箱订单失败", response.getSubMsg()));
            }
            return response.getBody();
        } catch (AlipayApiException exception) {
            throw new ServiceRuntimeException("调用支付宝沙箱失败：" + safeMessage(exception.getMessage()));
        }
    }

    public AlipayNotification verifyNotification(Map<String, String> parameters) {
        verifySignedParameters(parameters);
        AlipayNotification notification = new AlipayNotification();
        notification.setOrderNo(required(parameters, "out_trade_no"));
        notification.setTransactionNo(required(parameters, "trade_no"));
        notification.setTradeStatus(required(parameters, "trade_status"));
        notification.setTotalAmount(parseAmount(required(parameters, "total_amount")));
        return notification;
    }

    public AlipayNotification verifyReturn(Map<String, String> parameters) {
        verifySignedParameters(parameters);
        AlipayNotification result = new AlipayNotification();
        result.setOrderNo(required(parameters, "out_trade_no"));
        result.setTransactionNo(required(parameters, "trade_no"));
        result.setTotalAmount(parseAmount(required(parameters, "total_amount")));
        result.setTradeStatus("TRADE_SUCCESS");
        return result;
    }

    public AlipayTradeQueryResult query(String orderNo) {
        ensureAvailable();
        try {
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            AlipayTradeQueryModel model = new AlipayTradeQueryModel();
            model.setOutTradeNo(orderNo);
            request.setBizModel(model);
            AlipayTradeQueryResponse response = client().execute(request);

            AlipayTradeQueryResult result = new AlipayTradeQueryResult();
            result.setSuccess(response.isSuccess());
            result.setOrderNo(orderNo);
            result.setTransactionNo(response.getTradeNo());
            result.setTradeStatus(response.getTradeStatus());
            if (hasText(response.getTotalAmount())) {
                result.setTotalAmount(parseAmount(response.getTotalAmount()));
            }
            result.setMessage(response.isSuccess()
                    ? "支付宝交易查询成功"
                    : errorMessage(response.getMsg(), response.getSubMsg()));
            return result;
        } catch (AlipayApiException exception) {
            throw new ServiceRuntimeException("查询支付宝沙箱交易失败：" + safeMessage(exception.getMessage()));
        }
    }

    public boolean isAvailable() {
        return paymentProperties.isAlipaySandboxAvailable();
    }

    private void verifySignedParameters(Map<String, String> parameters) {
        ensureAvailable();
        try {
            PaymentProperties.AlipaySandbox properties = properties();
            boolean valid = AlipaySignature.rsaCheckV1(parameters, properties.getAlipayPublicKey(),
                    properties.getCharset(), properties.getSignType());
            if (!valid) {
                throw new ServiceRuntimeException("支付宝回调签名验证失败");
            }
            if (!properties.getAppId().equals(parameters.get("app_id"))) {
                throw new ServiceRuntimeException("支付宝回调的应用ID不匹配");
            }
            if (hasText(properties.getSellerId())
                    && !properties.getSellerId().equals(parameters.get("seller_id"))) {
                throw new ServiceRuntimeException("支付宝回调的收款账号不匹配");
            }
        } catch (AlipayApiException exception) {
            throw new ServiceRuntimeException("支付宝回调验签异常：" + safeMessage(exception.getMessage()));
        }
    }

    private AlipayClient client() {
        PaymentProperties.AlipaySandbox properties = properties();
        return new DefaultAlipayClient(properties.getGatewayUrl().trim(), properties.getAppId().trim(),
                properties.getPrivateKey().trim(), "json", properties.getCharset(),
                properties.getAlipayPublicKey().trim(), properties.getSignType());
    }

    private PaymentProperties.AlipaySandbox properties() {
        return paymentProperties.getAlipaySandbox();
    }

    private void ensureAvailable() {
        if (!paymentProperties.isAlipaySandboxAvailable()) {
            throw new ServiceRuntimeException("支付宝沙箱未启用或配置不完整");
        }
    }

    private String required(Map<String, String> parameters, String key) {
        String value = parameters == null ? null : parameters.get(key);
        if (!hasText(value)) {
            throw new ServiceRuntimeException("支付宝回调缺少参数：" + key);
        }
        return value.trim();
    }

    private BigDecimal parseAmount(String value) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            throw new ServiceRuntimeException("支付宝返回的金额格式不正确");
        }
    }

    private String errorMessage(String message, String subMessage) {
        String first = safeMessage(message);
        String second = safeMessage(subMessage);
        return "未知状态".equals(second) ? first : first + "（" + second + "）";
    }

    private String safeMessage(String message) {
        if (!hasText(message)) {
            return "未知状态";
        }
        String value = message.trim();
        String lower = value.toLowerCase();
        if ((value.contains("<") && value.contains(">"))
                || lower.contains("requested url was not found")
                || lower.contains("powered by tengine")) {
            return "支付宝沙箱服务返回异常响应，请稍后重试";
        }
        return value.length() > 200 ? value.substring(0, 200) + "…" : value;
    }

    private String limit(String value, int maxLength) {
        String safe = hasText(value) ? value.trim() : "培训班次缴费";
        return safe.length() <= maxLength ? safe : safe.substring(0, maxLength);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
