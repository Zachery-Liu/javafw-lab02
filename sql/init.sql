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

-- 字符集和排序规则需与原 emp.dept 完全一致，兼容实验一已有的数据库。
SELECT character_set_name, collation_name INTO @dept_charset, @dept_collation
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'emp' AND column_name = 'dept';
SET @dept_table_sql = CONCAT(
    'CREATE TABLE IF NOT EXISTS dept (',
    'dept_id INT PRIMARY KEY AUTO_INCREMENT, ',
    'dept_name VARCHAR(50) CHARACTER SET ', @dept_charset, ' COLLATE ', @dept_collation,
    ' NOT NULL, UNIQUE KEY uk_dept_name (dept_name)) ENGINE=InnoDB'
);
PREPARE dept_table_stmt FROM @dept_table_sql;
EXECUTE dept_table_stmt;
DEALLOCATE PREPARE dept_table_stmt;

-- 先迁入历史部门名称，再添加外键。已存在的员工记录和空部门值保留。
INSERT INTO dept (dept_name)
SELECT DISTINCT e.dept FROM emp AS e
WHERE e.dept IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM dept AS d WHERE d.dept_name = e.dept);

SET @dept_fk_sql = IF(
    EXISTS (
        SELECT 1 FROM information_schema.referential_constraints
        WHERE constraint_schema = DATABASE() AND table_name = 'emp'
          AND constraint_name = 'fk_emp_dept'
    ),
    'SELECT 1',
    'ALTER TABLE emp ADD CONSTRAINT fk_emp_dept FOREIGN KEY (dept)
         REFERENCES dept (dept_name) ON UPDATE CASCADE ON DELETE RESTRICT'
);
PREPARE dept_fk_stmt FROM @dept_fk_sql;
EXECUTE dept_fk_stmt;
DEALLOCATE PREPARE dept_fk_stmt;

-- 保存调动发生时的姓名与部门名，后续改名或删除人员后仍保留历史。
CREATE TABLE IF NOT EXISTS emp_transfer (
    transfer_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    emp_id INT NOT NULL,
    emp_name VARCHAR(50) NOT NULL,
    from_dept VARCHAR(50),
    to_dept VARCHAR(50),
    operator_name VARCHAR(128) NOT NULL,
    transferred_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_emp_transfer_employee (emp_id, transfer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_account (
    username VARCHAR(128) COLLATE utf8mb4_0900_bin PRIMARY KEY,
    password_hash VARCHAR(128) CHARACTER SET ascii NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 初始化实验管理员。盐和 PBKDF2-HMAC-SHA256 哈希使用 Base64，重复初始化保留已有密码。
INSERT INTO user_account (username, password_hash)
VALUES ('admin', 'iUt/0Asn2bHJCuVBkZcg8A==:T+ekDk7UquaRnaCou66FqGTZN6tRH1z/BqUZkKD1qDM=')
ON DUPLICATE KEY UPDATE username = user_account.username;
