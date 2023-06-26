package top.kelecc.winter.jdbc.tx;
import java.sql.Connection;
/**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/6/25 15:08
 */
public class TransactionStatus {

    final Connection connection;

    public TransactionStatus(Connection connection) {
        this.connection = connection;
    }
}
