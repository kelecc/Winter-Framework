package top.kelecc.web;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.kelecc.Exception.ErrorResponseException;
import top.kelecc.Exception.ServerErrorException;
import top.kelecc.Exception.ServerWebInputException;
import top.kelecc.annotation.*;
import top.kelecc.utils.JsonUtils;
import top.kelecc.utils.PathUtils;
import top.kelecc.utils.WebUtils;
import top.kelecc.winter.context.ApplicationContext;
import top.kelecc.winter.context.BeanDefinition;
import top.kelecc.winter.context.ConfigurableApplicationContext;
import top.kelecc.winter.exception.NestedRuntimeException;
import top.kelecc.winter.io.PropertyResolver;
import top.kelecc.winter.util.ClassUtils;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/6/30 18:09
 */
public class DispatcherServlet extends HttpServlet {
    final Logger logger = LoggerFactory.getLogger(getClass());
    ApplicationContext applicationContext;
    ViewResolver viewResolver;

    List<Dispatcher> getDispatchers = new ArrayList<>();
    List<Dispatcher> postDispatchers = new ArrayList<>();
    String faviconPath;
    String resourcePath;
    int fileBufferSize;

    public DispatcherServlet(ApplicationContext applicationContext, PropertyResolver propertyResolver) {
        this.applicationContext = applicationContext;
        this.resourcePath = propertyResolver.getProperty("${winter.web.static-path:/static/}");
        this.faviconPath = propertyResolver.getProperty("${winter.web.favicon-path:/favicon.ico}");
        this.fileBufferSize = Integer.parseInt(Objects.requireNonNull(propertyResolver.getProperty("${winter.web.file-buffer-size:8192}")));
        this.viewResolver = applicationContext.getBean(ViewResolver.class);
        if (!this.resourcePath.endsWith("/")) {
            this.resourcePath = this.resourcePath + "/";
        }
    }


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String uri = URLDecoder.decode(req.getRequestURI(), "utf-8");
        if (uri.equals(this.faviconPath) || uri.startsWith(this.resourcePath)) {
            doResource(uri, req, resp);
        } else {
            doService(req, resp, this.getDispatchers);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doService(req, resp, this.postDispatchers);
    }

    private void doService(HttpServletRequest req, HttpServletResponse resp, List<Dispatcher> dispatchers) throws IOException, ServletException {
        String uri = null;
        try {
            req.setCharacterEncoding("utf-8");
            resp.setCharacterEncoding("utf-8");
            uri = URLDecoder.decode(req.getRequestURI(), "utf-8");
            doService(uri, req, resp, dispatchers);
        } catch (ErrorResponseException e) {
            logger.warn("请求：'" + uri + "' 处理失败，状态码为：" + e.statusCode);
            if (!resp.isCommitted()) {
                resp.resetBuffer();
                resp.sendError(e.statusCode);
            }
        } catch (RuntimeException | ServletException | IOException e) {
            logger.warn("请求：'" + uri + "' 处理失败。");
            throw e;
        } catch (Exception e) {
            logger.warn("请求：'" + uri + "' 处理失败。");
            throw new NestedRuntimeException(e);
        }
    }

