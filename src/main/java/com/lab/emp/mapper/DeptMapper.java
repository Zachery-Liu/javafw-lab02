package com.lab.emp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.emp.entity.Dept;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface DeptMapper extends BaseMapper<Dept> {
    String WITH_EMPLOYEE_COUNT = "SELECT d.dept_id, d.dept_name, COUNT(e.emp_id) AS employee_count "
            + "FROM dept d LEFT JOIN emp e ON e.dept = d.dept_name ";
    String GROUP_DEPARTMENTS = " GROUP BY d.dept_id, d.dept_name ";

    @Select({"<script>", WITH_EMPLOYEE_COUNT,
            "<if test='name != null'>WHERE d.dept_name LIKE CONCAT('%', #{name}, '%')</if>",
            GROUP_DEPARTMENTS, "ORDER BY d.dept_id DESC", "</script>"})
    List<Dept> findSummaries(@Param("name") String name);

    @Select(WITH_EMPLOYEE_COUNT + "WHERE d.dept_id = #{id}" + GROUP_DEPARTMENTS)
    Dept findSummary(@Param("id") Integer id);
}
