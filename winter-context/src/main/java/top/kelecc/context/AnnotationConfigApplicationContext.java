package top.kelecc.context;

import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.kelecc.annotation.*;
import top.kelecc.exception.*;
import top.kelecc.io.PropertyResolver;
import top.kelecc.io.ResourceResolver;
import top.kelecc.util.ClassUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/6/19 14:07
 */
public class AnnotationConfigApplicationContext implements ConfigurableApplicationContext {
    private final Map<String, BeanDefinition> beans;
    private final PropertyResolver propertyResolver;
    private final Set<String> creatingBeanNames;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final List<BeanPostProcessor> beanPostProcessors = new ArrayList<>();

    public AnnotationConfigApplicationContext(Class<?> configClass, PropertyResolver propertyResolver) {
        ApplicationContextUtils.setApplicationContext(this);
        this.propertyResolver = propertyResolver;
        logger.debug("================开始扫描所有字节码================");
        //1.扫描获取所有的class类型
        Set<String> classNames = scanForClassNames(configClass);
        logger.debug("================开始扫描字节码中的注解================");
        //2.扫描所有字节码中的注解，创建BeanDefinition
        this.beans = createBeanDefinitions(classNames);
        this.creatingBeanNames = new HashSet<>();
        //3.创建@Configuration的工厂类,由于@Configuration标识的Bean实际上是工厂，它们必须先实例化，才能实例化其他普通Bean，所以我们先把@Configuration标识的Bean创建出来，再创建普通Bean。
        logger.debug("================开始创建@Configuration配置类================");
        createConfigurationBean();
        //4.创建BeanPostProcessor
        logger.debug("================开始创建BeanPostProcessor================");
        createBeanPostProcessor();
        //5.创建普通bean
        logger.debug("================开始创建普通Bean================");
        createNormalBeans();
        //6.通过set方法和字段注入
        logger.debug("================开始set方法和字段注入================");
        this.beans.values().forEach(this::injectBean);
        //7.调用所有bean的init方法
        logger.debug("================开始调用所有bean的init方法================");
        this.beans.values().stream().filter(beanDefinition -> !beanDefinition.isInit()).forEach(this::initBean);
        if (logger.isDebugEnabled()) {
            this.beans.values().stream().sorted().forEach(def -> {
                logger.debug("bean 初始化结果: {}", def);
            });
        }
    }

    /**
     * 调用init方法
     *
     * @param beanDefinition
     */
    private void initBean(BeanDefinition beanDefinition) {
        Object instance = getProxiedInstance(beanDefinition);
        callInitMethod(beanDefinition, instance);

        //调用BeanPostProcessor.postProcessAfterInitialization():
        beanPostProcessors.forEach(beanPostProcessor -> {
            Object processedInstance = beanPostProcessor.postProcessAfterInitialization(beanDefinition.getInstance(), beanDefinition.getName());
            logger.debug("{}的postProcessAfterInitialization方法将{} 替换成了 {}", beanPostProcessor.getClass().getSimpleName(), beanDefinition.getInstance().getClass().getName(), processedInstance.getClass().getName());
            beanDefinition.setInstance(processedInstance);
        });
    }

    private void callInitMethod(BeanDefinition def, Object instance) {
        String initMethodName = def.initMethodName;
        Method initMethod = def.initMethod;
        if (!Objects.isNull(initMethod)) {
            try {
                initMethod.invoke(instance);
            } catch (Exception e) {
                throw new BeanCreationException(e);
            }
        } else if (!Objects.isNull(initMethodName)) {
            String factoryName = def.getFactoryName();
            BeanDefinition factoryDef = findBeanDefinition(factoryName);
            Method method = ClassUtils.getMethodByName(factoryDef.getBeanClass(), initMethodName);
            method.setAccessible(true);
            try {
                method.invoke(factoryDef.getInstance());
            } catch (Exception e) {
                throw new BeanCreationException(e);
            }
        }
        def.setInit();
    }

