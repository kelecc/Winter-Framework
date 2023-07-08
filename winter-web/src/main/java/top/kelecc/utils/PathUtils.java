package top.kelecc.utils;

import javax.servlet.ServletException;
import java.util.regex.Pattern;

/**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/7/1 20:57
 */
public class PathUtils {
    public static Pattern compile(String path) throws ServletException {
        String regPath = path.replaceAll("\\{([a-zA-Z][a-zA-Z0-9]*)\\}", "(?<$1>[^/]*)");
        if (regPath.indexOf('{') >= 0 || regPath.indexOf('}') >= 0) {
            throw new ServletException("无效的路径: " + path);
        }
        return Pattern.compile("^" + regPath + "$");
    }
}
