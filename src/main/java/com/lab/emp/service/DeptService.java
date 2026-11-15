package com.lab.emp.service;

import com.lab.emp.entity.Dept;
import java.util.List;

public interface DeptService {
    Integer add(Dept dept);

    void update(Dept dept);

    void delete(Integer id);

    Dept get(Integer id);

    List<Dept> list(String name);

    String resolveAssignment(String name);
}
