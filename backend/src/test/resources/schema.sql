CREATE TABLE building (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    building_code VARCHAR(50) NOT NULL UNIQUE,
    building_name VARCHAR(100) NOT NULL,
    floor_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE washbasin (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    washbasin_code VARCHAR(50) NOT NULL UNIQUE,
    capacity INT NOT NULL,
    building_id BIGINT NOT NULL,
    location VARCHAR(100) DEFAULT '',
    status TINYINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE living_unit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    unit_code VARCHAR(50) NOT NULL UNIQUE,
    building_id BIGINT NOT NULL,
    floor INT DEFAULT 0,
    room_count INT DEFAULT 0,
    resident_count INT NOT NULL DEFAULT 0,
    status TINYINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- H2 不接受 ON UPDATE CURRENT_TIMESTAMP 等 MySQL 专有子句，统一按通用语法建表
CREATE TABLE unit_washbasin_binding (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    unit_id BIGINT NOT NULL,
    washbasin_id BIGINT NOT NULL,
    binding_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status TINYINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_unit_washbasin (unit_id, washbasin_id)
);

CREATE TABLE shift_quota_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    building_id BIGINT NOT NULL,
    quota_date DATE NOT NULL,
    duty_person VARCHAR(50) NOT NULL,
    quota_capacity INT NOT NULL,
    shift_start DATETIME NOT NULL,
    shift_end DATETIME NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'OPEN',
    reopen_reason VARCHAR(255) DEFAULT NULL,
    closed_at DATETIME DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE matching_check_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    unit_id BIGINT NOT NULL,
    washbasin_id BIGINT,
    check_type VARCHAR(50) NOT NULL,
    unit_resident_count INT NOT NULL,
    total_capacity INT NOT NULL,
    check_result VARCHAR(20) NOT NULL,
    check_message CLOB,
    operator VARCHAR(50) DEFAULT 'system',
    check_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE matching_check_record_1 (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    unit_id BIGINT NOT NULL,
    washbasin_id BIGINT,
    check_type VARCHAR(50) NOT NULL,
    unit_resident_count INT NOT NULL,
    total_capacity INT NOT NULL,
    check_result VARCHAR(20) NOT NULL,
    check_message CLOB,
    operator VARCHAR(50) DEFAULT 'system',
    check_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
