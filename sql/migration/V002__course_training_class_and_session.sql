-- V002: course catalogue, instructor profiles, training classes, sessions and user messages.
-- Apply after V001__roles_and_user_profile.sql.

CREATE TABLE IF NOT EXISTS t_instructor_profile (
    id int NOT NULL AUTO_INCREMENT COMMENT '讲师档案ID',
    user_id int NOT NULL COMMENT '关联t_user，必须为培训讲师角色',
    professional_title varchar(100) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '职称',
    specialties varchar(500) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '擅长领域',
    qualification_certificate varchar(500) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '资质证书',
    introduction text COLLATE utf8mb4_bin COMMENT '讲师简介',
    status varchar(20) COLLATE utf8mb4_bin NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
    created_by int DEFAULT NULL COMMENT '创建人ID',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    active_user_id int GENERATED ALWAYS AS (CASE WHEN is_deleted = 0 THEN user_id ELSE NULL END) STORED COMMENT '仅用于约束有效讲师档案唯一',
    PRIMARY KEY (id),
    UNIQUE KEY uk_instructor_active_user (active_user_id),
    KEY idx_instructor_status (status, is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='培训讲师档案';

CREATE TABLE IF NOT EXISTS t_course (
    id int NOT NULL AUTO_INCREMENT COMMENT '课程ID',
    course_code varchar(32) COLLATE utf8mb4_bin NOT NULL COMMENT '课程编码',
    course_name varchar(100) COLLATE utf8mb4_bin NOT NULL COMMENT '课程名称',
    course_category varchar(50) COLLATE utf8mb4_bin NOT NULL COMMENT '课程类别',
    course_description varchar(1000) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '课程简介',
    cover_image varchar(500) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '封面图片',
    syllabus text COLLATE utf8mb4_bin NOT NULL COMMENT '课程大纲',
    total_hours int NOT NULL COMMENT '总课时',
    target_audience varchar(500) COLLATE utf8mb4_bin NOT NULL COMMENT '适用对象',
    prerequisites varchar(1000) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '前置要求',
    default_instructor_id int DEFAULT NULL COMMENT '默认讲师用户ID',
    skill_indicators text COLLATE utf8mb4_bin COMMENT '技能指标',
    scoring_rules text COLLATE utf8mb4_bin COMMENT '评分细则',
    status varchar(20) COLLATE utf8mb4_bin NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
    created_by int DEFAULT NULL COMMENT '创建人ID',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_course_code (course_code),
    KEY idx_course_name (course_name),
    KEY idx_course_status (status, is_deleted),
    KEY idx_course_default_instructor (default_instructor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='培训课程';

CREATE TABLE IF NOT EXISTS t_course_instructor (
    id int NOT NULL AUTO_INCREMENT COMMENT '主键',
    course_id int NOT NULL COMMENT '课程ID',
    instructor_id int NOT NULL COMMENT '讲师用户ID',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_course_instructor (course_id, instructor_id),
    KEY idx_course_instructor_user (instructor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='课程讲师关联';

CREATE TABLE IF NOT EXISTS t_training_class (
    id int NOT NULL AUTO_INCREMENT COMMENT '培训班次ID',
    class_code varchar(32) COLLATE utf8mb4_bin NOT NULL COMMENT '班次编码',
    course_id int NOT NULL COMMENT '课程ID',
    class_name varchar(150) COLLATE utf8mb4_bin NOT NULL COMMENT '班次名称',
    instructor_id int NOT NULL COMMENT '主讲师用户ID',
    enrollment_start datetime NOT NULL COMMENT '报名开始时间',
    enrollment_end datetime NOT NULL COMMENT '报名结束时间',
    start_date date NOT NULL COMMENT '开课日期',
    end_date date NOT NULL COMMENT '结课日期',
    location varchar(255) COLLATE utf8mb4_bin NOT NULL COMMENT '上课地点',
    capacity int NOT NULL COMMENT '招生名额',
    fee decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '培训费用',
    class_status varchar(20) COLLATE utf8mb4_bin NOT NULL DEFAULT 'PLANNED' COMMENT 'PLANNED/ENROLLING/UPCOMING/IN_PROGRESS/COMPLETED/EXPIRED/CANCELLED',
    publish_status varchar(20) COLLATE utf8mb4_bin NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED',
    publish_time datetime DEFAULT NULL COMMENT '发布时间',
    version int NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_by int DEFAULT NULL COMMENT '创建人ID',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_training_class_code (class_code),
    KEY idx_training_class_course (course_id, is_deleted),
    KEY idx_training_class_instructor (instructor_id, is_deleted),
    KEY idx_training_class_visibility (publish_status, class_status, is_deleted),
    KEY idx_training_class_dates (enrollment_start, enrollment_end, start_date, end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='培训班次';

CREATE TABLE IF NOT EXISTS t_training_session (
    id int NOT NULL AUTO_INCREMENT COMMENT '课次ID',
    training_class_id int NOT NULL COMMENT '培训班次ID',
    session_no int NOT NULL COMMENT '班次内课次序号',
    topic varchar(200) COLLATE utf8mb4_bin NOT NULL COMMENT '课次主题',
    content varchar(2000) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '授课内容',
    start_time datetime NOT NULL COMMENT '开始时间',
    end_time datetime NOT NULL COMMENT '结束时间',
    location varchar(255) COLLATE utf8mb4_bin NOT NULL COMMENT '地点',
    instructor_id int NOT NULL COMMENT '讲师用户ID',
    status varchar(20) COLLATE utf8mb4_bin NOT NULL DEFAULT 'PLANNED' COMMENT 'PLANNED/COMPLETED/CANCELLED',
    created_by int DEFAULT NULL COMMENT '创建人ID',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    active_training_class_id int GENERATED ALWAYS AS (CASE WHEN is_deleted = 0 THEN training_class_id ELSE NULL END) STORED COMMENT '仅用于约束有效课次序号唯一',
    PRIMARY KEY (id),
    UNIQUE KEY uk_training_session_no (active_training_class_id, session_no),
    KEY idx_training_session_time (training_class_id, start_time, end_time),
    KEY idx_training_session_instructor (instructor_id, status, is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='培训课次';

CREATE TABLE IF NOT EXISTS t_user_message (
    id int NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    receiver_id int NOT NULL COMMENT '接收用户ID',
    title varchar(200) COLLATE utf8mb4_bin NOT NULL COMMENT '消息标题',
    content varchar(2000) COLLATE utf8mb4_bin NOT NULL COMMENT '消息内容',
    message_type varchar(30) COLLATE utf8mb4_bin NOT NULL DEFAULT 'SYSTEM' COMMENT '消息类型',
    business_type varchar(30) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '业务类型',
    business_id int DEFAULT NULL COMMENT '业务ID',
    is_read tinyint NOT NULL DEFAULT 0 COMMENT '是否已读',
    read_time datetime DEFAULT NULL COMMENT '阅读时间',
    created_by int DEFAULT NULL COMMENT '创建人ID',
    create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    is_deleted tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_user_message_receiver (receiver_id, is_read, is_deleted, create_time),
    KEY idx_user_message_business (business_type, business_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='站内消息';

-- Verification
SHOW TABLES WHERE Tables_in_db_exam IN (
    't_instructor_profile', 't_course', 't_course_instructor',
    't_training_class', 't_training_session', 't_user_message'
);
