package com.lab.emp.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.emp.dto.EmpQueryDTO;
import com.lab.emp.entity.Emp;
import java.util.List;

public interface EmpService {
    Integer add(Emp emp);

    void update(Emp emp);

    void delete(Integer id);

    int deleteBatch(List<Integer> ids);

    Emp get(Integer id);

    Page<Emp> pageQuery(int page, int size, EmpQueryDTO query);
}
