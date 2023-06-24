package top.kelecc.winter.annotation;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/6/23 23:16
 */
@FunctionalInterface
public interface ConnectionCallback<T> {
    T doInConnection(Connection conn) throws SQLException;
}
