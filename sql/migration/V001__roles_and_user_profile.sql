-- V001: add the certification auditor role and trainee profile fields.
-- Apply this migration after sql/db_exam.sql.

START TRANSACTION;

INSERT INTO t_role (id, role_name, code)
SELECT 4, '认证审核员', 'auditor'
WHERE NOT EXISTS (
    SELECT 1 FROM t_role WHERE code = 'auditor'
);

COMMIT;

ALTER TABLE t_user
    ADD COLUMN id_card varchar(255) NULL COMMENT '身份证号' AFTER real_name,
    ADD COLUMN organization varchar(150) NULL COMMENT '所在企业或单位' AFTER id_card,
    ADD COLUMN position varchar(100) NULL COMMENT '岗位' AFTER organization,
    ADD COLUMN phone varchar(50) NULL COMMENT '联系电话' AFTER position;

ALTER TABLE t_role
    ADD UNIQUE KEY uk_t_role_code (code);

-- Verification
SELECT id, role_name, code FROM t_role WHERE code = 'auditor';
SHOW COLUMNS FROM t_user WHERE Field IN ('id_card', 'organization', 'position', 'phone');
