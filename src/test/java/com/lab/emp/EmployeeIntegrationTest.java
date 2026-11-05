package com.lab.emp;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.emp.common.BusinessException;
import com.lab.emp.config.SpringConfig;
import com.lab.emp.dto.EmpQueryDTO;
import com.lab.emp.entity.Emp;
import com.lab.emp.mapper.EmpMapper;
import com.lab.emp.service.EmpService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

public class EmployeeIntegrationTest {
    private static AnnotationConfigApplicationContext context;
    private static JdbcTemplate jdbc;
    private static EmpService employees;
    private static TransactionExamples transactions;

    @BeforeClass
    public static void startSpring() {
        context = new AnnotationConfigApplicationContext();
        Map<String, Object> properties = new HashMap<>();
        properties.put("jdbc.driver", "org.h2.Driver");
        properties.put("jdbc.url", "jdbc:h2:mem:emp_tests;MODE=MySQL;DB_CLOSE_DELAY=-1");
        properties.put("jdbc.username", "sa");
        properties.put("jdbc.password", "");
        context.getEnvironment().getPropertySources()
                .addFirst(new MapPropertySource("testDatabase", properties));
        context.register(SpringConfig.class, TestConfig.class);
        context.refresh();

        jdbc = new JdbcTemplate(context.getBean(DataSource.class));
        jdbc.execute("CREATE TABLE emp (emp_id INT AUTO_INCREMENT PRIMARY KEY, "
                + "emp_name VARCHAR(50) NOT NULL, gender CHAR(1) DEFAULT '男', dept VARCHAR(50), "
                + "post VARCHAR(50), salary DECIMAL(10,2), hire_date DATE, status TINYINT DEFAULT 1)");
        employees = context.getBean(EmpService.class);
        transactions = context.getBean(TransactionExamples.class);
    }

    @AfterClass
    public static void stopSpring() {
        if (context != null) {
            context.close();
        }
    }

    @Before
    public void clearRows() {
        jdbc.update("DELETE FROM emp");
    }

    @Test
    public void employeeCrudAndFilteredPagination() {
        Emp first = employee("员工甲", "研发部", "女");
        Integer firstId = employees.add(first);
        assertNotNull(firstId);
        assertEquals(LocalDate.of(2024, 8, 15), employees.get(firstId).getHireDate());

        first.setSalary(new BigDecimal("9100.50"));
        employees.update(first);
        assertEquals(first.getSalary(), employees.get(firstId).getSalary());

        Integer secondId = employees.add(employee("员工乙", "研发部", "女"));
        employees.add(employee("员工丙", "市场部", "男"));
        EmpQueryDTO query = new EmpQueryDTO();
        query.setName("员工");
        query.setGender("女");
        query.setDept("研发部");
        Page<Emp> pagination = employees.pageQuery(1, 1, query);
        assertEquals(2, pagination.getTotal());
        assertEquals(secondId, pagination.getRecords().get(0).getEmpId());
        assertEquals(firstId, employees.pageQuery(2, 1, query).getRecords().get(0).getEmpId());
        assertEquals(0, employees.pageQuery(3, 1, query).getRecords().size());

        employees.delete(firstId);
        assertEquals(1, employees.deleteBatch(Arrays.asList(secondId, secondId)));
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(BusinessException.class,
                () -> employees.get(firstId)).getStatus());
    }

    @Test
    public void batchRollsBackWhenOneIdDoesNotExist() {
        Integer id = employees.add(employee("保留员工", "研发部", "女"));
        BusinessException exception = assertThrows(BusinessException.class,
                () -> employees.deleteBatch(Arrays.asList(id, Integer.MAX_VALUE)));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals(id, employees.get(id).getEmpId());
    }

    @Test
    public void unannotatedInsertionCommitsBeforeException() {
        assertThrows(IllegalStateException.class,
                () -> transactions.insertWithoutTransaction(employee("无事务", "研发部", "女")));
        assertEquals(1, count());
    }

    @Test
    public void transactionRollsBackOnRuntimeException() {
        assertThrows(IllegalStateException.class,
                () -> transactions.insertWithTransaction(employee("有事务", "研发部", "女")));
        assertEquals(0, count());
    }

    @Test
    public void rollbackForAlsoHandlesCheckedException() {
        assertThrows(Exception.class,
                () -> transactions.insertWithCheckedException(employee("受检异常", "研发部", "女")));
        assertEquals(0, count());
    }

    @Test
    public void swallowedExceptionCommits() {
        transactions.swallowException(employee("吞掉异常", "研发部", "女"));
        assertEquals(1, count());
    }

    @Test
    public void selfInvocationBypassesTransactionProxy() {
        assertThrows(IllegalStateException.class,
                () -> transactions.invokeOnThis(employee("内部调用", "研发部", "女")));
        assertEquals(1, count());
    }

    @Test
    public void privateMethodIsNotIntercepted() {
        assertThrows(IllegalStateException.class,
                () -> transactions.invokePrivate(employee("私有方法", "研发部", "女")));
        assertEquals(1, count());
    }

    private static int count() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM emp", Integer.class);
    }

    private static Emp employee(String name, String department, String gender) {
        Emp emp = new Emp();
        emp.setEmpName(name);
        emp.setDept(department);
        emp.setGender(gender);
        emp.setSalary(new BigDecimal("9000.00"));
        emp.setHireDate(LocalDate.of(2024, 8, 15));
        return emp;
    }

    @Configuration
    static class TestConfig {
        @Bean
        public TransactionExamples transactionExamples(EmpMapper mapper) {
            return new TransactionExamples(mapper);
        }
    }
}
