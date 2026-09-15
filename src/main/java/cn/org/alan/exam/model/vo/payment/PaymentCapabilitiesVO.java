package cn.org.alan.exam.model.vo.payment;

import lombok.Data;

@Data
public class PaymentCapabilitiesVO {
    private boolean localSimulationEnabled;
    private boolean alipaySandboxEnabled;
    private boolean alipaySandboxConfigured;
    private boolean alipaySandboxAvailable;
}
