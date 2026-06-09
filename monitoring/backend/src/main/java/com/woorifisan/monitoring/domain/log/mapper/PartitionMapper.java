package com.woorifisan.monitoring.domain.log.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PartitionMapper {

    boolean partitionExists(@Param("partitionName") String partitionName);

    void addDailyPartition(@Param("partitionName") String partitionName,
                           @Param("lessThanDate") String lessThanDate);

    void dropPartition(@Param("partitionName") String partitionName);
}