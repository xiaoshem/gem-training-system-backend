-- V004: Alipay sandbox payment support.
-- Apply after V003__training_enrollment_audit_and_payment.sql.

ALTER TABLE t_payment_order
    ADD COLUMN payment_channel varchar(20) COLLATE utf8mb4_bin DEFAULT NULL
        COMMENT 'SIMULATED/ALIPAY_SANDBOX' AFTER status,
    MODIFY COLUMN transaction_no varchar(64) COLLATE utf8mb4_bin DEFAULT NULL
        COMMENT '支付平台交易流水号';

ALTER TABLE t_payment_record
    MODIFY COLUMN transaction_no varchar(64) COLLATE utf8mb4_bin NOT NULL
        COMMENT '支付平台交易流水号',
    MODIFY COLUMN payment_method varchar(20) COLLATE utf8mb4_bin NOT NULL DEFAULT 'SIMULATED'
        COMMENT 'SIMULATED/ALIPAY_SANDBOX';
