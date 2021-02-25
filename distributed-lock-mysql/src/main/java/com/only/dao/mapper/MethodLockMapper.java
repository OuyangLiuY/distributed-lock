package com.only.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.only.dao.entity.MethodLock;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

@Repository
public interface MethodLockMapper extends BaseMapper<MethodLock> {

    @Select("select * from method_lock where method_name = #{methodName}  for update")
    MethodLock selectByMethodName(String methodName);
}
