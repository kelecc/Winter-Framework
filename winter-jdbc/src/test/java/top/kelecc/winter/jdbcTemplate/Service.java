package top.kelecc.winter.jdbcTemplate;

import top.kelecc.winter.annotation.Autowired;
import top.kelecc.winter.annotation.Component;
import top.kelecc.winter.annotation.Transactional;
import top.kelecc.winter.exception.TransactionException;
import top.kelecc.winter.jdbc.JdbcTemplate;

/**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/6/25 21:04
 */
@Transactional
@Component
public class Service {
    @Autowired
    JdbcTemplate jdbcTemplate;

    public int transactional() {
        int update = jdbcTemplate.update("update `xiyou` set name = ? where id = ?;", "傻逼", 1);
        int delete = jdbcTemplate.update("DELETE FROM `xiyou` WHERE id = ?;", 20);
        if (update == 0 || delete == 0) {
            throw new TransactionException("事务发生异常！");
        }
        return update + delete;
    }

}
