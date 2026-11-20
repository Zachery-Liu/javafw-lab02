package com.lab.emp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.emp.entity.Emp;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface EmpMapper extends BaseMapper<Emp> {
    @Select("SELECT emp_id, dept FROM emp WHERE emp_id = #{id} FOR UPDATE")
    Emp findDepartmentForUpdate(@Param("id") Integer id);
}
