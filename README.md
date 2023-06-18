# 手写Spring
本仓库是以Spring框架为原型，专注于实现Spring的核心功能，编写一个迷你版的Spring框架，我们把它命名为Winter Framework，与Spring主要区别在于，它俩的名字有所不同，当然功能也是Spring的皮毛。

**Winter Framework设计目标如下：**
> * context模块：实现ApplicationContext容器与Bean的管理；
> * aop模块：实现AOP功能；
> * jdbc模块：实现JdbcTemplate，以及声明式事务管理；
> * web模块：实现Web MVC和REST API；
> * boot模块：实现一个简化版的“Spring Boot”，用于打包运行。
> * 剩下的将进一步完善...

# 实现IOC容器功能
Spring的核心就是能管理一组Bean，并能自动配置依赖关系的IoC容器。而我们的**Winter Framework**的核心**context模块**就是要实现IoC容器。

---
**设计目标**
Spring的IoC容器分为两类：BeanFactory和ApplicationContext，前者总是延迟创建Bean，而后者则在启动时初始化所有Bean。实际使用时，99%都采用ApplicationContext，因此，Summer Framework仅实现ApplicationContext，不支持BeanFactory。

早期的Spring容器采用XML来配置Bean，后期又加入了自动扫描包的功能，即通过`<context:component-scan base-package="org.example"/>`的配置。再后来，又加入了Annotation配置，并通过`@ComponentScan`注解实现自动扫描。如果使用Spring Boot，则99%都采用`@ComponentScan`注解方式配置，因此，Summer Framework也仅实现Annotation配置+`@ComponentScan`扫描方式完成容器的配置。

此外，Summer Framework仅支持Singleton类型的Bean，不支持Prototype类型的Bean，因为实际使用中，99%都采用Singleton。依赖注入则与Spring保持一致，支持构造方法、Setter方法与字段注入。支持`@Configuration`和`BeanPostProcessor`。至于Spring的其他功能，例如，层级容器、MessageSource、一个Bean允许多个名字等功能，一概不支持！

下表列出了Spring Framework和Winter Framework在IoC容器方面的异同：

| 功能     | 	Spring Framework                 | 	Summer Framework      |
|:-------|:----------------------------------|:-----------------------|
| IoC容器  | 	支持BeanFactory和ApplicationContext | 	仅支持ApplicationContext |
| 配置方式   | 	支持XML与Annotation                 | 	仅支持Annotation         |
| 扫描方式   | 	支持按包名扫描                          | 	支持按包名扫描               |
| Bean类型 | 	支持Singleton和Prototype            | 	仅支持Singleton          |
| Bean工厂 | 	支持FactoryBean和@Bean注解            | 	仅支持@Bean注解            |
| 定制Bean | 	支持BeanPostProcessor              | 	支持BeanPostProcessor   |
| 依赖注入   | 	支持构造方法、Setter方法与字段               | 	支持构造方法、Setter方法与字段    |
| 多容器    | 	支持父子容器                           | 	不支持                   |

**Annotation配置**
从使用者的角度看，使用IoC容器时，需要定义一个入口配置，它通常长这样：
```java
@ComponentScan
public class AppConfig {
}
```
`AppConfig`只是一个配置类，它的目的是通过`@ComponentScan`来标识要扫描的Bean的包。如果没有明确写出包名，那么将基于`AppConfig`所在包进行扫描，如果明确写出了包名，则在指定的包下进行扫描。

在扫描过程中，凡是带有注解`@Component`的类，将被添加到IoC容器进行管理：
```java
@Component
public class UserService {
}
``` 
我们用到的许多第三方组件也经常会纳入IoC容器管理。这些第三方组件是不可能带有`@Component`注解的，引入第三方`Bean`只能通过工厂模式，即在`@Configuration`工厂类中定义带`@Bean`的工厂方法：
```java
@Configuration
public class DbConfig {
    @Bean
    DataSource createDataSource(...) {
        return new HikariDataSource(...);
    }

    @Bean
    JdbcTemplate createJdbcTemplate(...) {
        return new JdbcTemplate(...);
    }
}
```
基于Annotation配置的IoC容器基本用法就是上面所述。下面，我们就一步一步来实现IoC容器。

## 1. 实现ResourceResolver
在编写IoC容器之前，我们首先要实现`@ComponentScan`，即解决“在指定包下扫描所有Class”的问题。

Java的ClassLoader机制可以在指定的Classpath中根据类名加载指定的Class，但遗憾的是，给出一个包名，例如，`org.example`，它并不能获取到该包下的所有Class，也不能获取子包。要在Classpath中扫描指定包名下的所有Class，包括子包，实际上是在Classpath中搜索所有文件，找出文件名匹配的`.class`文件。例如，Classpath中搜索的文件`org/example/Hello.class`就符合包名`org.example`，我们需要根据文件路径把它变为`org.example.Hello`，就相当于获得了类名。因此，搜索Class变成了搜索文件。

