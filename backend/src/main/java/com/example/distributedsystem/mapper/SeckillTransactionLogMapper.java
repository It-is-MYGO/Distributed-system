package com.example.distributedsystem.mapper;

import com.example.distributedsystem.entity.SeckillTransactionLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface SeckillTransactionLogMapper {
    @Insert("""
            INSERT INTO seckill_transaction_log(request_id, username, product_id, quantity, status, redis_deducted, db_deducted)
            VALUES(#{requestId}, #{username}, #{productId}, #{quantity}, #{status}, #{redisDeducted}, #{dbDeducted})
            """)
    int insert(SeckillTransactionLog log);

    @Update("""
            UPDATE seckill_transaction_log
            SET status = #{status},
                redis_deducted = #{redisDeducted},
                db_deducted = #{dbDeducted},
                order_no = #{orderNo},
                error_message = #{errorMessage},
                updated_at = CURRENT_TIMESTAMP
            WHERE request_id = #{requestId}
            """)
    int update(SeckillTransactionLog log);

    @Select("""
            SELECT id, request_id, username, product_id, quantity, status, redis_deducted, db_deducted,
                   order_no, error_message, created_at, updated_at
            FROM seckill_transaction_log
            WHERE request_id = #{requestId}
            """)
    SeckillTransactionLog findByRequestId(String requestId);

    @Select("""
            SELECT id, request_id, username, product_id, quantity, status, redis_deducted, db_deducted,
                   order_no, error_message, created_at, updated_at
            FROM seckill_transaction_log
            ORDER BY id DESC
            LIMIT #{limit}
            """)
    List<SeckillTransactionLog> findRecent(@Param("limit") int limit);
}