    /**
     * 注入依赖但不调用init方法
     *
     * @param beanDefinition
     */
    private void injectBean(BeanDefinition beanDefinition) {
        final Object instance = getProxiedInstance(beanDefinition);
        try {
            injectProperties(beanDefinition, beanDefinition.getBeanClass(), instance);
        } catch (ReflectiveOperationException e) {
            throw new BeanCreationException(e);
        }
    }

    /**
     * 注入属性
     *
     * @param def
     * @param clazz
     * @param instance
     * @throws ReflectiveOperationException
     */
    private void injectProperties(BeanDefinition def, Class<?> clazz, Object instance) throws ReflectiveOperationException {
        for (Method method : clazz.getDeclaredMethods()) {
            tryInjectProperties(def, method, instance);
        }
        for (Field field : clazz.getDeclaredFields()) {
            tryInjectProperties(def, field, instance);
        }
        // 在父类查找Field和Method并注入:
        Class<?> superclass = clazz.getSuperclass();
        if (!Objects.isNull(superclass)) {
            injectProperties(def, superclass, instance);
        }
    }

    /**
     * 注入单个属性
     *
     * @param def
     * @param accessibleObject Field或者Method
     * @param instance
     */
    private void tryInjectProperties(BeanDefinition def, AccessibleObject accessibleObject, Object instance) throws ReflectiveOperationException {
        Value valueAnno = accessibleObject.getAnnotation(Value.class);
        Autowired autowiredAnno = accessibleObject.getAnnotation(Autowired.class);
        //不是需要注入的属性或方法
        if (Objects.isNull(valueAnno) && Objects.isNull(autowiredAnno)) {
            return;
        }
        Field field = null;
        Method method = null;
        if (accessibleObject instanceof Field) {
            Field f = (Field) accessibleObject;
            //检测需注入的属性是否支持注入
            checkFieldOrMethod(f);
            f.setAccessible(true);
            field = f;
        }
        if (accessibleObject instanceof Method) {
            Method m = (Method) accessibleObject;
            //检测需注入的方法是否支持注入
            checkFieldOrMethod(m);
            if (m.getParameterCount() != 1) {
                throw new BeanDefinitionException(String.format("无法为 bean '%s' : %s 注入非 setter 方法: %s", def.getName(), def.getBeanClass().getName(), m.getName()));
            }
            m.setAccessible(true);
            method = m;
        }

        String accessibleName = field == null ? method.getName() : field.getName();
        Class<?> accessibleType = field == null ? method.getParameterTypes()[0] : field.getType();

        if (!Objects.isNull(valueAnno) && !Objects.isNull(autowiredAnno)) {
            throw new BeanDefinitionException(String.format("注入bean '%s': %s 的 %s.%s 时不能同时有@Autowired和@Value!", def.getName(), def.getBeanClass().getName(), def.getBeanClass().getSimpleName(), accessibleName));
        }

        //@Value注入
        if (!Objects.isNull(valueAnno)) {
            Object property = this.propertyResolver.getProperty(valueAnno.value(), accessibleType);
            if (!Objects.isNull(field)) {
                logger.debug("使用@Value进行属性注入：{}.{} = {}", def.getBeanClass().getName(), accessibleName, property);
                field.set(instance, property);
            }
            if (!Objects.isNull(method)) {
                logger.debug("使用@Value进行方法注入：{}.{} = {}", def.getBeanClass().getName(), accessibleName, property);
                method.invoke(instance, property);
            }
        }
        //@Autowired注入
        if (!Objects.isNull(autowiredAnno)) {
            String name = autowiredAnno.name();
            boolean required = autowiredAnno.value();
            Object dependency = name.isEmpty() ? findBean(accessibleType) : findBean(name, accessibleType);
            if (Objects.isNull(dependency) && required) {
                throw new UnsatisfiedDependencyException(String.format("注入 %s: %s 时未找到依赖的bean: '%s': %s", def.getBeanClass().getSimpleName(), accessibleName, def.getName(), def.getBeanClass().getName()));
            }
            if (!Objects.isNull(dependency)) {
                if (!Objects.isNull(method)) {
                    logger.debug("使用@Autowired进行方法注入：{}.{} = {}", def.getBeanClass().getName(), accessibleName, dependency);
                    method.invoke(instance, dependency);
                }
                if (!Objects.isNull(field)) {
                    logger.debug("使用@Autowired进行属性注入：{}.{} = {}", def.getBeanClass().getName(), accessibleName, dependency);
                    field.set(instance, dependency);
                }
            }
        }

    }

