package top.kelecc.winter.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/6/24 15:19
 */
@FunctionalInterface
public interface PreparedStatementCreator {
    PreparedStatement createPrepareStatement(Connection conn) throws SQLException;
}
