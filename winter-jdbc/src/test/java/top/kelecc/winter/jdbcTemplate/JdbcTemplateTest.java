package top.kelecc.winter.jdbcTemplate;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import top.kelecc.winter.App;
import top.kelecc.winter.context.AnnotationConfigApplicationContext;
import top.kelecc.winter.context.ApplicationContext;
import top.kelecc.winter.context.ApplicationContextUtils;
import top.kelecc.winter.io.PropertyResolver;
import top.kelecc.winter.jdbc.JdbcTemplate;
import top.kelecc.winter.jdbcTemplate.bean.Xiyou;
import top.kelecc.winter.util.YamlUtils;

import java.math.BigInteger;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertSame;

class JdbcTemplateTest {

    @BeforeAll
    static void init() {
        Properties props = new Properties();
        props.putAll(YamlUtils.loadYamlAsPlainMap("application.yaml"));
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(App.class, new PropertyResolver(props));
    }

    @AfterAll
    static void destroy() {
        ApplicationContext ctx = ApplicationContextUtils.getRequiredApplicationContext();
        ctx.close();
    }

    @Test
    void updateUpdateTest() {
        ApplicationContext ctx = ApplicationContextUtils.getRequiredApplicationContext();
        JdbcTemplate jdbcTemplate = ctx.getBean("jdbcTemplate");
        int update = jdbcTemplate.update("update `xiyou` set name = ? where id = ?;", "可乐", 1);
        assertSame(1, update);
    }

    @Test
    void updateDeleteTest() {
        ApplicationContext ctx = ApplicationContextUtils.getRequiredApplicationContext();
        JdbcTemplate jdbcTemplate = ctx.getBean("jdbcTemplate");
        int update = jdbcTemplate.update("DELETE FROM `xiyou` WHERE id = ?;", 2);
        assertSame(1, update);
    }

    @Test
    void updateInsertTest() {
        ApplicationContext ctx = ApplicationContextUtils.getRequiredApplicationContext();
        JdbcTemplate jdbcTemplate = ctx.getBean("jdbcTemplate");
        int update = jdbcTemplate.update("INSERT INTO `xiyou` (id,name,age) VALUES (2, '雪碧', 23), (3, '芬达', 23);");
        assertSame(2, update);
    }

    @Test
    void queryForObject() {
        ApplicationContext ctx = ApplicationContextUtils.getRequiredApplicationContext();
        JdbcTemplate jdbcTemplate = ctx.getBean("jdbcTemplate");
        Xiyou xiyou = jdbcTemplate.queryForObject("SELECT * FROM xiyou WHERE id = ?;", Xiyou.class, 2);
        Assertions.assertEquals("kele", xiyou.getName());
        Assertions.assertEquals(22, xiyou.getAge());
        Assertions.assertEquals(2, xiyou.getId());
    }

    @Test
    void queryForList() {
        ApplicationContext ctx = ApplicationContextUtils.getRequiredApplicationContext();
        JdbcTemplate jdbcTemplate = ctx.getBean("jdbcTemplate");
        List<Xiyou> list = jdbcTemplate.queryForList("SELECT * FROM xiyou WHERE id = ?;", Xiyou.class, 1);
        Assertions.assertEquals(1, list.size());
        List<Xiyou> listAll = jdbcTemplate.queryForList("SELECT * FROM xiyou;", Xiyou.class);
        Assertions.assertEquals(15, listAll.size());
    }

    @Test
    void updateAndReturnGeneratedKey() {
        ApplicationContext ctx = ApplicationContextUtils.getRequiredApplicationContext();
        JdbcTemplate jdbcTemplate = ctx.getBean("jdbcTemplate");
        BigInteger number = (BigInteger) jdbcTemplate.updateAndReturnGeneratedKey("INSERT INTO `xiyou` (name,age) VALUES (?, ?);", "青梅绿茶", 23);
        Assertions.assertEquals(15, number.intValue());
    }
}