    private void checkFieldOrMethod(Member m) {
        int mods = m.getModifiers();
        if (Modifier.isStatic(mods)) {
            throw new BeanDefinitionException("不能在静态属性上注入：" + m);
        }
        if (Modifier.isFinal(mods)) {
            if (m instanceof Field) {
                throw new BeanDefinitionException("不能在final属性上注入：" + m);
            }
            if (m instanceof Method) {
                logger.warn("对于注入的 final 方法需要小心，因为当 bean 被代理时，该方法不会在目标 bean 上被调用，可能会导致空指针异常。");
            }
        }
    }

    private Object getProxiedInstance(BeanDefinition beanDefinition) {
        Object instance = beanDefinition.getInstance();
        //如果Proxy改变了原始Bean，又希望注入到原始Bean，则由BeanPostProcessor指定原始Bean:
        ArrayList<BeanPostProcessor> reversedBeanPostProcessors = new ArrayList<>(this.beanPostProcessors);
        Collections.reverse(reversedBeanPostProcessors);
        for (BeanPostProcessor beanPostProcessor : reversedBeanPostProcessors) {
            Object restoredInstance = beanPostProcessor.postProcessOnSetProperty(beanDefinition.getInstance(), beanDefinition.getName());
            if (restoredInstance != instance) {
                instance = restoredInstance;
            }
        }
        return instance;
    }

    /**
     * 创建@Configuration的工厂类
     */
    private void createConfigurationBean() {
        this.beans.values().stream()
                .filter(this::isConfigurationDefinition)
                .sorted()
                .forEach(this::createBeanAsEarlySingleton);
    }

    /**
     * 创建BeanPostProcessor
     */
    private void createBeanPostProcessor() {
        List<BeanPostProcessor> collect = this.beans.values().stream()
                .filter(this::isBeanPostProcessor)
                .sorted()
                .map(beanDefinition -> (BeanPostProcessor) createBeanAsEarlySingleton(beanDefinition))
                .collect(Collectors.toList());
        this.beanPostProcessors.addAll(collect);
    }

    /**
     * 创建普通bean
     */
    private void createNormalBeans() {
        this.beans.values().stream()
                .filter(def -> Objects.isNull(def.getInstance()))
                .sorted()
                .forEach(def -> {
                    // 如果Bean未被创建(可能在其他Bean的构造方法注入前被创建):
                    if (Objects.isNull(def.getInstance())) {
                        this.createBeanAsEarlySingleton(def);
                    }
                });
    }

    /**
     * 判断此beanDefinition是否有Configuration注解
     *
     * @param beanDefinition
     * @return
     */
    private boolean isConfigurationDefinition(BeanDefinition beanDefinition) {
        return !Objects.isNull(ClassUtils.findAnnotation(beanDefinition.getBeanClass(), Configuration.class));
    }

