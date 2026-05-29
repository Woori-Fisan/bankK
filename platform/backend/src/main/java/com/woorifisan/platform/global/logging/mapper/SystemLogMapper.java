package com.woorifisan.platform.global.logging.mapper;

import com.woorifisan.platform.global.logging.model.SystemLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * system_logs 테이블 데이터 접근 인터페이스.
 */
@Mapper
public interface SystemLogMapper {

    /**
     * 시스템 로그 1건을 삽입한다.
     */
    void insert(SystemLog systemLog);
}
