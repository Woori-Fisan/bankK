package com.woorifisan.monitoring.domain.user.mapper;

import com.woorifisan.monitoring.domain.user.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {
    User findByLoginId(@Param("loginId") String loginId);
    void save(User user);
}