    /**
     * 创建早期单例
     *
     * @param beanDefinition
     * @return
     */
    @Override
    public Object createBeanAsEarlySingleton(BeanDefinition beanDefinition) {
        logger.debug("尝试将Bean '{}': {} 创建为早期单例。", beanDefinition.getName(), beanDefinition.getBeanClass().getName());
        //检测是否产生循环依赖
        if (!creatingBeanNames.add(beanDefinition.getName())) {
            throw new UnsatisfiedDependencyException(String.format("创建Bean '%s' 时检测到循环依赖。", beanDefinition.getName()));
        }
        //创建方式，构造函数或工厂方法
        Executable createFn;
        if (beanDefinition.getFactoryName() == null) {
            createFn = beanDefinition.getConstructor();
        } else {
            createFn = beanDefinition.getFactoryMethod();
        }
        //获取创建所需的参数
        final Parameter[] parameters = createFn.getParameters();
        final Annotation[][] parametersAnno = createFn.getParameterAnnotations();
        Object[] args = new Object[parameters.length];
        final boolean isConfiguration = isConfigurationDefinition(beanDefinition);
        final boolean isBeanPostProcessor = isBeanPostProcessor(beanDefinition);
        //遍历注入参数
        for (int i = 0; i < parameters.length; i++) {
            final Parameter parameter = parameters[i];
            final Annotation[] paraAnno = parametersAnno[i];
            final Value valueAnno = ClassUtils.getAnnotation(Value.class, paraAnno);
            final Autowired autowiredAnno = ClassUtils.getAnnotation(Autowired.class, paraAnno);

            // @Configuration类型的Bean是工厂，不允许使用@Autowired创建:
            if (isConfiguration && !Objects.isNull(autowiredAnno)) {
                throw new BeanCreationException(String.format("创建 @Configuration 类型的 Bean '%s' '%s' 时不能指定 @Autowired!", beanDefinition.getName(), beanDefinition.getBeanClass().getName()));
            }
            // BeanPostProcessor不能依赖其他的bean，不允许使用@Autowired创建
            if (isBeanPostProcessor && !Objects.isNull(autowiredAnno)) {
                throw new BeanCreationException(String.format("创建 BeanPostProcessor 类型的 Bean '%s' '%s' 时不能指定 @Autowired!", beanDefinition.getName(), beanDefinition.getBeanClass().getName()));
            }
            // 参数必须使用且只使用@Value或@Autowired其中一个
            if (Objects.isNull(valueAnno) && Objects.isNull(autowiredAnno)) {
                throw new BeanCreationException(String.format("创建 Bean '%s' '%s' 时必须使用且只使用@Value或者@Autowired的一个!", beanDefinition.getName(), beanDefinition.getBeanClass().getName()));
            }
            if (!Objects.isNull(valueAnno) && !Objects.isNull(autowiredAnno)) {
                throw new BeanCreationException(String.format("创建 Bean '%s' '%s' 时必须使用且只使用@Value或者@Autowired的一个!", beanDefinition.getName(), beanDefinition.getBeanClass().getName()));
            }
            final Class<?> type = parameter.getType();
            if (!Objects.isNull(valueAnno)) {
                args[i] = this.propertyResolver.getProperty(valueAnno.value(), type);
            } else {
                //参数是@Autowired标注
                String name = autowiredAnno.name();
                boolean required = autowiredAnno.value();
                //获取所依赖的BeanDefinition
                BeanDefinition dependencyDefinition = name.isEmpty() ? findBeanDefinition(type) : findBeanDefinition(name, type);
                //没找到依赖
                if (required && Objects.isNull(dependencyDefinition)) {
                    throw new BeanCreationException(String.format("创建 bean '%s' '%s' 时缺少类型 '%s' 的bean进行自动装配。", beanDefinition.getName(), beanDefinition.getBeanClass().getName(), type.getName()));
                }
                if (dependencyDefinition != null) {
                    Object dependencyInstance = dependencyDefinition.getInstance();
                    if (dependencyInstance == null) {
                        dependencyInstance = createBeanAsEarlySingleton(dependencyDefinition);
                    }
                    args[i] = dependencyInstance;
                } else {
                    args[i] = null;
                }
            }
        }

        //创建bean
        Object instance;
        if (beanDefinition.getFactoryMethod() == null) {
            //用构造方法
            try {
                instance = beanDefinition.getConstructor().newInstance(args);
            } catch (Exception e) {
                throw new BeanCreationException(String.format("创建Bean '%s': %s" + "时发生异常！", beanDefinition.getName(), beanDefinition.getBeanClass().getName()), e);
            }
        } else {
            //用@Bean方法创建
            Object configInstance = getBean(beanDefinition.getFactoryName());
            try {
                instance = beanDefinition.getFactoryMethod().invoke(configInstance, args);
            } catch (Exception e) {
                throw new BeanCreationException(String.format("创建Bean '%s': %s" + "时发生异常！", beanDefinition.getName(), beanDefinition.getBeanClass().getName()), e);
            }
        }
        //将创建的bean存入beanDefinition的instance
        beanDefinition.setInstance(instance);

        //调用BeanPostProcessor处理bean
        for (BeanPostProcessor processor : beanPostProcessors) {
            Object processed = processor.postProcessBeforeInitialization(beanDefinition.getInstance(), beanDefinition.getName());
            if (processed == null) {
                throw new BeanCreationException(String.format("PostBeanProcessor '%s' 处理Bean '%s' 时返回值为null!", processor, beanDefinition.getName()));
            }
            if (beanDefinition.getInstance() != processed) {
                logger.debug("Bean '{}' 被post processor {} 替换了。", beanDefinition.getName(), processor.getClass().getName());
                beanDefinition.setInstance(processed);
            }
        }

        return beanDefinition.getInstance();
    }