    private void doService(String uri, HttpServletRequest req, HttpServletResponse resp, List<Dispatcher> dispatchers) throws Exception {
        //遍历集合找到匹配的dispatcher
        for (Dispatcher dispatcher : dispatchers) {
            //匹配uri进行处理，若不匹配返回的Result{processed: false; returnObject： null;}
            Result result = dispatcher.process(uri, req, resp);
            //如果匹配上了且处理完
            if (result.processed) {
                Object returnObject = result.returnObject;
                //判断是不是rest
                if (dispatcher.isResponseBody) {
                    if (!resp.isCommitted()) {
                        resp.setContentType("application/json");
                    }
                    //handler返回值为对象，则将其转为json响应
                    PrintWriter writer = resp.getWriter();
                    JsonUtils.writeJson(writer, returnObject);
                    writer.flush();
                } else {
                    if (!resp.isCommitted()) {
                        resp.setContentType("text/html");
                    }
                    if (returnObject instanceof String) {
                        String returnString = (String) returnObject;
                        if (dispatcher.isResponseBody) {
                            PrintWriter writer = resp.getWriter();
                            writer.write(returnString);
                            writer.flush();
                        } else if (returnString.startsWith("redirect:")) {
                            resp.sendRedirect(returnString.substring(9));
                        } else {
                            throw new ServletException("无法处理handler: '" + uri + "' 返回的String结果！");
                        }
                    } else if (returnObject instanceof byte[]) {
                        byte[] data = (byte[]) returnObject;
                        if (dispatcher.isResponseBody) {
                            ServletOutputStream outputStream = resp.getOutputStream();
                            outputStream.write(data);
                            outputStream.flush();
                        } else {
                            throw new ServletException("无法处理handler: '" + uri + "' 返回的byte[]结果！");
                        }
                    } else if (returnObject instanceof ModelAndView) {
                        ModelAndView mv = (ModelAndView) returnObject;
                        String view = mv.getView();
                        if (view.startsWith("redirect:")) {
                            resp.sendRedirect(view.substring(9));
                        } else {
                            this.viewResolver.render(view, mv.getModel(), req, resp);
                        }
                    } else if (!dispatcher.isVoid && returnObject != null) {
                        throw new ServletException("无法处理handler: '" + uri + "' 返回的" + returnObject.getClass().getName() + "结果！");
                    }
                }
                return;
            }
        }
        resp.sendError(404, "Not Found!");
    }

    private void doResource(String uri, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ServletContext ctx = req.getServletContext();
        try (InputStream input = ctx.getResourceAsStream(uri)) {
            if (input == null) {
                resp.sendError(404, "Not Found!");
            } else {
                //推断类型
                String file = uri;
                int n = uri.lastIndexOf('/');
                if (n > 0) {
                    file = uri.substring(n + 1);
                }
                String mimeType = ctx.getMimeType(file);
                if (mimeType == null) {
                    mimeType = "application/octet-stream";
                }
                resp.setContentType(mimeType);
                ServletOutputStream output = resp.getOutputStream();
                byte[] buffer = new byte[fileBufferSize];
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
                output.flush();
            }
        }
    }

    @Override
    public void destroy() {
        this.applicationContext.close();
    }

    @Override
    public void init() throws ServletException {
        logger.info("初始化 {}", getClass().getName());
        //扫描@Controller和@RestController
        for (BeanDefinition beanDefinition : ((ConfigurableApplicationContext) this.applicationContext).findBeanDefinitions(Object.class)) {
            Class<?> beanClass = beanDefinition.getBeanClass();
            Object bean = beanDefinition.getRequiredInstance();
            Controller controller = beanClass.getAnnotation(Controller.class);
            RestController restController = beanClass.getAnnotation(RestController.class);
            if (controller != null && restController != null) {
                throw new ServletException("在类: '" + beanClass.getName() + "'" + "同时扫描到@Controller和@RestController注解！");
            }
            if (controller != null) {
                addController(false, beanDefinition.getName(), bean);
            }
            if (restController != null) {
                addController(true, beanDefinition.getName(), bean);
            }
        }
    }

    void addController(boolean isRest, String name, Object bean) throws ServletException {
        logger.info("添加 controller '{}': {}", name, bean.getClass().getName());
        addMethods(isRest, bean, bean.getClass());
    }

    void addMethods(boolean isRest, Object bean, Class<?> type) throws ServletException {
        for (Method method : type.getDeclaredMethods()) {
            GetMapping getMapping = method.getAnnotation(GetMapping.class);
            if (getMapping != null) {
                checkMethod(method);
                this.getDispatchers.add(new Dispatcher(isRest, bean, method, getMapping.value()));
            }
            PostMapping postMapping = method.getAnnotation(PostMapping.class);
            if (postMapping != null) {
                checkMethod(method);
                this.postDispatchers.add(new Dispatcher(isRest, bean, method, postMapping.value()));
            }
        }
        Class<?> superClass = type.getSuperclass();
        if (superClass != null) {
            addMethods(isRest, bean, superClass);
        }
    }

