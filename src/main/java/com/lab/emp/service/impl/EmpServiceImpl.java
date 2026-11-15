package com.lab.emp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.emp.common.BusinessException;
import com.lab.emp.dto.EmpQueryDTO;
import com.lab.emp.entity.Emp;
import com.lab.emp.mapper.EmpMapper;
import com.lab.emp.service.DeptService;
import com.lab.emp.service.EmpService;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class EmpServiceImpl implements EmpService {
    private final EmpMapper mapper;
    private final DeptService departments;

    public EmpServiceImpl(EmpMapper mapper, DeptService departments) {
        this.mapper = mapper;
        this.departments = departments;
    }

    @Override
    @Transactional
    public Integer add(Emp emp) {
        if (emp.getEmpId() != null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "新增员工时由数据库生成员工编号");
        }
        emp.setDept(departments.resolveAssignment(emp.getDept()));
        mapper.insert(emp);
        return emp.getEmpId();
    }

    @Override
    @Transactional
    public void update(Emp emp) {
        Integer id = requireId(emp.getEmpId());
        emp.setDept(departments.resolveAssignment(emp.getDept()));
        if (mapper.updateById(emp) == 0 && mapper.selectById(id) == null) {
            throw missingEmployee(id);
        }
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        if (mapper.deleteById(requireId(id)) == 0) {
            throw missingEmployee(id);
        }
    }

    @Override
    @Transactional
    public int deleteBatch(List<Integer> ids) {
        if (ids == null || ids.isEmpty() || ids.size() > 1000) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "批量删除需要 1 到 1000 个员工编号");
        }
        Set<Integer> distinctIds = new LinkedHashSet<>();
        for (Integer id : ids) {
            distinctIds.add(requireId(id));
        }
        int affected = mapper.deleteBatchIds(distinctIds);
        if (affected != distinctIds.size()) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "批量删除中有员工不存在");
        }
        return affected;
    }

    @Override
    public Emp get(Integer id) {
        Emp emp = mapper.selectById(requireId(id));
        if (emp == null) {
            throw missingEmployee(id);
        }
        return emp;
    }

    @Override
    public Page<Emp> pageQuery(int page, int size, EmpQueryDTO query) {
        String name = StringUtils.trimWhitespace(query.getName());
        String gender = StringUtils.trimWhitespace(query.getGender());
        String dept = query.getDept();
        LambdaQueryWrapper<Emp> filter = new LambdaQueryWrapper<Emp>()
                .like(StringUtils.hasText(name), Emp::getEmpName, name)
                .eq(StringUtils.hasText(gender), Emp::getGender, gender)
                .eq(StringUtils.hasText(dept), Emp::getDept, dept);
        return selectPage(page, size, filter);
    }

    @Override
    public Page<Emp> pageByDepartment(int page, int size, String department) {
        return selectPage(page, size, new LambdaQueryWrapper<Emp>().eq(Emp::getDept, department));
    }

    private Page<Emp> selectPage(int page, int size, LambdaQueryWrapper<Emp> filter) {
        if (page < 1 || size < 1 || size > 100) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "page 至少为 1，size 必须在 1 到 100 之间");
        }
        return mapper.selectPage(new Page<>(page, size), filter.orderByDesc(Emp::getEmpId));
    }

    private static Integer requireId(Integer id) {
        if (id == null || id <= 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "员工编号必须为正整数");
        }
        return id;
    }

    private static BusinessException missingEmployee(Integer id) {
        return new BusinessException(HttpStatus.NOT_FOUND, "员工不存在，编号 " + id);
    }
}
