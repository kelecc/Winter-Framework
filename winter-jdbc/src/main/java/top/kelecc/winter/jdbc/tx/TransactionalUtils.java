package top.kelecc.winter.jdbc.tx;

import jakarta.annotation.Nullable;

import java.sql.Connection;

/**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/6/25 18:28
 */
public class TransactionalUtils {
    @Nullable
    public static Connection getCurrentConnection() {
        TransactionStatus transactionStatus = DataSourceTransactionManager.TRANSACTION_STATUS.get();
        return transactionStatus == null ? null : transactionStatus.connection;
    }
}
