package com.lab.emp.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.emp.common.Result;
import com.lab.emp.dto.EmpQueryDTO;
import com.lab.emp.entity.Emp;
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
@RequestMapping("/emps")
public class EmpController {
    private final EmpService service;

    public EmpController(EmpService service) {
        this.service = service;
    }

    @PostMapping
    public Result<Integer> add(@Valid @RequestBody Emp emp) {
        return Result.ok(service.add(emp));
    }

    @PutMapping
    public Result<Void> update(@Valid @RequestBody Emp emp) {
        service.update(emp);
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        service.delete(id);
        return Result.ok(null);
    }

    @DeleteMapping
    public Result<Integer> deleteBatch(@RequestBody List<Integer> ids) {
        return Result.ok(service.deleteBatch(ids));
    }

    @GetMapping("/{id}")
    public Result<Emp> get(@PathVariable Integer id) {
        return Result.ok(service.get(id));
    }

    @GetMapping
    public Result<Page<Emp>> page(@RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "5") int size,
                                  EmpQueryDTO query) {
        return Result.ok(service.pageQuery(page, size, query));
    }
}
