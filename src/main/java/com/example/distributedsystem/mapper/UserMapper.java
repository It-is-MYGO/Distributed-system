package com.example.distributedsystem.mapper;

import com.example.distributedsystem.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class UserMapper {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RowMapper<User> userRowMapper = (rs, rowNum) -> {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setUsername(rs.getString("username"));
        u.setPassword(rs.getString("password"));
        return u;
    };

    public User findByUsername(String username) {
        String sql = "SELECT id, username, password FROM user WHERE username = ?";
        try {
            return jdbcTemplate.queryForObject(sql, userRowMapper, username);
        } catch (Exception e) {
            return null;
        }
    }

    public int insert(User user) {
        String sql = "INSERT INTO user(username, password) VALUES(?, ?)";
        return jdbcTemplate.update(sql, user.getUsername(), user.getPassword());
    }
}
