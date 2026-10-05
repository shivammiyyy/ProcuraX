package com.procurax.payment.service;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SandboxPaymentProcessor {

    public String authorize() {
        return "sbx_auth_" + UUID.randomUUID();
    }

    public String capture() {
        return "sbx_capture_" + UUID.randomUUID();
    }

    public String refund() {
        return "sbx_refund_" + UUID.randomUUID();
    }
}
