package com.lab.emp;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.emp.common.BusinessException;
import com.lab.emp.config.SpringConfig;
import com.lab.emp.dto.EmpQueryDTO;
import com.lab.emp.entity.Dept;
import com.lab.emp.entity.Emp;
import com.lab.emp.entity.EmpTransfer;
import com.lab.emp.mapper.EmpMapper;
import com.lab.emp.service.EmpService;
import com.lab.emp.service.DeptService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

public class EmployeeIntegrationTest {
    private static AnnotationConfigApplicationContext context;
    private static JdbcTemplate jdbc;
    private static EmpService employees;
    private static DeptService departments;
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
        jdbc.execute("CREATE TABLE dept (dept_id INT AUTO_INCREMENT PRIMARY KEY, "
                + "dept_name VARCHAR(50) NOT NULL UNIQUE)");
        jdbc.execute("CREATE TABLE emp (emp_id INT AUTO_INCREMENT PRIMARY KEY, "
                + "emp_name VARCHAR(50) NOT NULL, gender CHAR(1) DEFAULT '男', dept VARCHAR(50), "
                + "post VARCHAR(50), salary DECIMAL(10,2), hire_date DATE, status TINYINT DEFAULT 1, "
                + "CONSTRAINT fk_emp_dept FOREIGN KEY (dept) REFERENCES dept (dept_name) "
                + "ON UPDATE CASCADE ON DELETE RESTRICT)");
        jdbc.execute("CREATE TABLE emp_transfer (transfer_id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                + "emp_id INT NOT NULL, emp_name VARCHAR(50) NOT NULL, from_dept VARCHAR(50), "
                + "to_dept VARCHAR(50), operator_name VARCHAR(128) NOT NULL, "
                + "transferred_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6))");
        employees = context.getBean(EmpService.class);
        departments = context.getBean(DeptService.class);
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
        jdbc.update("DELETE FROM emp_transfer");
        jdbc.update("DELETE FROM emp");
        jdbc.update("DELETE FROM dept");
        jdbc.update("INSERT INTO dept (dept_name) VALUES (?), (?)", "研发部", "市场部");
    }

    @Test
    public void employeeCrudAndFilteredPagination() {
        Emp first = employee("员工甲", "研发部", "女");
        Integer firstId = employees.add(first);
        assertNotNull(firstId);
        assertEquals(LocalDate.of(2024, 8, 15), employees.get(firstId).getHireDate());

        first.setSalary(new BigDecimal("9100.50"));
        employees.update(first, "admin");
        assertEquals(first.getSalary(), employees.get(firstId).getSalary());
        assertEquals(0, employees.transfers(firstId, 1, 5).getTotal());

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
    public void departmentCrudAndRelatedEmployees() {
        Dept dept = new Dept();
        dept.setDeptName("支持部");
        Integer id = departments.add(dept);
        assertNotNull(id);
        assertEquals(Long.valueOf(0), departments.get(id).getEmployeeCount());
        assertEquals(id, departments.list("支持").get(0).getDeptId());

        Integer employeeId = employees.add(employee("李一", "支持部", "女"));
        employees.add(employee("李二", "支持部", "男"));
        assertEquals(Long.valueOf(2), departments.get(id).getEmployeeCount());
        assertEquals(2, employees.pageByDepartment(1, 1, "支持部").getTotal());
        assertEquals(1, employees.pageByDepartment(1, 1, "支持部").getRecords().size());
        assertEquals(HttpStatus.CONFLICT, assertThrows(BusinessException.class,
                () -> departments.delete(id)).getStatus());

        dept.setDeptName("客户支持部");
        departments.update(dept);
        assertEquals("客户支持部", employees.get(employeeId).getDept());
        assertEquals(Long.valueOf(2), departments.get(id).getEmployeeCount());
        assertEquals(0, employees.pageByDepartment(1, 5, "支持部").getTotal());

        Emp updated = employees.get(employeeId);
        updated.setDept(null);
        employees.update(updated, "admin");
        assertNull(employees.get(employeeId).getDept());
        assertEquals(Long.valueOf(1), departments.get(id).getEmployeeCount());
    }

    @Test
    public void departmentNameMustBeUniqueAndUnusedDepartmentCanBeDeleted() {
        Dept dept = new Dept();
        dept.setDeptName(" 研发部 ");
        assertEquals(HttpStatus.CONFLICT, assertThrows(BusinessException.class,
                () -> departments.add(dept)).getStatus());

        dept.setDeptName("财务部");
        Integer id = departments.add(dept);
        dept.setDeptName("市场部");
        assertEquals(HttpStatus.CONFLICT, assertThrows(BusinessException.class,
                () -> departments.update(dept)).getStatus());
        assertEquals("财务部", departments.get(id).getDeptName());

        departments.delete(id);
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(BusinessException.class,
                () -> departments.get(id)).getStatus());
    }

    @Test
    public void unknownDepartmentCannotBeAssignedToEmployee() {
        Emp unknown = employee("新员工", "不存在的部门", "女");
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(BusinessException.class,
                () -> employees.add(unknown)).getStatus());
        assertEquals(0, count());
        Integer id = employees.add(employee("暂无归属", null, "女"));
        assertNull(employees.get(id).getDept());
    }

    @Test
    public void changesInDepartmentProducePaginatedHistory() {
        Emp emp = employee("调动员工", null, "女");
        Integer id = employees.add(emp);
        assertEquals(0, employees.transfers(id, 1, 5).getTotal());
        emp.setDept("研发部");
        employees.update(emp, "admin");
        emp.setDept("市场部");
        employees.update(emp, "复核员");
        employees.update(emp, "复核员");

        Page<EmpTransfer> newest = employees.transfers(id, 1, 1);
        assertEquals(2, newest.getTotal());
        EmpTransfer recent = newest.getRecords().get(0);
        assertEquals(id, recent.getEmpId());
        assertEquals("调动员工", recent.getEmpName());
        assertEquals("研发部", recent.getFromDept());
        assertEquals("市场部", recent.getToDept());
        assertEquals("复核员", recent.getOperatorName());
        assertNotNull(recent.getTransferredAt());

        EmpTransfer older = employees.transfers(id, 2, 1).getRecords().get(0);
        assertNull(older.getFromDept());
        assertEquals("研发部", older.getToDept());
        assertEquals("admin", older.getOperatorName());
    }

    @Test
    public void historyPreservesSnapshotsAfterRenameAndEmployeeDeletion() {
        Emp emp = employee("调动员工", "研发部", "女");
        Integer id = employees.add(emp);
        emp.setDept("市场部");
        employees.update(emp, "admin");

        Dept target = departments.list("市场部").get(0);
        target.setDeptName("营销部");
        departments.update(target);
        assertEquals("营销部", employees.get(id).getDept());
        assertEquals(1, employees.transfers(id, 1, 5).getTotal());

        employees.delete(id);
        EmpTransfer history = employees.transfers(id, 1, 5).getRecords().get(0);
        assertEquals("研发部", history.getFromDept());
        assertEquals("市场部", history.getToDept());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(BusinessException.class,
                () -> employees.transfers(Integer.MAX_VALUE, 1, 5)).getStatus());
    }

    @Test
    public void transferInsertFailureRollsBackDepartmentUpdate() {
        Emp emp = employee("回滚员工", "研发部", "女");
        Integer id = employees.add(emp);
        emp.setDept("市场部");
        String invalidOperator = String.join("", java.util.Collections.nCopies(129, "a"));

        assertThrows(DataIntegrityViolationException.class, () -> employees.update(emp, invalidOperator));
        assertEquals("研发部", employees.get(id).getDept());
        assertEquals(0, employees.transfers(id, 1, 5).getTotal());
    }

    @Test
    public void concurrentTransfersRecordTheActualPreviousDepartment() throws Exception {
        Dept third = new Dept();
        third.setDeptName("支持部");
        departments.add(third);
        Integer id = employees.add(employee("并发调动员工", "研发部", "女"));
        Emp first = employee("并发调动员工", "市场部", "女");
        first.setEmpId(id);
        Emp second = employee("并发调动员工", "支持部", "女");
        second.setEmpId(id);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService threads = Executors.newFixedThreadPool(2);
        try {
            Future<?> one = threads.submit(() -> {
                start.await();
                employees.update(first, "admin");
                return null;
            });
            Future<?> two = threads.submit(() -> {
                start.await();
                employees.update(second, "复核员");
                return null;
            });
            start.countDown();
            one.get(10, TimeUnit.SECONDS);
            two.get(10, TimeUnit.SECONDS);
        } finally {
            threads.shutdownNow();
        }

        List<EmpTransfer> history = employees.transfers(id, 1, 5).getRecords();
        assertEquals(2, history.size());
        assertEquals("研发部", history.get(1).getFromDept());
        assertEquals(history.get(1).getToDept(), history.get(0).getFromDept());
        assertEquals(employees.get(id).getDept(), history.get(0).getToDept());
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