    private boolean isBeanPostProcessor(BeanDefinition beanDefinition) {
        return BeanPostProcessor.class.isAssignableFrom(beanDefinition.getBeanClass());
    }


    /**
     * 扫描@Component、@Bean、@Configuration注解创建BeanDefinition
     *
     * @param classNames
     * @return
     */
    private Map<String, BeanDefinition> createBeanDefinitions(Set<String> classNames) {
        Map<String, BeanDefinition> beanDefinitionsMap = new HashMap<>();
        for (String className : classNames) {
            //1.获取Class
            Class<?> clazz;
            try {
                clazz = Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new BeanCreationException(e);
            }
            //2.如果是接口 枚举 注解 则不管他
            if (clazz.isAnnotation() || clazz.isEnum() || clazz.isInterface()) {
                continue;
            }
            //3.是否有@Component注解
            Component componentAnno = ClassUtils.findAnnotation(clazz, Component.class);
            if (!Objects.isNull(componentAnno)) {
                logger.debug("扫描到一个Component组件： {}", className);
                //4.是否是抽象类
                int mods = clazz.getModifiers();
                if (Modifier.isAbstract(mods)) {
                    throw new BeanDefinitionException("被@Component标注的类 " + className + " 不能是抽象类。");
                }
                //5.是否为private修饰的
                if (Modifier.isPrivate(mods)) {
                    throw new BeanDefinitionException("被@Component标注的类 " + className + " 不能是私有的。");
                }
                String beanName = componentAnno.value();
                //6.未设置beanName则默认类名的首字母小写的驼峰命名
                if ("".equals(beanName)) {
                    beanName = Character.toLowerCase(clazz.getSimpleName().charAt(0)) + clazz.getSimpleName().substring(1);
                }
                BeanDefinition beanDefinition = new BeanDefinition(beanName, clazz, getSuitableConstructors(clazz), getOrder(clazz), clazz.isAnnotationPresent(Primary.class), null, null, ClassUtils.findAnnotationMethod(clazz, PostConstruct.class), ClassUtils.findAnnotationMethod(clazz, PreDestroy.class));
                //7.加入beanDefinitionsMap集合中
                addBeanDefinitions(beanDefinitionsMap, beanDefinition);
                //8.扫描是否有@Configuration注解
                Configuration configurationAnno = ClassUtils.findAnnotation(clazz, Configuration.class);
                if (!Objects.isNull(configurationAnno)) {
                    //9.存在Configuration注解,如果是BeanPostProcessor的实现类，那么会产生冲突
                    if (BeanPostProcessor.class.isAssignableFrom(clazz)) {
                        throw new BeanDefinitionException("@Configuration标注的类 " + clazz.getName() + " 不能是BeanPostProcessor的实现类！");
                    }
                    scanFactoryMethods(beanName, beanDefinitionsMap, clazz);
                }
            }
        }

        return beanDefinitionsMap;
    }

