package cn.org.alan.exam.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "payment")
public class PaymentProperties {
    private boolean localSimulationEnabled = true;
    private AlipaySandbox alipaySandbox = new AlipaySandbox();

    public boolean isAlipaySandboxConfigured() {
        return alipaySandbox != null
                && hasText(alipaySandbox.getGatewayUrl())
                && hasText(alipaySandbox.getAppId())
                && hasText(alipaySandbox.getPrivateKey())
                && hasText(alipaySandbox.getAlipayPublicKey());
    }

    public boolean isAlipaySandboxAvailable() {
        return alipaySandbox != null && alipaySandbox.isEnabled() && isAlipaySandboxConfigured();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    @Data
    public static class AlipaySandbox {
        private boolean enabled;
        private String gatewayUrl;
        private String appId;
        private String privateKey;
        private String alipayPublicKey;
        private String sellerId;
        private String notifyUrl;
        private String returnUrl;
        private String frontendReturnUrl;
        private String charset = "UTF-8";
        private String signType = "RSA2";
    }
}