我们先定义一个`Resource`类型表示文件：
```java
public record Resource(String path, String name) {
}
```
> `Resource` 是一个 Java 类，它包含了两个字段 `path` 和 `name`。它使用 Java 14 中的 `public record` 约束来声明这个类。
> 
>具体来说，`public record` 是 Java 14 中的一种新特性，它是一种轻量级的数据传输类，可以处理数据的传递、透明的调用等问题。在这里，`Resource` 类使用了 `public record` 约束来约束自己，表示这是一个描述资源的数据类，同时可以自动生成构造器、getter、equals、hashCode 等方法。
> 
>`path` 字段表示资源的路径，`name` 字段表示资源的名称。
>
> 举个例子，如果你需要在代码中处理一些资源文件，你可以定义一个 `Resource` 类来描述这些资源文件的名称和路径，方便程序的处理：
>
> ```java
> public record Resource(String path, String name) {
>     // 可以添加其他方法和字段
> }
> ```
>
> 当你实例化 `Resource` 类时，可以像下面这样创建一个对象：
>
> ```java
> Resource resource = new Resource("/path/to/resource", "resource_name");
> ```
> 
> 这样一来，你就可以方便地使用 `resource.path()` 或 `resource.name()` 方法来获取该资源对象的路径或名称了。 

再仿造Spring提供一个`ResourceResolver`，定义`scan()`方法来获取扫描到的`Resource`：
```java
public class ResourceResolver {
    String basePackage;

    public ResourceResolver(String basePackage) {
        this.basePackage = basePackage;
    }

    public <R> List<R> scan(Function<Resource, R> mapper) {
        ...
    }
}
```
这样，我们就可以扫描指定包下的所有文件。有的同学会问，我们的目的是扫描`.class`文件，如何过滤出Class？

注意到`scan()`方法传入了一个映射函数，我们传入`Resource`到Class Name的映射，就可以扫描出Class Name：
```java
// 定义一个扫描器:
ResourceResolver rr = new ResourceResolver("org.example");
List<String> classList = rr.scan(res -> {
   String name = res.name(); // 资源名称"org/example/Hello.class"
   if (name.endsWith(".class")) { // 如果以.class结尾
    // 把"org/example/Hello.class"变为"org.example.Hello":
    return name.substring(0, name.length() - 6).replace("/", ".").replace("\\", ".");
   }
   // 否则返回null表示不是有效的Class Name:
   return null;
});
```
这样，`ResourceResolver`只负责扫描并列出所有文件，由客户端决定是找出`.class`文件，还是找出`.properties`文件。

在ClassPath中扫描文件的代码是固定模式，可以在网上搜索获得，例如StackOverflow的这个回答。这里要注意的一点是，Java支持在jar包中搜索文件，所以，不但需要在普通目录中搜索，也需要在Classpath中列出的jar包中搜索，核心代码如下：
```java
// 通过ClassLoader获取URL列表:
Enumeration<URL> en = getContextClassLoader().getResources("org/example");
while (en.hasMoreElements()) {
    URL url = en.nextElement();
    URI uri = url.toURI();
    if (uri.toString().startsWith("file:")) {
        // 在目录中搜索
    }
    if (uri.toString().startsWith("jar:")) {
        // 在Jar包中搜索
    }
}
```

几个要点：

1. ClassLoader首先从`Thread.getContextClassLoader()`获取，如果获取不到，再从当前Class获取，因为Web应用的ClassLoader不是JVM提供的基于Classpath的ClassLoader，而是Servlet容器提供的ClassLoader，它不在默认的Classpath搜索，而是在`/WEB-INF/classes`目录和`/WEB-INF/lib`的所有jar包搜索，从`Thread.getContextClassLoader()`可以获取到Servlet容器专属的`ClassLoader`；

2. Windows和Linux/macOS的路径分隔符不同，前者是`\`，后者是`/`，需要正确处理；

3. 扫描目录时，返回的路径可能是`abc/xyz`，也可能是`abc/xyz/`，需要注意处理末尾的`/`。

这样我们就完成了能扫描指定包以及子包下所有文件的`ResourceResolver`。

## 2. 实现PropertyResolver
Spring的注入分为`@Autowired`和`@Value`两种。对于`@Autowired`，涉及到Bean的依赖，而对于`@Value`，则仅仅是将对应的配置注入，不涉及Bean的依赖，相对比较简单。为了注入配置，我们用`PropertyResolver`保存所有配置项，对外提供查询功能。

本节我们来实现`PropertyResolver`，它支持3种查询方式：
1. 按配置的key查询，例如：`getProperty("app.title")`;
2. 以`${abc.xyz}`形式的查询，例如，`getProperty("${app.title}")`，常用于`@Value("${app.title}")`注入；
3. 带默认值的，以`${abc.xyz:defaultValue}`形式的查询，例如，`getProperty("${app.title:Winter}")`，常用于`@Value("${app.title:Winter}")`注入。

Java本身提供了按key-value查询的`Properties`，我们先传入`Properties`，内部按key-value存储：
```java
public class PropertyResolver {

