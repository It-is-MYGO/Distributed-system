package com.example.distributedsystem.mapper;

import com.example.distributedsystem.entity.GovernanceConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface GovernanceConfigMapper {
    @Select("SELECT config_key, config_value, description, updated_at FROM governance_config ORDER BY config_key")
    List<GovernanceConfig> findAll();

    @Select("SELECT config_key, config_value, description, updated_at FROM governance_config WHERE config_key = #{key}")
    GovernanceConfig findByKey(String key);

    @Update("""
            UPDATE governance_config
            SET config_value = #{value}, updated_at = CURRENT_TIMESTAMP
            WHERE config_key = #{key}
            """)
    int updateValue(@Param("key") String key, @Param("value") String value);
}
