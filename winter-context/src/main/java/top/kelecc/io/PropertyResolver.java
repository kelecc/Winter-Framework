package top.kelecc.io;

import jakarta.annotation.Nullable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;

/**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/6/18 16:12
 */
public class PropertyResolver {
    Map<String, String> properties = new HashMap<>();
    Map<Class<?>, Function<String, Object>> converters = new HashMap<>();

    public PropertyResolver(Properties props) {
        //初始化converters
        initConverters();
        //存入环境变量
        this.properties.putAll(System.getenv());
        //获取props的key集合
        Set<String> names = props.stringPropertyNames();
        names.forEach(name -> this.properties.put(name, props.getProperty(name)));
    }

    /**
     * 通过key获取value
     * 支持通过${database:${db:redis}}进行嵌套，冒号后为默认值
     *
     * @param key
     * @return key存在返回value否则返回null
     */
    @Nullable
    public String getProperty(String key) {
        PropertyExpr parse = parse(key);
        if (Objects.isNull(parse)) {
            return this.properties.get(key);
        }
        String defaultValue = parse.getDefaultValue();
        String parseKey = parse.getKey();
        String s = this.properties.get(parseKey);
        if (s != null) {
            return s;
        }
        if (defaultValue != null) {
            if (defaultValue.startsWith("${") && defaultValue.endsWith("}")) {
                return getProperty(parse.getDefaultValue());
            }
            return defaultValue;
        }
        return null;
    }

    @Nullable
    public <T> T getProperty(String key, Class<T> targetType) {
        String value = getProperty(key);
        if (value == null) {
            return null;
        }
        return convert(value, targetType);
    }

    @SuppressWarnings("unchecked")
    private <T> T convert(String value, Class<T> targetType) {
        Function<String, Object> mapper = this.converters.get(targetType);
        if (Objects.isNull(mapper)) {
            throw new IllegalArgumentException("类型不支持：" + targetType.getName());
        }
        T target = null;
        try {
            target = (T) mapper.apply(value);
        } catch (Exception e) {
            throw new ClassCastException("类型转换失败：java.lang.String("+ value +")====>" + targetType.getName());
        }
        return target;
    }

    /**
     * 初始化converters
     */
    private void initConverters(){
        converters.put(int.class,Integer::parseInt);
        converters.put(Integer.class,Integer::valueOf);

        converters.put(byte.class, Byte::parseByte);
        converters.put(Byte.class, Byte::valueOf);

        converters.put(short.class, Short::parseShort);
        converters.put(Short.class, Short::valueOf);

        converters.put(long.class, Long::parseLong);
        converters.put(Long.class, Long::valueOf);

        converters.put(float.class, Float::parseFloat);
        converters.put(Float.class, Float::valueOf);

        converters.put(double.class, Double::parseDouble);
        converters.put(Double.class, Double::valueOf);

        converters.put(boolean.class, Boolean::parseBoolean);
        converters.put(Boolean.class, Boolean::valueOf);

        converters.put(char.class, s -> s.charAt(0));
        converters.put(Character.class, s -> s.charAt(0));

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        converters.put(LocalDate.class, s -> LocalDate.parse(s, dateFormatter));

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        converters.put(LocalDateTime.class, s -> LocalDateTime.parse(s, dateTimeFormatter));

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        converters.put(LocalTime.class, s -> LocalTime.parse(s, timeFormatter));

        converters.put(String.class, s -> s);

    }

    /**
     * 向PropertyResolver中注册converter
     */
    public   void registerConverter(Map<Class<?>, Function<String, Object>> newConverters){
        this.converters.putAll(newConverters);
    }
    /**
     * 解析key是否是`${}`这种格式的
     *
     * @param key
     * @return 是返回解析后的PropertyExpr否则返回null
     */
    PropertyExpr parse(String key) {
        if (key.startsWith("${") && key.endsWith("}")) {
            //先判断是否含有默认值
            int i = key.indexOf(":");
            if (i == -1) {
                String parseKey = key.substring(2, key.length() - 1);
                return new PropertyExpr(parseKey, null);
            } else {
                String parseKey = key.substring(2, i);
                String defaultValue = key.substring(i + 1, key.length() - 1);
                return new PropertyExpr(parseKey, defaultValue);
            }
        }
        return null;
    }

}