    void checkMethod(Method method) throws ServletException {
        int modifiers = method.getModifiers();
        boolean isStatic = Modifier.isStatic(modifiers);
        if (isStatic) {
            throw new ServletException("不能映射URL到静态方法上: " + method);
        }
        method.setAccessible(true);
    }

    static class Dispatcher {
        final static Result NOT_PROCESSED = new Result(false, null);
        final Logger logger = LoggerFactory.getLogger(getClass());
        boolean isRest;
        boolean isResponseBody;
        boolean isVoid;
        Pattern urlPattern;
        Object controller;
        Method handlerMethod;
        Param[] methodParameters;

        public Dispatcher(boolean isRest, Object controller, Method method, String urlPattern) throws ServletException {
            this.isRest = isRest;
            this.isResponseBody = isRest || method.getAnnotation(ResponseBody.class) != null;
            this.isVoid = method.getReturnType() == void.class;
            this.urlPattern = PathUtils.compile(urlPattern);
            this.controller = controller;
            this.handlerMethod = method;
            Parameter[] parameters = method.getParameters();
            Annotation[][] parameterAnnotations = method.getParameterAnnotations();
            this.methodParameters = new Param[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                this.methodParameters[i] = new Param(method, parameters[i], parameterAnnotations[i]);
            }
            logger.debug("映射 {} 到 handler {}.{}", urlPattern, controller.getClass().getSimpleName(), method.getName());
            if (logger.isDebugEnabled()) {
                for (Param param : this.methodParameters) {
                    logger.debug("参数: {}", param);
                }
            }
        }

        Result process(String uri, HttpServletRequest req, HttpServletResponse resp) throws Exception {
            Matcher matcher = urlPattern.matcher(uri);
            if (matcher.matches()) {
                Object[] arguments = new Object[this.methodParameters.length];
                for (int i = 0; i < methodParameters.length; i++) {
                    Param param = this.methodParameters[i];
                    switch (param.paramType) {
                        case PATH_VARIABLE:
                            try {
                                String str = matcher.group(param.name);
                                arguments[i] = convertToType(param.classType, str);
                                break;
                            } catch (IllegalArgumentException e) {
                                throw new ServerWebInputException("Path variable '" + param.name + "' not found.");
                            }
                        case REQUEST_BODY:
                            BufferedReader reader = req.getReader();
                            arguments[i] = JsonUtils.readJson(reader, param.classType);
                            break;
                        case REQUEST_PARAM:
                            arguments[i] = convertToType(param.classType,getOrDefault(req, param.name, param.defaultValue));
                            break;
                        case SERVLET_VARIABLE:
                            Class<?> classType = param.classType;
                            if (classType == HttpServletRequest.class) {
                                arguments[i] = req;
                            } else if (classType == HttpServletResponse.class) {
                                arguments[i] = resp;
                            } else if (classType == HttpSession.class) {
                                arguments[i] = req.getSession();
                            } else if (classType == ServletContext.class) {
                                arguments[i] = req.getServletContext();
                            } else {
                                throw new ServerErrorException("无法确定参数类型: " + classType);
                            }
                            break;
                        default:
                    }
                }
                Object result = null;
                try {
                    result = this.handlerMethod.invoke(this.controller, arguments);
                } catch (InvocationTargetException e) {
                    Throwable t = e.getCause();
                    if (t instanceof Exception) {
                        throw (Exception) t;
                    }
                    throw e;
                } catch (ReflectiveOperationException e) {
                    throw new ServerErrorException(e);
                }
                return new Result(true, result);
            }
            return NOT_PROCESSED;
        }

