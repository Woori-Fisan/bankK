package com.woorifisan.monitoring.domain.log.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PartitionMapper {

    void addDailyPartition(@Param("partitionName") String partitionName,
                           @Param("lessThanDate") String lessThanDate);

    void dropOldPartition(@Param("partitionName") String partitionName);

    List<String> findOldPartitionNames(@Param("cutoff") String cutoff);
}