    Map<String, String> properties = new HashMap<>();

    public PropertyResolver(Properties props) {
        //存入环境变量
        this.properties.putAll(System.getenv());
        //获取props的key集合
        Set<String> names = props.stringPropertyNames();
        names.forEach(name -> this.properties.put(name, props.getProperty(name)));
    }
}
```
这样，我们在`PropertyResolver`内部，通过一个`Map<String, String>`存储了所有的配置项，包括环境变量。对于按key查询的功能，我们可以简单实现如下：
```java
@Nullable
public String getProperty(String key) {
    return this.properties.get(key);
}
```
下一步，我们准备解析`${abc.xyz:defaultValue}`这样的key，先定义一个`PropertyExpr`，把解析后的key和defaultValue存储起来：
```java
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
    public String toString() {}
}
```
然后按`${...}`解析：
```java
    /**
     * 解析key是否是`${}`这种格式的
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
```
我们把`getProperty()`改造一下，即可实现查询`${abc.xyz:defaultValue}`：
```java
 /**
     * 通过key获取value
     * 支持通过${database:${db:redis}}进行嵌套，冒号后为默认值
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
        if ( s != null) {
            return s;
        }
        if (defaultValue != null) {
            if (defaultValue.startsWith("${") && defaultValue.endsWith("}")){
                return getProperty(parse.getDefaultValue());
            }
            return defaultValue;
        }
        return null;
    }
```
每次查询到value后，我们递归调用`getProperty()`，这样就可以支持嵌套的key，例如：
```java
${app.title:${APP_NAME:Winter}}
```
这样可以先查询`app.title`，没有找到就再查询`APP_NAME`，还没有找到就返回默认值`Winter`。

注意到Spring的`${...}`表达式实际上可以做到组合，例如：
```java
jdbc.url=jdbc:mysql//${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME}
```
而我们实现的`${...}`表达式只能嵌套，不能组合，因为要实现Spring的表达式，需要编写一个完整的能解析表达式的复杂功能，而不能仅仅依靠判断${开头、}结尾。由于解析表达式的功能过于复杂，因此我们决定不予支持。

Spring还支持更复杂的`#{...}`表达式，它可以引用Bean、调用方法、计算等：
```java
#{appBean.version() + 1}
```
为此Spring专门提供了一个`spring-expression`库来支持这种更复杂的功能。按照一切从简的原则，我们不支持`#{...}`表达式。
**实现类型转换**
除了String类型外，`@Value`注入时，还允许`boolean`、`int`、`Long`等基本类型和包装类型。此外，Spring还支持`Date`、`Duration`等类型的注入。我们既要实现类型转换，又不能写死，否则，将来支持新的类型时就要改代码。

我们先写类型转换的入口查询：
```java
@Nullable
public <T> T getProperty(String key, Class<T> targetType) {
    String value = getProperty(key);
    if (value == null) {
        return null;
    }
    // 转换为指定类型:
    return convert(targetType, value);
}
```
再考虑如何实现`convert()`方法。对于类型转换，实际上是从`String`转换为指定类型，因此，用函数式接口`Function<String, Object>`就很合适：
```java
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
        throw new ClassCastException("类型转换失败：java.lang.String====>" + targetType.getName());
    }
    return target;
}
```
这样我们就已经实现了类型转换，下一步是把各种要转换的类型放到Map里。在构造方法中，我们放入常用的基本类型转换器：
```java
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
```
再加一个`registerConverter()`接口，我们就可以对外提供扩展，让用户自己编写自己定制的Converter。

```java
public void registerConverter(Map<Class<?>, Function<String, Object>> newConverters){
    this.converters.putAll(newConverters);
}
```

这样一来，我们的PropertyResolver就准备就绪，读取配置的初始化代码如下：
```java
// Java标准库读取properties文件:
Properties props = new Properties();
// 读取类路径下的application.properties配置文件
props.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("application.properties"));
// 构造PropertyResolver
propertyResolver = new PropertyResolver(props);
// 获取配置信息
LocalDate localDate = propertyResolver.getProperty("${localDate:2023-06-18}", LocalDate.class);
```

