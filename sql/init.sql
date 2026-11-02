CREATE DATABASE IF NOT EXISTS ssm_emp DEFAULT CHARACTER SET utf8mb4;
USE ssm_emp;

CREATE TABLE IF NOT EXISTS emp (
    emp_id INT PRIMARY KEY AUTO_INCREMENT COMMENT '员工编号',
    emp_name VARCHAR(50) NOT NULL COMMENT '姓名',
    gender CHAR(1) DEFAULT '男' COMMENT '性别',
    dept VARCHAR(50) COMMENT '部门',
    post VARCHAR(50) COMMENT '岗位',
    salary DECIMAL(10, 2) COMMENT '薪资',
    hire_date DATE COMMENT '入职时间',
    status TINYINT DEFAULT 1 COMMENT '状态：1在职 0离职',
    INDEX idx_emp_name (emp_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 实验一已建表时保留原数据，仅在缺少姓名索引时补建。
SET @emp_index_sql = IF(
    EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'emp'
          AND column_name = 'emp_name' AND seq_in_index = 1
    ),
    'SELECT 1',
    'CREATE INDEX idx_emp_name ON emp (emp_name)'
);
PREPARE emp_index_stmt FROM @emp_index_sql;
EXECUTE emp_index_stmt;
DEALLOCATE PREPARE emp_index_stmt;
