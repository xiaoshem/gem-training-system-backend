-- V003: training enrollment, audit trail and simulated payment.
-- Apply after V002__course_training_class_and_session.sql.

CREATE TABLE IF NOT EXISTS t_training_enrollment (
    id int NOT NULL AUTO_INCREMENT COMMENT '报名ID',
    enrollment_no varchar(40) COLLATE utf8mb4_bin NOT NULL COMMENT '报名编号',
    training_class_id int NOT NULL COMMENT '培训班次ID',
    student_id int NOT NULL COMMENT '报名学员ID',
    snapshot_real_name varchar(50) COLLATE utf8mb4_bin NOT NULL COMMENT '报名时真实姓名快照',
    snapshot_id_card varchar(18) COLLATE utf8mb4_bin NOT NULL COMMENT '报名时身份证号快照',
    snapshot_organization varchar(100) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '报名时单位快照',
    snapshot_position varchar(100) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '报名时岗位快照',
    snapshot_phone varchar(20) COLLATE utf8mb4_bin NOT NULL COMMENT '报名时联系电话快照',
    status varchar(20) COLLATE utf8mb4_bin NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/ADMITTED/REJECTED/CANCELLED',
    review_reason varchar(500) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '审核说明',
    reviewed_by int DEFAULT NULL COMMENT '审核人ID',
    reviewed_at datetime DEFAULT NULL COMMENT '审核时间',
    cancelled_at datetime DEFAULT NULL COMMENT '取消时间',
    version int NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '报名时间',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    active_student_key varchar(80) GENERATED ALWAYS AS
        (CASE WHEN is_deleted = 0 AND status IN ('PENDING', 'ADMITTED')
              THEN CONCAT(training_class_id, '-', student_id) ELSE NULL END) STORED
        COMMENT '仅用于约束同一班次有效报名唯一',
    PRIMARY KEY (id),
    UNIQUE KEY uk_training_enrollment_no (enrollment_no),
    UNIQUE KEY uk_training_enrollment_active_student (active_student_key),
    KEY idx_training_enrollment_class_status (training_class_id, status, is_deleted),
    KEY idx_training_enrollment_student (student_id, is_deleted, create_time),
    KEY idx_training_enrollment_review (status, reviewed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='培训报名';

CREATE TABLE IF NOT EXISTS t_enrollment_audit_log (
    id int NOT NULL AUTO_INCREMENT COMMENT '审核日志ID',
    enrollment_id int NOT NULL COMMENT '报名ID',
    action varchar(20) COLLATE utf8mb4_bin NOT NULL COMMENT 'APPLY/ADMIT/REJECT/CANCEL',
    from_status varchar(20) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '原状态',
    to_status varchar(20) COLLATE utf8mb4_bin NOT NULL COMMENT '目标状态',
    reason varchar(500) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '操作说明',
    operator_id int NOT NULL COMMENT '操作人ID',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (id),
    KEY idx_enrollment_audit_enrollment (enrollment_id, create_time),
    KEY idx_enrollment_audit_operator (operator_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='培训报名审核日志';

CREATE TABLE IF NOT EXISTS t_payment_order (
    id int NOT NULL AUTO_INCREMENT COMMENT '缴费订单ID',
    order_no varchar(40) COLLATE utf8mb4_bin NOT NULL COMMENT '订单编号',
    enrollment_id int NOT NULL COMMENT '报名ID',
    student_id int NOT NULL COMMENT '学员ID',
    training_class_id int NOT NULL COMMENT '培训班次ID',
    amount decimal(10,2) NOT NULL COMMENT '应付金额',
    status varchar(20) COLLATE utf8mb4_bin NOT NULL DEFAULT 'UNPAID' COMMENT 'UNPAID/PAID/CANCELLED/REFUNDED',
    transaction_no varchar(40) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '模拟交易流水号',
    paid_at datetime DEFAULT NULL COMMENT '支付时间',
    version int NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_order_no (order_no),
    UNIQUE KEY uk_payment_order_enrollment (enrollment_id),
    UNIQUE KEY uk_payment_order_transaction (transaction_no),
    KEY idx_payment_order_student (student_id, status, is_deleted),
    KEY idx_payment_order_class (training_class_id, status, is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='培训缴费订单';

CREATE TABLE IF NOT EXISTS t_payment_record (
    id int NOT NULL AUTO_INCREMENT COMMENT '支付记录ID',
    payment_order_id int NOT NULL COMMENT '缴费订单ID',
    transaction_no varchar(40) COLLATE utf8mb4_bin NOT NULL COMMENT '模拟交易流水号',
    student_id int NOT NULL COMMENT '学员ID',
    amount decimal(10,2) NOT NULL COMMENT '实付金额',
    payment_method varchar(20) COLLATE utf8mb4_bin NOT NULL DEFAULT 'SIMULATED' COMMENT '支付方式',
    status varchar(20) COLLATE utf8mb4_bin NOT NULL DEFAULT 'SUCCESS' COMMENT '支付状态',
    paid_at datetime NOT NULL COMMENT '支付时间',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_record_order (payment_order_id),
    UNIQUE KEY uk_payment_record_transaction (transaction_no),
    KEY idx_payment_record_student (student_id, paid_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='培训支付流水';

SHOW TABLES WHERE Tables_in_db_exam IN (
    't_training_enrollment', 't_enrollment_audit_log',
    't_payment_order', 't_payment_record'
);
