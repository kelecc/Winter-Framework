package top.kelecc.winter.jdbc;

import jakarta.annotation.Nullable;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/6/24 15:24
 */
@FunctionalInterface
public interface PreparedStatementCallback<T> {
    @Nullable
    T doInPreparedStatement(PreparedStatement ps) throws SQLException;
}
