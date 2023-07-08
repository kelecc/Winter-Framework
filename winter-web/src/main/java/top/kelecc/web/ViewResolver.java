package top.kelecc.web;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * @author 可乐
 */
public interface ViewResolver {

    /**
     * 初始化ViewResolver
     */
    void init();

    /**
     * 渲染
     *
     * @param viewName
     * @param model
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    void render(String viewName, Map<String, Object> model, HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException;

}