    /**
     * 扫描@Configuration标注类中的@bean方法
     *
     * @param factoryName
     * @param beanDefinitionsMap
     * @param clazz
     */
    private void scanFactoryMethods(String factoryName, Map<String, BeanDefinition> beanDefinitionsMap, Class<?> clazz) {
        for (Method method : clazz.getDeclaredMethods()) {
            Bean beanAnno = method.getAnnotation(Bean.class);
            if (!Objects.isNull(beanAnno)) {
                //1.检查修饰符
                int mods = method.getModifiers();
                //1.1 不能是抽象方法
                if (Modifier.isAbstract(mods)) {
                    throw new BeanDefinitionException("被@Bean标注的" + clazz.getName() + "的" + method.getName() + "方法不能是抽象方法！");
                }
                //1.2 不能是final方法
                if (Modifier.isFinal(mods)) {
                    throw new BeanDefinitionException("被@Bean标注的" + clazz.getName() + "的" + method.getName() + "方法不能是final方法！");
                }
                //1.3 不能是private方法
                if (Modifier.isPrivate(mods)) {
                    throw new BeanDefinitionException("被@Bean标注的" + clazz.getName() + "的" + method.getName() + "方法不能是私有方法！");
                }
                //2. 检查返回值类型
                Class<?> beanClass = method.getReturnType();
                //2.1 返回类型不能是基本类型
                if (beanClass.isPrimitive()) {
                    throw new BeanDefinitionException("被@Bean标注的" + clazz.getName() + "的" + method.getName() + "方法返回值不能是基本类型！");
                }
                //2.2 返回类型不能是void
                if (beanClass == void.class || beanClass == Void.class) {
                    throw new BeanDefinitionException("被@Bean标注的" + clazz.getName() + "的" + method.getName() + "方法返回值不能是void！");
                }
                //3. 创建BeanDefinition
                BeanDefinition beanDefinition = new BeanDefinition(method.getName(), beanClass, factoryName, method, getOrder(method), method.isAnnotationPresent(Primary.class), beanAnno.initMethod().isEmpty() ? null : beanAnno.initMethod(), beanAnno.destroyMethod().isEmpty() ? null : beanAnno.destroyMethod(), null, null);
                addBeanDefinitions(beanDefinitionsMap, beanDefinition);
            }
        }
    }

    /**
     * 查找@Bean方法上的@Order
     *
     * @param method
     * @return
     */
    private int getOrder(Method method) {
        Order annotation = method.getAnnotation(Order.class);
        if (Objects.isNull(annotation)) {
            return Integer.MAX_VALUE;
        }
        return annotation.value();
    }

    /**
     * 将BeanDefinition加入到Map中，如果map中存在同名的则抛出异常
     *
     * @param beanDefinitionsMap
     * @param beanDefinition
     */
    private void addBeanDefinitions(Map<String, BeanDefinition> beanDefinitionsMap, BeanDefinition beanDefinition) {
        if (beanDefinitionsMap.put(beanDefinition.getName(), beanDefinition) != null) {
            throw new BeanDefinitionException("BeanName重复：" + beanDefinition.getName());
        }
        logger.debug("定义了bean: {}", beanDefinition);
    }

    /**
     * 返回order
     *
     * @param clazz
     * @return
     */
    private int getOrder(Class<?> clazz) {
        Order order = clazz.getAnnotation(Order.class);
        if (Objects.isNull(order)) {
            return Integer.MAX_VALUE;
        }
        return order.value();
    }

    /**
     * 获取唯一的一个构造函数
     *
     * @param clazz
     * @return
     */
    private Constructor<?> getSuitableConstructors(Class<?> clazz) {
        Constructor<?>[] constructors = clazz.getConstructors();
        if (constructors.length == 0) {
            constructors = clazz.getDeclaredConstructors();
            if (constructors.length != 1) {
                throw new BeanDefinitionException("类 " + clazz.getName() + " 中构造函数不唯一！");
            }
        }
        if (constructors.length != 1) {
            throw new BeanDefinitionException("类 " + clazz.getName() + " 中公有的构造函数不唯一！");
        }
        return constructors[0];
    }

