package org.js.mvc.autumn.servlet;

import org.js.mvc.autumn.annotation.*;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author JiaShun
 * @date 2018/8/14
 */
public class DispatcherServlet extends HttpServlet {

    private Properties contextConfig = new Properties();
    private List<String> classNames = new ArrayList<>();
    private Map<String,Object> ioc = new HashMap<>();
    private List<Handler> handleMapping = new ArrayList<>();

    /**
     * ===========初始阶段=========
     * @param config
     */
    @Override
    public void init(ServletConfig config) {
        //1：加载配置文件
        doLoadConfig(config.getInitParameter("contextConfigLocation"));
        //2：扫描到所有相关的类
        doScanner(contextConfig.getProperty("scanPackage"));
        //3：初始化刚刚扫描到的类，并将其存入到IOC容器中
        doInstance();
        //4：自动注入
        doAutowired();
        //5：初始化HandlerMapping
        initHandlerMapping();

        System.out.println("Autumn MVC is init !");

        //6：等待请求，匹配URL，定位方法，反射调用执行。
        // 调用doGet()，doPost()

    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        this.doPost(req,resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try{
            doDispatcher(req,resp);
        }catch (Exception e){
            resp.getWriter().write("500" + e.getMessage());
        }
    }

    private void doDispatcher(HttpServletRequest req, HttpServletResponse resp) throws Exception{
        Handler handler = getHandler(req);
        if(handler == null){
            resp.getWriter().write("404 Not Found");
            return;
        }
        Class<?>[] paramTypes = handler.method.getParameterTypes();
        //保存所有需要自动赋值的参数值
        Object[] paramValues = new Object[paramTypes.length];
        Map<String,String[]> params = req.getParameterMap();
        for(Map.Entry<String,String[]> param : params.entrySet()){
            String value = Arrays.toString(param.getValue()).replaceAll("\\[","")
                    .replaceAll("\\]","").replaceAll(",\\s",",");
            //如果找到匹配的对象，则开始填充参数值
            if(!handler.paramIndexMapping.containsKey(param.getKey())) {
                continue;
            }
            int index = handler.paramIndexMapping.get(param.getKey());
            paramValues[index] = convert(paramTypes[index],value);
        }
        //设置方法中的request和response对象
        int reqIndex = handler.paramIndexMapping.get(HttpServletRequest.class.getName());
        paramValues[reqIndex] = req;
        int respIndex = handler.paramIndexMapping.get(HttpServletResponse.class.getName());
        paramValues[respIndex] = resp;
        handler.method.invoke(handler.controller,paramValues);
    }

    /**
     * Handler内部类:记录Controller中RequestMapping和Method的对应关系
     */
    private class Handler{
        /**
         * 保存方法对应的实例
         */
        Object controller;
        /**
         * 保存映射的方法
         */
        Method method;
        Pattern pattern;
        /**
         * 参数顺序
         */
        Map<String,Integer> paramIndexMapping;
        
        Handler(Pattern pattern,Object controller,Method method){
            this.controller = controller;
            this.method = method;
            this.pattern = pattern;
            paramIndexMapping = new HashMap<>();
            putParamIndexMapping(method);
        }

        private void putParamIndexMapping(Method method) {
            //提取方法中注解的参数
            Annotation[] []pa = method.getParameterAnnotations();
            for(int i=0; i<pa.length; i++){
                for(Annotation an : pa[i]){
                    if(an instanceof RequestParam){
                        String paramName = ((RequestParam) an).value();
                        if(!"".equals(paramName.trim())) {
                            paramIndexMapping.put(paramName,i);
                        }
                    }
                }
            }
            //提取方法中request和response的参数
            Class<?>[] paramTypes = method.getParameterTypes();
            for(int i=0; i<paramTypes.length; i++){
                Class<?> type = paramTypes[i];
                if(type == HttpServletRequest.class || type == HttpServletResponse.class){
                    paramIndexMapping.put(type.getName(),i);
                }
            }
        }
    }

    private Handler getHandler(HttpServletRequest req) {
        if(handleMapping.isEmpty()) {
            return null;
        }
        String uri = req.getRequestURI();
        String contextPath = req.getContextPath();
        uri = uri.replace(contextPath,"").replaceAll("/+","/");
        for(Handler handler :handleMapping){
            Matcher matcher = handler.pattern.matcher(uri);
            if(!matcher.matches()) {
                continue;
            }
            return handler;
        }
        return null;
    }

    private Object convert(Class<?> type,String value){
        if(Integer.class == type){
            return Integer.valueOf(value);
        }
        return value;
    }



    /**
     * 初始化HandleMapping
     * 和请求url对应
     */
    private void initHandlerMapping() {
        if(ioc.isEmpty()) {
            return;
        }
        for(Map.Entry<String,Object> entry : ioc.entrySet()){
            Class<?> clazz = entry.getValue().getClass();
            if(!clazz.isAnnotationPresent(Controller.class)) {
                continue;
            }
            String baseUrl = "";
            if(clazz.isAnnotationPresent(RequestMapping.class)){
                RequestMapping requestMapping = clazz.getAnnotation(RequestMapping.class);
                baseUrl = requestMapping.value();
            }
            //Spring中只有public的方法才会被扫描到
            for(Method method : clazz.getMethods()){
                if(!method.isAnnotationPresent(RequestMapping.class)) {
                    continue;
                }
                RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
                String regex = ("/" + baseUrl + "/" +  requestMapping.value()).replaceAll("/+" ,"/");
                Pattern pattern = Pattern.compile(regex);
                handleMapping.add(new Handler(pattern,entry.getValue(),method));
                System.out.println("Mapped：" + regex +"," + method);
            }
        }
    }

    /**
     * 依赖注入
     * 遍历IOC容器，获取Bean实例的属性。
     * 如果有属性带有@Autowired注解，就将属性set注入
     */
    private void doAutowired() {
        if(ioc.isEmpty()) {
            return;
        }
        for(Map.Entry<String,Object> entry : ioc.entrySet()){
            //获得IOC容器中Bean实例的所有属性
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            if(fields.length == 0) {
                continue;
            }
            for(Field field : fields){
                //只有带有@Autowired注解的才会DI
                if(!field.isAnnotationPresent(Autowired.class)) {
                    continue;
                }
                Autowired autowired = field.getAnnotation(Autowired.class);
                String beanName = autowired.value().trim();
                if("".equals(beanName)) {
                    beanName = field.getType().getName();
                }
                //强制授权
                field.setAccessible(true);
                try {
                    //将带有@Autowired注解的类的实例set进entry类中
                    field.set(entry.getValue(),ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 初始化扫描包下(List结合中)
     * 所有带有@Controller和@Service注解的类
     */
    private void doInstance() {
        if(classNames.isEmpty()) {
            return;
        }
        try{
            for(String className : classNames){
                Class<?> clazz = Class.forName(className);
                //初始化
                //判断，不是所有的类都初始化的。
                if(clazz.isAnnotationPresent(Controller.class)){
                    Object instance = clazz.getDeclaredConstructor().newInstance();
                    String beanName = lowerFirstCase(clazz.getSimpleName());
                    ioc.put(beanName,instance);
                } else if(clazz.isAnnotationPresent(Service.class)){
                    //1：默认是首字母小写
                    //2：优先使用自定义beanId
                    //3：key接口的type
                    Service service = clazz.getAnnotation(Service.class);
                    String beanName = service.value();
                    if("".equals(beanName.trim())) {
                        beanName = lowerFirstCase(clazz.getSimpleName());
                    }
                    Object instance = clazz.getConstructor().newInstance();
                    ioc.put(beanName,instance);
                    //实例化接口,key为接口名称，value为实现类
                    Class<?>[] interfaces = clazz.getInterfaces();
                    for(Class<?> i : interfaces){
                        ioc.put(i.getName(),instance);
                    }
                } else {
                    continue;
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 扫描出包下所有类，并放入一个List集合。
     * @param scanPackage
     */
    private void doScanner(String scanPackage) {
        URL url = this.getClass().getClassLoader().getResource("/" +
                scanPackage.replaceAll("\\.","/"));
        File classDir = new File(url.getFile());
        for(File file : classDir.listFiles()){
            if(file.isDirectory()){
                doScanner(scanPackage + "." + file.getName());
            } else{
                String className = (scanPackage + "." + file.getName().replace(".class",""));
                classNames.add(className);
            }
        }
    }

    /**
     * 加载配置文件
     * @param config
     */
    private void doLoadConfig(String config) {
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(config);
        try {
            contextConfig.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(null != is){
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }



    /**
     * 字符串首字母变小写
     * @param str
     * @return
     */
    private String lowerFirstCase(String str){
        char[] chars = str.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }
}
