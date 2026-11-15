package com.lab.emp.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.emp.common.Result;
import com.lab.emp.entity.Dept;
import com.lab.emp.entity.Emp;
import com.lab.emp.service.DeptService;
import com.lab.emp.service.EmpService;
import java.util.List;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/depts")
public class DeptController {
    private final DeptService departments;
    private final EmpService employees;

    public DeptController(DeptService departments, EmpService employees) {
        this.departments = departments;
        this.employees = employees;
    }

    @GetMapping
    public Result<List<Dept>> list(@RequestParam(required = false) String name) {
        return Result.ok(departments.list(name));
    }

    @GetMapping("/{id}")
    public Result<Dept> get(@PathVariable Integer id) {
        return Result.ok(departments.get(id));
    }

    @GetMapping("/{id}/emps")
    public Result<Page<Emp>> employees(@PathVariable Integer id,
                                      @RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "5") int size) {
        Dept dept = departments.get(id);
        return Result.ok(employees.pageByDepartment(page, size, dept.getDeptName()));
    }

    @PostMapping
    public Result<Integer> add(@Valid @RequestBody Dept dept) {
        return Result.ok(departments.add(dept));
    }

    @PutMapping
    public Result<Void> update(@Valid @RequestBody Dept dept) {
        departments.update(dept);
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        departments.delete(id);
        return Result.ok(null);
    }
}