    /**
     * 通过扫描配置类，获取所有字节码类名
     *
     * @param configClass
     * @return
     */
    public Set<String> scanForClassNames(Class<?> configClass) {
        //获取ComponentScan注解
        ComponentScan componentScan = ClassUtils.findAnnotation(configClass, ComponentScan.class);
        //获取ComponentScan注解中包名，未设置则默认为configClass所在包
        String[] scanPackages = componentScan == null || componentScan.value().length == 0 ? new String[]{configClass.getPackage().getName()} : componentScan.value();

        HashSet<String> classNameSet = new HashSet<>();

        for (String pkg : scanPackages) {
            logger.debug("扫描包： {}", pkg);
            ResourceResolver resourceResolver = new ResourceResolver(pkg);
            List<String> classNameList = resourceResolver.scan(resource -> {
                String name = resource.getName();
                if (name.endsWith(".class")) {
                    return name.substring(0, name.length() - 6).replace("/", ".").replace("\\", ".");
                }
                return null;
            });

            if (logger.isDebugEnabled()) {
                classNameList.forEach((className) -> logger.debug("通过ComponentScan注解扫描到字节码: {}", className));
            }

            classNameSet.addAll(classNameList);
        }

        //继续查找@import(****.class)导入的配置
        Import anImport = configClass.getAnnotation(Import.class);
        if (!Objects.isNull(anImport)) {
            for (Class<?> importClass : anImport.value()) {
                String name = importClass.getName();
                //判断set中是否已经有这个类了
                if (classNameSet.contains(name)) {
                    logger.warn("类{}已存在,本次import已忽略！", name);
                } else {
                    classNameSet.add(name);
                }
            }
        }
        return classNameSet;
    }

    /**
     * 通过bean名字和类型找出对应的BeanDefinition，如果没有同名的返回null，如果有同名的但是类型不匹配将抛出异常。
     *
     * @return BeanDefinition
     */
    @Nullable
    public BeanDefinition findBeanDefinition(String name, Class<?> requiredType) {
        BeanDefinition beanDefinition = findBeanDefinition(name);
        if (Objects.isNull(beanDefinition)) {
            return null;
        }
        if (!requiredType.isAssignableFrom(beanDefinition.getBeanClass())) {
            throw new BeanDefinitionException("找到一个名为： " + name + "的Bean，类型为： " + beanDefinition.getBeanClass().getName() + "，而不是需要的： " + requiredType.getName());
        }
        return beanDefinition;
    }

    /**
     * 通过bean名字找出对应的BeanDefinition
     *
     * @param name beanName
     * @return BeanDefinition
     */
    @Nullable
    public BeanDefinition findBeanDefinition(String name) {
        return this.beans.get(name);
    }

    /**
     * 根据Type查找1个BeanDefinition，返回null或1个
     *
     * @param type 想要找的类型
     * @return BeanDefinition
     */
    @Nullable
    public BeanDefinition findBeanDefinition(Class<?> type) {
        List<BeanDefinition> defs = findBeanDefinitions(type);
        //没找到任何BeanDefinition
        if (defs.isEmpty()) {
            return null;
        }
        //如果只找到一个，那么返回他
        if (defs.size() == 1) {
            return defs.get(0);
        }
        //找到多个Definition是优先查找是否有@Primary
        List<BeanDefinition> primaryDefs = defs.stream().filter(BeanDefinition::isPrimary).collect(Collectors.toList());
        //如果找到唯一的Primary
        if (primaryDefs.size() == 1) {
            return primaryDefs.get(0);
        }
        //Primary不存在
        if (primaryDefs.isEmpty()) {
            throw new NoUniqueBeanDefinitionException(String.format("找到多个 '%s' 类型的Bean，但是未找到有@Praimary注解的。", type.getName()));
        } else {
            //@Primary不唯一
            throw new NoUniqueBeanDefinitionException(String.format("找到多个 '%s' 类型的Bean，并且找到多个@Praimary注解。", type.getName()));
        }
    }