        String getOrDefault(HttpServletRequest req, String name, String defaultValue) {
            String parameter = req.getParameter(name);
            if (parameter == null) {
                if (WebUtils.DEFAULT_PARAM_VALUE.equals(defaultValue)) {
                    throw new ServerWebInputException("请求参数 '" + name + "'未找到！");
                }
                return defaultValue;
            }
            return parameter;
        }

        Object convertToType(Class<?> classType, String str) {
            if (classType == String.class) {
                return str;
            } else if (classType == boolean.class || classType == Boolean.class) {
                return Boolean.valueOf(str);
            } else if (classType == int.class || classType == Integer.class) {
                return Integer.valueOf(str);
            } else if (classType == long.class || classType == Long.class) {
                return Long.valueOf(str);
            } else if (classType == byte.class || classType == Byte.class) {
                return Byte.valueOf(str);
            } else if (classType == short.class || classType == Short.class) {
                return Short.valueOf(str);
            } else if (classType == float.class || classType == Float.class) {
                return Float.valueOf(str);
            } else if (classType == double.class || classType == Double.class) {
                return Double.valueOf(str);
            } else {
                throw new ServerErrorException("无法确定参数类型: " + classType);
            }
        }
    }

    static class Param {
        String name;
        ParamType paramType;
        Class<?> classType;
        String defaultValue;

        public Param(Method method, Parameter parameter, Annotation[] annotations) throws ServletException {
            PathVariable pathVariable = ClassUtils.getAnnotation(PathVariable.class, annotations);
            RequestParam requestParam = ClassUtils.getAnnotation(RequestParam.class, annotations);
            RequestBody requestBody = ClassUtils.getAnnotation(RequestBody.class, annotations);
            int total = (pathVariable == null ? 0 : 1) + (requestParam == null ? 0 : 1) + (requestBody == null ? 0 : 1);
            if (total > 1) {
                throw new ServletException(String.format("在方法: '%s'中，注解@PathVariable、@RequestParam和@RequestBody只能标注其中一个。", method));
            }
            this.classType = parameter.getType();
            if (pathVariable != null) {
                this.name = pathVariable.value();
                this.paramType = ParamType.PATH_VARIABLE;
            } else if (requestParam != null) {
                this.name = requestParam.value();
                this.paramType = ParamType.REQUEST_PARAM;
                this.defaultValue = requestParam.defaultValue();
            } else if (requestBody != null) {
                this.paramType = ParamType.REQUEST_BODY;
            } else {
                this.paramType = ParamType.SERVLET_VARIABLE;
                if (this.classType != HttpServletRequest.class && this.classType != HttpServletResponse.class && this.classType != HttpSession.class && this.classType != ServletContext.class) {
                    throw new ServletException(String.format("方法: '%s' 中包含未知参数: '%s'.", method, classType));
                }
            }
        }

        @Override
        public String toString() {
            return "Param [name=" + name + ", paramType=" + paramType + ", classType=" + classType + ", defaultValue=" + defaultValue + "]";
        }
    }

    static enum ParamType {
        /**
         * PATH_VARIABLE：路径参数，从URL中提取；
         * REQUEST_PARAM：URL参数，从URL Query或Form表单提取；
         * REQUEST_BODY：REST请求参数，从Post传递的JSON提取；
         * SERVLET_VARIABLE：HttpServletRequest等Servlet API提供的参数，直接从DispatcherServlet的方法参数获得。
         */
        PATH_VARIABLE, REQUEST_PARAM, REQUEST_BODY, SERVLET_VARIABLE;
    }

    static class Result {
        private boolean processed;
        private Object returnObject;

        public Result(boolean processed, Object returnObject) {
            this.processed = processed;
            this.returnObject = returnObject;
        }

        public boolean isProcessed() {
            return processed;
        }

        public Object getReturnObject() {
            return returnObject;
        }

        public void setProcessed(boolean processed) {
            this.processed = processed;
        }

        public void setReturnObject(Object returnObject) {
            this.returnObject = returnObject;
        }

        @Override
        public String toString() {
            return "Result{" +
                    "processed=" + processed +
                    ", returnObject=" + returnObject +
                    '}';
        }
    }

}
