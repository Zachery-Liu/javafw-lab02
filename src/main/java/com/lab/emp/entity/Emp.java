package com.lab.emp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@TableName("emp")
public class Emp {
    @TableId(value = "emp_id", type = IdType.AUTO)
    private Integer empId;

    @NotBlank(message = "员工姓名不能为空")
    @Size(max = 50, message = "员工姓名不能超过 50 个字符")
    private String empName;

    @Size(max = 1, message = "性别不能超过 1 个字符")
    private String gender;

    @Size(max = 50, message = "部门不能超过 50 个字符")
    private String dept;

    @Size(max = 50, message = "岗位不能超过 50 个字符")
    private String post;

    @NotNull(message = "薪资不能为空")
    @DecimalMin(value = "0", message = "薪资不能为负数")
    @Digits(integer = 8, fraction = 2, message = "薪资最多 8 位整数和 2 位小数")
    private BigDecimal salary;

    private LocalDate hireDate;

    @Min(value = 0, message = "状态只能为 0 或 1")
    @Max(value = 1, message = "状态只能为 0 或 1")
    private Integer status;

    public Integer getEmpId() {
        return empId;
    }

    public void setEmpId(Integer empId) {
        this.empId = empId;
    }

    public String getEmpName() {
        return empName;
    }

    public void setEmpName(String empName) {
        this.empName = empName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getDept() {
        return dept;
    }

    public void setDept(String dept) {
        this.dept = dept;
    }

    public String getPost() {
        return post;
    }

    public void setPost(String post) {
        this.post = post;
    }

    public BigDecimal getSalary() {
        return salary;
    }

    public void setSalary(BigDecimal salary) {
        this.salary = salary;
    }

    public LocalDate getHireDate() {
        return hireDate;
    }

    public void setHireDate(LocalDate hireDate) {
        this.hireDate = hireDate;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