    /**
     * 根据Type查找若干个BeanDefinition，返回0个或多个
     *
     * @param type 想要找的类型
     * @return BeanDefinition列表
     */
    public List<BeanDefinition> findBeanDefinitions(Class<?> type) {
        return this.beans.values().stream().filter(def -> type.isAssignableFrom(def.getBeanClass())).sorted().collect(Collectors.toList());
    }


    @Override
    public boolean containsBean(String name) {
        return this.beans.containsKey(name);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getBean(String name) {
        BeanDefinition def = this.beans.get(name);
        if (def == null) {
            throw new NoSuchBeanDefinitionException(String.format("没找到bean '%s'!", name));
        }
        return (T) def.getRequiredInstance();
    }

    @Override
    public <T> T getBean(String name, Class<T> requiredType) {
        T t = findBean(name, requiredType);
        if (t == null) {
            throw new NoSuchBeanDefinitionException(String.format("没找到名字为 '%s' 类型为 '%s' 的bean!", name, requiredType));
        }
        return t;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> requiredType) {
        BeanDefinition def = findBeanDefinition(requiredType);
        if (def == null) {
            throw new NoSuchBeanDefinitionException(String.format("没找到类型为 '%s' 的bean!", requiredType));
        }
        return (T) def.getRequiredInstance();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> getBeans(Class<T> requiredType) {
        List<BeanDefinition> beanDefinitions = this.findBeanDefinitions(requiredType);
        return (List<T>) beanDefinitions.stream()
                .map(BeanDefinition::getInstance)
                .collect(Collectors.toList());
    }
    // findXxx与getXxx类似，但不存在时返回null

    @Nullable
    @SuppressWarnings("unchecked")
    protected <T> T findBean(String name, Class<T> requiredType) {
        BeanDefinition def = findBeanDefinition(name, requiredType);
        if (def == null) {
            return null;
        }
        return (T) def.getRequiredInstance();
    }

    @Nullable
    @SuppressWarnings("unchecked")
    protected <T> T findBean(Class<T> requiredType) {
        BeanDefinition def = findBeanDefinition(requiredType);
        if (def == null) {
            return null;
        }
        return (T) def.getRequiredInstance();
    }

    @Nullable
    @SuppressWarnings("unchecked")
    protected <T> List<T> findBeans(Class<T> requiredType) {
        return findBeanDefinitions(requiredType).stream().map(def -> (T) def.getRequiredInstance()).collect(Collectors.toList());
    }

    @Override
    public void close() {
        logger.debug("IOC容器开始关闭，开始执行bean的destroy方法进行销毁...");
        this.beans.values().forEach(beanDefinition -> {
            Object instance = getProxiedInstance(beanDefinition);
            callDestroyMethod(beanDefinition, instance);
        });
        this.beans.clear();
        logger.debug("IOC容器已经关闭！");
        ApplicationContextUtils.setApplicationContext(null);
    }

    private void callDestroyMethod(BeanDefinition def, Object instance) {
        String destroyMethodName = def.destroyMethodName;
        Method destroyMethod = def.destroyMethod;
        if (!Objects.isNull(destroyMethod)) {
            try {
                destroyMethod.invoke(instance);
                logger.debug("bean: '{}': {} 已经销毁!", def.getName(), def.getBeanClass().getName());
            } catch (Exception e) {
                throw new BeanCreationException(e);
            }
        } else if (!Objects.isNull(destroyMethodName)) {
            String factoryName = def.getFactoryName();
            BeanDefinition factoryDef = findBeanDefinition(factoryName);
            Method method = ClassUtils.getMethodByName(factoryDef.getBeanClass(), destroyMethodName);
            method.setAccessible(true);
            try {
                method.invoke(factoryDef.getInstance());
                logger.debug("bean: '{}': {} 已经销毁!", def.getName(), def.getBeanClass().getName());
            } catch (Exception e) {
                throw new BeanCreationException(e);
            }
        }
    }
}
