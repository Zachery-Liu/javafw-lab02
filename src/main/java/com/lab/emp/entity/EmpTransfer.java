package com.lab.emp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;

@TableName("emp_transfer")
public class EmpTransfer {
    @TableId(value = "transfer_id", type = IdType.AUTO)
    private Long transferId;

    private Integer empId;
    private String empName;
    private String fromDept;
    private String toDept;
    private String operatorName;
    private Instant transferredAt;

    public Long getTransferId() {
        return transferId;
    }

    public void setTransferId(Long transferId) {
        this.transferId = transferId;
    }

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

    public String getFromDept() {
        return fromDept;
    }

    public void setFromDept(String fromDept) {
        this.fromDept = fromDept;
    }

    public String getToDept() {
        return toDept;
    }

    public void setToDept(String toDept) {
        this.toDept = toDept;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public Instant getTransferredAt() {
        return transferredAt;
    }

    public void setTransferredAt(Instant transferredAt) {
        this.transferredAt = transferredAt;
    }
}
