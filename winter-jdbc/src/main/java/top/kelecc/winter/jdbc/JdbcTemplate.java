package top.kelecc.winter.jdbc;

import jakarta.annotation.Nullable;
import top.kelecc.winter.annotation.ConnectionCallback;
import top.kelecc.winter.exception.DataAccessException;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/6/23 23:12
 */
public class JdbcTemplate {
    final DataSource dataSource;

    public JdbcTemplate(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Number queryForNumber(String sql, Object... args) {
        return queryForObject(sql, NumberRowMapper.instance, args);
    }

    public <T> List<T> queryForList(String sql, Class<T> clazz, Object... args) {
        return queryForList(sql, new BeanRowMapper<>(clazz), args);
    }

    public <T> List<T> queryForList(String sql, RowMapper<T> rowMapper, Object... args) {
        return execute(getPreparedStatementCreator(sql, args), ps -> {
            ArrayList<T> list = new ArrayList<>();
            try (ResultSet resultSet = ps.executeQuery()) {
                while (resultSet.next()) {
                    list.add(rowMapper.mapRow(resultSet, resultSet.getRow()));
                }
            }
            return list;
        });
    }

    public Number updateAndReturnGeneratedKey(String sql, Object... args) throws DataAccessException {
        return execute(conn -> {
            PreparedStatement preparedStatement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            bindArgs(preparedStatement,args);
            return preparedStatement;
        },ps -> {
            int n = ps.executeUpdate();
            if (n == 0) {
                throw new DataAccessException("插入了0行！");
            }
            if (n > 1) {
                throw new DataAccessException("插入了多行！");
            }
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                while (generatedKeys.next()) {
                    return (Number) generatedKeys.getObject(1);
                }
            }
            throw new DataAccessException("不应该到达此处！");
        });
    }

    @SuppressWarnings("unchecked")
    public <T> T queryForObject(String sql, Class<T> clazz, Object... args) throws DataAccessException {
        if (clazz == String.class) {
            return (T) queryForObject(sql, StringRowMapper.instance, args);
        }
        if (clazz == Boolean.class || clazz == boolean.class) {
            return (T) queryForObject(sql, BooleanRowMapper.instance, args);
        }
        if (Number.class.isAssignableFrom(clazz) || clazz.isPrimitive()) {
            return (T) queryForObject(sql, NumberRowMapper.instance, args);
        }
        return queryForObject(sql, new BeanRowMapper<>(clazz), args);
    }

    public <T> T queryForObject(String sql, RowMapper<T> rowMapper, Object... args) throws DataAccessException {
        return execute(getPreparedStatementCreator(sql, args), ps -> {
            T t = null;
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (t == null) {
                        t = rowMapper.mapRow(rs, rs.getRow());
                    } else {
                        throw new DataAccessException("发现多行记录！");
                    }
                }
            }
            if (t == null) {
                throw new DataAccessException("结果集为空！");
            }
            return t;
        });
    }

    public int update(String sql, Object... args) {
        return execute(getPreparedStatementCreator(sql, args), PreparedStatement::executeUpdate);
    }

    public <T> T execute(ConnectionCallback<T> action) throws DataAccessException {
        try (Connection connection = dataSource.getConnection()) {
            return action.doInConnection(connection);
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    public <T> T execute(PreparedStatementCreator psc, PreparedStatementCallback<T> callback) {
        return execute(conn -> {
            try (PreparedStatement prepareStatement = psc.createPrepareStatement(conn)) {
                return callback.doInPreparedStatement(prepareStatement);
            }
        });
    }

    private PreparedStatementCreator getPreparedStatementCreator(String sql, Object... args) {
        return (Connection conn) -> {
            PreparedStatement preparedStatement = conn.prepareStatement(sql);
            bindArgs(preparedStatement, args);
            return preparedStatement;
        };
    }

    private void bindArgs(PreparedStatement preparedStatement, Object[] args) throws SQLException {
        for (int i = 0; i < args.length; i++) {
            preparedStatement.setObject(i + 1, args[i]);
        }
    }
}

class BooleanRowMapper implements RowMapper<Boolean> {
    static BooleanRowMapper instance = new BooleanRowMapper();

    @Nullable
    @Override
    public Boolean mapRow(ResultSet rs, int rowNum) throws SQLException {
        return rs.getBoolean(1);
    }
}

class StringRowMapper implements RowMapper<String> {
    static StringRowMapper instance = new StringRowMapper();

    @Nullable
    @Override
    public String mapRow(ResultSet rs, int rowNum) throws SQLException {
        return rs.getString(1);
    }
}

class NumberRowMapper implements RowMapper<Number> {
    static NumberRowMapper instance = new NumberRowMapper();

    @Nullable
    @Override
    public Number mapRow(ResultSet rs, int rowNum) throws SQLException {
        return (Number) rs.getObject(1);
    }
}
