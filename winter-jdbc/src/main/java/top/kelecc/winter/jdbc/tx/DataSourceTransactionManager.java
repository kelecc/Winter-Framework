package top.kelecc.winter.jdbc.tx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.kelecc.winter.exception.TransactionException;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/6/25 15:08
 */
public class DataSourceTransactionManager implements
        PlatformTransactionManager, InvocationHandler {
    static final ThreadLocal<TransactionStatus> TRANSACTION_STATUS = new ThreadLocal<>();
    final DataSource dataSource;
    final Logger logger = LoggerFactory.getLogger(getClass());

    public DataSourceTransactionManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        TransactionStatus transactionStatus = TRANSACTION_STATUS.get();
        if (transactionStatus == null) {
            //当前无事务，开启新事务。
            try (Connection connection = dataSource.getConnection()) {
                final boolean autoCommit = connection.getAutoCommit();
                if (autoCommit) {
                    connection.setAutoCommit(false);
                }
                try {
                    //设置ThreadLocal状态
                    TRANSACTION_STATUS.set(new TransactionStatus(connection));
                    //调用业务方法
                    Object result = method.invoke(proxy, args);
                    //提交事务
                    connection.commit();
                    return result;
                } catch (Exception e) {
                    //回滚事务
                    logger.warn("发生了异常： {}，事务即将回滚！", e.getCause() == null ? "null" : e.getCause().getClass().getName());
                    TransactionException transactionException = new TransactionException(e.getCause());
                    try {
                        connection.rollback();
                    } catch (SQLException ex) {
                        transactionException.addSuppressed(ex);
                    }
                    throw transactionException;
                } finally {
                    TRANSACTION_STATUS.remove();
                    if (autoCommit) {
                        connection.setAutoCommit(true);
                    }
                }
            }
        } else {
            // 当前已有事务,加入当前事务执行:
            return method.invoke(proxy, args);
        }
    }
}
