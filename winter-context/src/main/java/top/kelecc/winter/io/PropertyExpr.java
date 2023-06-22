package top.kelecc.winter.io;

/**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/6/18 16:39
 */
public class PropertyExpr {
    private String key;
    private String defaultValue;

    public PropertyExpr(String key, String defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }

    public String getKey() {
        return key;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"key\":").append(key == null ? "" : "\"")
                .append(key).append(key == null ? "" : "\"");
        sb.append(",\"defaultValue\":").append(defaultValue == null ? "" : "\"")
                .append(defaultValue).append(defaultValue == null ? "" : "\"");
        sb.append('}');
        return sb.toString();
    }
}
