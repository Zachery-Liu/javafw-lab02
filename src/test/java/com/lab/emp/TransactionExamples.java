package com.lab.emp;

import com.lab.emp.entity.Emp;
import com.lab.emp.mapper.EmpMapper;
import org.springframework.transaction.annotation.Transactional;

public class TransactionExamples {
    private final EmpMapper mapper;

    public TransactionExamples(EmpMapper mapper) {
        this.mapper = mapper;
    }

    public void insertWithoutTransaction(Emp employee) {
        insertAndFail(employee);
    }

    @Transactional
    public void insertWithTransaction(Emp employee) {
        insertAndFail(employee);
    }

    @Transactional(rollbackFor = Exception.class)
    public void insertWithCheckedException(Emp employee) throws Exception {
        mapper.insert(employee);
        throw new Exception("rollback demonstration");
    }

    @Transactional
    public void swallowException(Emp employee) {
        try {
            insertAndFail(employee);
        } catch (IllegalStateException expected) {
            // The proxy sees a normal return, so this demonstration commits.
        }
    }

    public void invokeOnThis(Emp employee) {
        this.insertWithTransaction(employee);
    }

    public void invokePrivate(Emp employee) {
        insertWithPrivateTransaction(employee);
    }

    @Transactional
    private void insertWithPrivateTransaction(Emp employee) {
        insertAndFail(employee);
    }

    private void insertAndFail(Emp employee) {
        mapper.insert(employee);
        throw new IllegalStateException("rollback demonstration");
    }
}
