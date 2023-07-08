package top.kelecc.web;

import jakarta.annotation.Nullable;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/7/6 19:21
 */
public class ModelAndView {
    private String view;
    private Map<String, Object> model;
    int status;

    public ModelAndView(String view, @Nullable Map<String, Object> model, int status) {
        this.view = view;
        this.status = status;
        if (model != null) {
            addModel(model);
        }
    }

    public ModelAndView(String view, @Nullable Map<String, Object> model) {
        this(view, model, HttpServletResponse.SC_OK);
    }

    public ModelAndView(String view) {
        this(view, null, HttpServletResponse.SC_OK);
    }

    public ModelAndView(String view, int status) {
        this(view, null, status);
    }

    public ModelAndView(String view, String modelName, Object model) {
        this(view, null, HttpServletResponse.SC_OK);
        addModel(modelName, model);
    }

    public String getView() {
        return view;
    }

    public Map<String, Object> getModel() {
        return model;
    }

    public int getStatus() {
        return status;
    }

    public void addModel(String key, Object value) {
        if (this.model == null) {
            this.model = new HashMap<>();
        }
        this.model.put(key, value);
    }

    public void addModel(Map<String, Object> map) {
        if (this.model == null) {
            this.model = new HashMap<>();
        }
        this.model.putAll(map);
    }
}
