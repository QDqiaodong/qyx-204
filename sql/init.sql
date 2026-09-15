SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

CREATE TABLE IF NOT EXISTS building (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    building_code VARCHAR(50) NOT NULL UNIQUE COMMENT '楼栋编号',
    building_name VARCHAR(100) NOT NULL COMMENT '楼栋名称',
    floor_count INT DEFAULT 0 COMMENT '楼层数',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='楼栋表';

CREATE TABLE IF NOT EXISTS washbasin (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    washbasin_code VARCHAR(50) NOT NULL UNIQUE COMMENT '洗漱台编号',
    capacity INT NOT NULL COMMENT '可容纳人数',
    building_id BIGINT NOT NULL COMMENT '安装楼栋ID',
    location VARCHAR(100) DEFAULT '' COMMENT '安装位置',
    status TINYINT DEFAULT 1 COMMENT '状态：1正常 0停用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (building_id) REFERENCES building(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='洗漱台表';

CREATE TABLE IF NOT EXISTS living_unit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    unit_code VARCHAR(50) NOT NULL UNIQUE COMMENT '单元编号',
    building_id BIGINT NOT NULL COMMENT '所属楼栋ID',
    floor INT DEFAULT 0 COMMENT '所在楼层',
    room_count INT DEFAULT 0 COMMENT '房间数',
    resident_count INT NOT NULL DEFAULT 0 COMMENT '居住人数',
    status TINYINT DEFAULT 1 COMMENT '状态：1正常 0停用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (building_id) REFERENCES building(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='居住单元表';

CREATE TABLE IF NOT EXISTS unit_washbasin_binding (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    unit_id BIGINT NOT NULL COMMENT '居住单元ID',
    washbasin_id BIGINT NOT NULL COMMENT '洗漱台ID',
    binding_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
    status TINYINT DEFAULT 1 COMMENT '绑定状态：1有效 0失效',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (unit_id) REFERENCES living_unit(id) ON DELETE CASCADE,
    FOREIGN KEY (washbasin_id) REFERENCES washbasin(id) ON DELETE CASCADE,
    UNIQUE KEY uk_unit_washbasin (unit_id, washbasin_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='单元洗漱台绑定关系表';

CREATE TABLE IF NOT EXISTS shift_quota_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    building_id BIGINT NOT NULL COMMENT '楼栋ID',
    quota_date DATE NOT NULL COMMENT '定额所属自然日',
    duty_person VARCHAR(50) NOT NULL COMMENT '值班人',
    quota_capacity INT NOT NULL COMMENT '定额可洗人数',
    shift_start DATETIME NOT NULL COMMENT '当班开始时间',
    shift_end DATETIME NOT NULL COMMENT '当班结束时间',
    status VARCHAR(10) NOT NULL DEFAULT 'OPEN' COMMENT '状态：OPEN未结 CLOSED已结',
    reopen_reason VARCHAR(255) DEFAULT NULL COMMENT '补开原因',
    closed_at DATETIME DEFAULT NULL COMMENT '结案时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    open_flag TINYINT GENERATED ALWAYS AS (IF(status = 'OPEN', 1, NULL)) STORED COMMENT '未结标记：未结为1，已结为NULL',
    UNIQUE KEY uk_building_open (building_id, open_flag),
    KEY idx_building_date (building_id, quota_date),
    FOREIGN KEY (building_id) REFERENCES building(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='楼栋当班用水定额单';

CREATE TABLE IF NOT EXISTS matching_check_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    unit_id BIGINT NOT NULL COMMENT '居住单元ID',
    washbasin_id BIGINT COMMENT '洗漱台ID',
    check_type VARCHAR(50) NOT NULL COMMENT '校验类型：BIND/UNBIND/UPDATE',
    unit_resident_count INT NOT NULL COMMENT '单元居住人数',
    total_capacity INT NOT NULL COMMENT '总容纳容量',
    check_result VARCHAR(20) NOT NULL COMMENT '校验结果：PASS/WARN/FAIL',
    check_message TEXT COMMENT '校验消息',
    operator VARCHAR(50) DEFAULT 'system' COMMENT '操作人',
    check_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '校验时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='匹配校验记录表';

CREATE TABLE IF NOT EXISTS matching_check_record_1 (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    unit_id BIGINT NOT NULL COMMENT '居住单元ID',
    washbasin_id BIGINT COMMENT '洗漱台ID',
    check_type VARCHAR(50) NOT NULL COMMENT '校验类型：BIND/UNBIND/UPDATE',
    unit_resident_count INT NOT NULL COMMENT '单元居住人数',
    total_capacity INT NOT NULL COMMENT '总容纳容量',
    check_result VARCHAR(20) NOT NULL COMMENT '校验结果：PASS/WARN/FAIL',
    check_message TEXT COMMENT '校验消息',
    operator VARCHAR(50) DEFAULT 'system' COMMENT '操作人',
    check_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '校验时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='匹配校验记录表-楼栋1';

CREATE TABLE IF NOT EXISTS matching_check_record_2 (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    unit_id BIGINT NOT NULL COMMENT '居住单元ID',
    washbasin_id BIGINT COMMENT '洗漱台ID',
    check_type VARCHAR(50) NOT NULL COMMENT '校验类型：BIND/UNBIND/UPDATE',
    unit_resident_count INT NOT NULL COMMENT '单元居住人数',
    total_capacity INT NOT NULL COMMENT '总容纳容量',
    check_result VARCHAR(20) NOT NULL COMMENT '校验结果：PASS/WARN/FAIL',
    check_message TEXT COMMENT '校验消息',
    operator VARCHAR(50) DEFAULT 'system' COMMENT '操作人',
    check_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '校验时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='匹配校验记录表-楼栋2';

CREATE TABLE IF NOT EXISTS matching_check_record_3 (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    unit_id BIGINT NOT NULL COMMENT '居住单元ID',
    washbasin_id BIGINT COMMENT '洗漱台ID',
    check_type VARCHAR(50) NOT NULL COMMENT '校验类型：BIND/UNBIND/UPDATE',
    unit_resident_count INT NOT NULL COMMENT '单元居住人数',
    total_capacity INT NOT NULL COMMENT '总容纳容量',
    check_result VARCHAR(20) NOT NULL COMMENT '校验结果：PASS/WARN/FAIL',
    check_message TEXT COMMENT '校验消息',
    operator VARCHAR(50) DEFAULT 'system' COMMENT '操作人',
    check_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '校验时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='匹配校验记录表-楼栋3';

INSERT INTO building (building_code, building_name, floor_count) VALUES
('B001', '一号宿舍楼', 6),
('B002', '二号宿舍楼', 6),
('B003', '三号宿舍楼', 5);

INSERT INTO washbasin (washbasin_code, capacity, building_id, location, status) VALUES
('W001', 20, 1, '一层东侧', 1),
('W002', 20, 1, '一层西侧', 1),
('W003', 15, 2, '一层东侧', 1),
('W004', 15, 2, '一层西侧', 1),
('W005', 10, 3, '一层', 1);

INSERT INTO living_unit (unit_code, building_id, floor, room_count, resident_count, status) VALUES
('U001', 1, 1, 4, 16, 1),
('U002', 1, 2, 4, 18, 1),
('U003', 1, 3, 4, 15, 1),
('U004', 2, 1, 4, 12, 1),
('U005', 2, 2, 4, 14, 1),
('U006', 3, 1, 3, 8, 1),
('U007', 3, 2, 3, 7, 1);
