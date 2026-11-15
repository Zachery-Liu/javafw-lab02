package com.lab.emp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lab.emp.common.BusinessException;
import com.lab.emp.entity.Dept;
import com.lab.emp.mapper.DeptMapper;
import com.lab.emp.service.DeptService;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DeptServiceImpl implements DeptService {
    private final DeptMapper mapper;

    public DeptServiceImpl(DeptMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Integer add(Dept dept) {
        if (dept.getDeptId() != null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "新增部门时由数据库生成部门编号");
        }
        dept.setDeptName(normalizedName(dept));
        try {
            mapper.insert(dept);
        } catch (DuplicateKeyException exception) {
            throw duplicateName();
        }
        return dept.getDeptId();
    }

    @Override
    @Transactional
    public void update(Dept dept) {
        Integer id = requireId(dept.getDeptId());
        dept.setDeptName(normalizedName(dept));
        try {
            if (mapper.updateById(dept) == 0 && mapper.selectById(id) == null) {
                throw missingDepartment(id);
            }
        } catch (DuplicateKeyException exception) {
            throw duplicateName();
        }
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        requireId(id);
        try {
            if (mapper.deleteById(id) == 0) {
                throw missingDepartment(id);
            }
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(HttpStatus.CONFLICT, "该部门仍有员工，请先调整员工归属");
        }
    }

    @Override
    public Dept get(Integer id) {
        Dept dept = mapper.findSummary(requireId(id));
        if (dept == null) {
            throw missingDepartment(id);
        }
        return dept;
    }

    @Override
    public List<Dept> list(String name) {
        return mapper.findSummaries(StringUtils.hasText(name) ? StringUtils.trimWhitespace(name) : null);
    }

    @Override
    public String resolveAssignment(String name) {
        if (!StringUtils.hasText(name)) {
            return null;
        }
        Dept dept = mapper.selectOne(new LambdaQueryWrapper<Dept>().eq(Dept::getDeptName, name));
        if (dept == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "指定部门不存在，请选择已有部门");
        }
        return dept.getDeptName();
    }

    private static String normalizedName(Dept dept) {
        String name = StringUtils.trimWhitespace(dept.getDeptName());
        if (!StringUtils.hasText(name)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "部门名称不能为空");
        }
        return name;
    }

    private static Integer requireId(Integer id) {
        if (id == null || id <= 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "部门编号必须为正整数");
        }
        return id;
    }

    private static BusinessException missingDepartment(Integer id) {
        return new BusinessException(HttpStatus.NOT_FOUND, "部门不存在，编号 " + id);
    }

    private static BusinessException duplicateName() {
        return new BusinessException(HttpStatus.CONFLICT, "部门名称已存在");
    }
}
