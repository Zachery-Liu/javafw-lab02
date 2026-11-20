package com.lab.emp.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.emp.dto.EmpQueryDTO;
import com.lab.emp.entity.Emp;
import com.lab.emp.entity.EmpTransfer;
import java.util.List;

public interface EmpService {
    Integer add(Emp emp);

    void update(Emp emp, String operator);

    void delete(Integer id);

    int deleteBatch(List<Integer> ids);

    Emp get(Integer id);

    Page<Emp> pageQuery(int page, int size, EmpQueryDTO query);

    Page<Emp> pageByDepartment(int page, int size, String department);

    Page<EmpTransfer> transfers(Integer id, int page, int size);
}
