package top.kelecc.winter.jdbc;

import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.kelecc.winter.exception.DataAccessException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/6/24 16:42
 */
public class BeanRowMapper<T> implements RowMapper<T> {
    final Logger logger = LoggerFactory.getLogger(getClass());
    Class<T> clazz;
    Constructor<T> constructor;
    Map<String, Field> fieldMap = new HashMap<>();
    Map<String, Method> methodMap = new HashMap<>();

    public BeanRowMapper(Class<T> clazz) {
        this.clazz = clazz;
        try {
            this.constructor = clazz.getConstructor();
        } catch (ReflectiveOperationException e) {
            throw new DataAccessException(String.format("构建BeanRowMapper时未在类 %s 中发现公共无参构造方法！", clazz.getName()), e);
        }
        for (Field field : clazz.getFields()) {
            String name = field.getName();
            this.fieldMap.put(name, field);
            logger.debug("添加一行映射：{} 到字段 {}", name, name);
        }
        for (Method method : clazz.getMethods()) {
            Parameter[] ps = method.getParameters();
            if (ps.length == 1) {
                String name = method.getName();
                if (name.length() >= 4 && name.startsWith("set")) {
                    String prop = Character.toLowerCase(name.charAt(3)) + name.substring(4);
                    this.methodMap.put(prop, method);
                    logger.debug("添加一行映射：{} 到 {}({})", prop, name, ps[0].getType().getSimpleName());
                }
            }
        }
    }

    @Nullable
    @Override
    public T mapRow(ResultSet rs, int rowNum) throws SQLException {
        T bean;
        try {
            bean = this.constructor.newInstance();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                String label = metaData.getColumnLabel(i);
                Method method = this.methodMap.get(label);
                if (method == null) {
                    Field field = this.fieldMap.get(label);
                    if (field != null) {
                        field.set(bean, rs.getObject(label));
                    }
                } else {
                    method.invoke(bean,rs.getObject(label));
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new DataAccessException(String.format("不能映射结果集到类：%s",this.clazz.getName()),e);
        }
        return bean;
    }
}
