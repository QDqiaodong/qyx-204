-- 洗漱台送检台账 - 存量库增量迁移脚本
-- 全新部署由 init.sql 自动建表；已在运行的库先 USE 到 .env 中 MYSQL_DATABASE 指定的库，
-- 再执行本脚本即可（可重复执行）。

CREATE TABLE IF NOT EXISTS washbasin_repair_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    washbasin_id BIGINT NOT NULL COMMENT '送检洗漱台ID',
    damage_part VARCHAR(255) NOT NULL COMMENT '损坏部位',
    duty_person VARCHAR(50) NOT NULL COMMENT '经办值班员',
    operator VARCHAR(50) DEFAULT 'system' COMMENT '开单操作人',
    status VARCHAR(10) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING待接单 REPAIRED已修复',
    open_flag TINYINT DEFAULT 1 COMMENT '未结标记：待接单为1，已修复为NULL',
    repair_note VARCHAR(255) DEFAULT NULL COMMENT '修复备注',
    repaired_at DATETIME DEFAULT NULL COMMENT '修复时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '送检开始时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_washbasin_open (washbasin_id, open_flag),
    KEY idx_washbasin_status (washbasin_id, status),
    FOREIGN KEY (washbasin_id) REFERENCES washbasin(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='洗漱台送检单';
