# 手写Spring
本仓库是以Spring框架为原型，专注于实现Spring的核心功能，编写一个迷你版的Spring框架，我们把它命名为Winter Framework，与Spring主要区别在于，它俩的名字有所不同，当然功能也是Spring的皮毛。

**Winter Framework设计目标如下：**
> * context模块：实现ApplicationContext容器与Bean的管理；
> * aop模块：实现AOP功能；
> * jdbc模块：实现JdbcTemplate，以及声明式事务管理；
> * web模块：实现Web MVC和REST API；
> * boot模块：实现一个简化版的“Spring Boot”，用于打包运行。
> * 剩下的将进一步完善...

# 实现IoC容器功能
Spring的核心就是能管理一组Bean，并能自动配置依赖关系的IoC容器。而我们的**Winter Framework**的核心**context模块**就是要实现IoC容器。

---
**设计目标**

Spring的IoC容器分为两类：BeanFactory和ApplicationContext，前者总是延迟创建Bean，而后者则在启动时初始化所有Bean。实际使用时，99%都采用ApplicationContext，因此，Winter Framework仅实现ApplicationContext，不支持BeanFactory。

早期的Spring容器采用XML来配置Bean，后期又加入了自动扫描包的功能，即通过`<context:component-scan base-package="org.example"/>`的配置。再后来，又加入了Annotation配置，并通过`@ComponentScan`注解实现自动扫描。如果使用Spring Boot，则99%都采用`@ComponentScan`注解方式配置，因此，Winter Framework也仅实现Annotation配置+`@ComponentScan`扫描方式完成容器的配置。

此外，Winter Framework仅支持Singleton类型的Bean，不支持Prototype类型的Bean，因为实际使用中，99%都采用Singleton。依赖注入则与Spring保持一致，支持构造方法、Setter方法与字段注入。支持`@Configuration`和`BeanPostProcessor`。至于Spring的其他功能，例如，层级容器、MessageSource、一个Bean允许多个名字等功能，一概不支持！

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
这样，我们就可以扫描指定包下的所有文件。我们的目的是扫描`.class`文件，如何过滤出Class？

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

**使用YAML配置**
Spring Framework并不支持YAML配置，但Spring Boot支持。因为`yaml`配置比`.properties`要方便，所以我们把对YAML的支持也集成进来。

首先引入依赖`org.yaml:snakeyaml:2.0`，然后我们写一个`YamlUtils`，通过`loadYamlAsPlainMap()`方法读取一个YAML文件，并返回`Map`：
```java
public class YamlUtils {
    public static Map<String, Object> loadYamlAsPlainMap(String path) {
        return ...
    }
}
```
我们把YAML格式：
```yaml
server:
  port: 8080
kele:
  lover:
    name:
      - 可乐
      - 雪碧
      - 芬达
```
读取为`Map`，其中，每个key都是完整路径，相当于把它变为`.properties`格式：
```properties
server.port=8080
kele.lover.name[0]=可乐
kele.lover.name[1]=雪碧
kele.lover.name[2]=芬达
```
这样我们无需改动`PropertyResolver`的代码，使用YAML时，可以按如下方式读取配置：
```java
Map<String, Object> map = YamlUtils.loadYamlAsPlainMap("application.yaml");
Properties properties = new Properties();
properties.putAll(map);
PropertyResolver propertyResolver = new PropertyResolver(properties);
LocalDateTime localDateTime = propertyResolver.getProperty("web.localDateTime", LocalDateTime.class);
System.out.println(localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
```

> 读取YAML的代码比较简单，注意要点如下：
> 1. SnakeYaml默认读出的结构是树形结构，需要“拍平”成abc.xyz格式的key；
> 2. SnakeYaml默认会自动转换int、boolean等value，需要禁用自动转换，把所有value均按String类型返回。

## 3. 创建BeanDefinition
现在，我们可以用`ResourceResolver`扫描`Class`，用`PropertyResolver`获取配置，下面，我们开始实现IoC容器。

在IoC容器中，每个Bean都有一个唯一的名字标识。Spring还允许为一个Bean定义多个名字，这里我们简化一下，一个Bean只允许一个名字，因此，很容易想到用一个`Map<String, Object>`保存所有的Bean：
```java
public class AnnotationConfigApplicationContext {
    Map<String, Object> beans;
}
```
这么做不是不可以，但是丢失了大量Bean的定义信息，不便于我们创建Bean以及解析依赖关系。合理的方式是先定义`BeanDefinition`，它能从`Annotation`中提取到足够的信息，便于后续创建Bean、设置依赖、调用初始化方法等：
```java
public class BeanDefinition implements Comparable<BeanDefinition>{
    // 全局唯一的Bean Name:
    String name;
    // Bean的声明类型:
    Class<?> beanClass;
    // Bean的实例:
    Object instance = null;
    // 构造方法/null:
    Constructor<?> constructor;
    // 工厂方法名称/null:
    String factoryName;
    // 工厂方法/null:
    Method factoryMethod;
    // Bean的顺序:
    int order;
    // 是否标识@Primary:
    boolean primary;
    // init/destroy方法名称:
    String initMethodName;
    String destroyMethodName;
    // init/destroy方法:
    Method initMethod;
    Method destroyMethod;
}
```
> BeanDefinition实现Comparable接口重写compareTo方法来实现对bean进行排序
> ```java
>    @Override
>    public int compareTo(BeanDefinition def) {
>        int cmp = Integer.compare(this.order, def.order);
>        if (cmp != 0) {
>            return cmp;
>        }
>        return this.name.compareTo(def.name);
>    }
> ```
> 根据给定的 BeanDefinition 对象的优先级（order）和名称（name）进行排序。
> 首先，它比较两个 BeanDefinition 对象的优先级（order）。如果两个对象的优先级不相等，则返回它们之间的比较结果。这意味着具有较小优先级的 BeanDefinition 对象将在排序后的列表中排在前面。
> 如果两个对象的优先级相等，则根据它们的名称（name）进行比较。使用字符串的自然排序来确定它们在排序后的列表中的位置。
> 
> 通过这种方式，可以将 BeanDefinition 对象按照优先级和名称进行排序，以满足特定的排序需求。

对于自己定义的带`@Component`注解的Bean，我们需要获取Class类型，获取构造方法来创建Bean，然后收集`@PostConstruct`和`@PreDestroy`标注的初始化与销毁的方法，以及其他信息，如`@Order`定义Bean的内部排序顺序，`@Primary`定义存在多个相同类型时返回哪个“主要”Bean。一个典型的定义如下：
```java
@Component
public class Hello {
    @PostConstruct
    void init() {}

    @PreDestroy
    void destroy() {}
}
```
对于`@Configuration`定义的`@Bean`方法，我们把它看作Bean的工厂方法，我们需要获取方法返回值作为Class类型，方法本身作为创建Bean的`factoryMethod`，然后收集`@Bean`定义的`initMethod`和`destroyMethod`标识的初始化于销毁的方法名，以及其他`@Order`、`@Primary`等信息。一个典型的定义如下：
```java
@Configuration
public class AppConfig {
    @Bean(initMethod="init", destroyMethod="close")
    DataSource createDataSource() {
        return new HikariDataSource(...);
    }
}
```
**Bean的声明类型**
这里我们要特别注意一点，就是Bean的声明类型。对于`@Component`定义的Bean，它的声明类型就是其Class本身。然而，对于用`@Bean`工厂方法创建的Bean，它的声明类型与实际类型不一定是同一类型。上述`createDataSource()`定义的Bean，声明类型是`DataSource`，实际类型却是某个子类，例如`HikariDataSource`，因此要特别注意，我们在`BeanDefinition`中，存储的`beanClass`是声明类型，实际类型不必存储，因为可以通过`instance.getClass()`获得.
```java
public class BeanDefinition {
    // Bean的声明类型:
    Class<?> beanClass;

    // Bean的实例:
    Object instance = null;
}
```
这也引出了下一个问题：如果我们按照名字查找Bean或BeanDefinition，要么拿到唯一实例，要么不存在，即通过查询`Map<String, BeanDefinition>`即可完成：
```java
public class AnnotationConfigApplicationContext {
    Map<String, BeanDefinition> beans;

    // 根据Name查找BeanDefinition，如果Name不存在，返回null
    @Nullable
    public BeanDefinition findBeanDefinition(String name) {
        return this.beans.get(name);
    }
}
```
但是通过类型查找Bean或BeanDefinition，我们没法定义一个`Map<Class, BeanDefinition>`，原因就是Bean的声明类型与实际类型不一定相符，举个例子：
```java
@Configuration
public class AppConfig {
    @Bean
    AtomicInteger counter() {
        return new AtomicInteger();
    }
    
    @Bean
    Number bigInt() {
        return new BigInteger("1000000000");
    }
}
```
当我们调用`getBean(AtomicInteger.class)`时，我们会获得`counter()`方法创建的唯一实例，但是，当我们调用`getBean(Number.class)`时，`counter()`方法和`bigInt()`方法创建的实例均符合要求，此时，如果有且仅有一个标注了`@Primary`，就返回标注了`@Primary`的Bean，否则，直接报`NoUniqueBeanDefinitionException`错误。

因此，对于`getBean(Class)`方法，必须遍历找出所有符合类型的Bean，如果不唯一，再判断`@Primary`，才能返回唯一Bean或报错。

我们编写一个找出所有类型的`findBeanDefinitions(Class)`方法如下：
```java
    /**
    * 根据Type查找若干个BeanDefinition，返回0个或多个
    *
    * @param type 想要找的类型
    * @return BeanDefinition列表
    */
    List<BeanDefinition> findBeanDefinitions(Class<?> type) {
        return this.beans.values().stream()
        .filter(def -> type.isAssignableFrom(def.getClass()))
        .sorted()
        .collect(Collectors.toList());
    }
```
我们再编写一个`findBeanDefinition(Class)`方法如下：
```java
    /**
     * 根据Type查找1个BeanDefinition，返回null或1个
     *
     * @param type 想要找的类型
     * @return BeanDefinition
     */
    @Nullable
    BeanDefinition findBeanDefinition(Class<?> type) {
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
        //如果流中没有任何匹配的元素，返回的结果将是一个空集合（empty list），而不是 null。
        List<BeanDefinition> primaryDefs = defs.stream()
                .filter(BeanDefinition::isPrimary)
                .collect(Collectors.toList());
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
```
现在，我们已经定义好了数据结构，下面开始获取所有BeanDefinition信息，实际分两步：
```java
public class AnnotationConfigApplicationContext {
    Map<String, BeanDefinition> beans;

    public AnnotationConfigApplicationContext(Class<?> configClass, PropertyResolver propertyResolver) {
        // 扫描获取所有Bean的Class类型:
        Set<String> beanClassNames = scanForClassNames(configClass);

        // 扫描所有字节码中的注解，创建BeanDefinition:
        this.beans = createBeanDefinitions(beanClassNames);
    }
    ...
}
```
第一步是扫描指定包下的所有Class，然后返回Class名字，这一步比较简单：
```java
    /**
     * 通过扫描配置类，获取所有的字节码类名
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
                classNameList.forEach((className) -> {
                    logger.debug("通过ComponentScan注解扫描到字节码: {}", className);
                });
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
```
注意到扫描结果是指定包的所有Class名称，以及通过`@Import`导入的Class名称，下一步才会真正处理各种注解：
```java
    /**
     * 扫描@Component、@Bean、@Configuration注解创建BeanDefinition
     * @param classNames
     * @return
     */
    private Map<String, BeanDefinition> createBeanDefinitions(Set<String> classNames) {
        Map<String, BeanDefinition> beanDefinitionsMap = new HashMap<>();
        for (String className : classNames) {
            //1.获取Class
            Class<?> clazz = null;
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
```
上述代码需要注意的一点是，查找`@Component`时，并不是简单地在Class定义查看`@Component`注解，因为Spring的`@Component`是可以扩展的，例如，标记为`@Controller`的Class也符合要求：
```java
@Controller
public class MvcController {...}
```
原因就在于，`@Controller`注解的定义包含了`@Component`：
```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Controller {
    String value() default "";
}
```
所以，判断是否存在`@Component`，不但要在当前类查找`@Component`，还要在当前类的所有注解上，查找该注解是否有`@Component`，因此，我们编写了一个能递归查找注解的方法：
```java
    /**
     * 递归查找Annotation
     *
     * @param target          要查找的类
     * @param annotationClass 匹配的注解
     * @param <A>
     * @return 找到的注解
     */
    @Nullable
    public static <A extends Annotation> A findAnnotation(Class<?> target, Class<A> annotationClass) {
        A annotation = target.getAnnotation(annotationClass);
        for (Annotation anno : target.getDeclaredAnnotations()) {
            Class<? extends Annotation> annoType = anno.annotationType();
            if (!"java.lang.annotation".equals(annoType.getPackage().getName())) {
                A a = findAnnotation(annoType, annotationClass);
                if (a != null) {
                    if (annotation != null) {
                        throw new BeanDefinitionException(String.format("类：'%s' 上有重复的 '%s' 注解！", target.getName(), annotationClass.getName()));
                    }
                    annotation = a;
                }
            }
        }
        return annotation;
    }
```
带有`@Configuration`注解的Class，视为Bean的工厂，我们需要继续在`scanFactoryMethods()`中查找`@Bean`标注的方法：
```java
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
```
注意到`@Configuration`注解本身又用`@Component`注解修饰了，因此，对于一个`@Configuration`来说：
```java
@Configuration
public class DateTimeConfig {
    @Bean
    LocalDateTime local() { return LocalDateTime.now(); }

    @Bean
    ZonedDateTime zoned() { return ZonedDateTime.now(); }
}
```
实际上创建了3个`BeanDefinition`：

* DateTimeConfig本身
* LocalDateTime
* ZonedDateTime

不创建`DateTimeConfig`行不行？不行，因为后续没有`DateTimeConfig`的实例，无法调用`local()`和`zoned()`
方法。因为当前我们只创建了`BeanDefinition`，所以对于`LocalDateTime`和`ZonedDateTime`的`BeanDefinition`
来说，还必须保存`DateTimeConfig`的名字，将来才能通过名字查找`DateTimeConfig`的实例。

我们同时存储了`initMethodName`和`initMethod`，以及`destroyMethodName`和`destroyMethod`，这是因为在`@Component`声明的`Bean`
中，我们可以根据`@PostConstruct`和`@PreDestroy`直接拿到`Method`本身，而在`@Bean`声明的`Bean`中，我们拿不到`Method`
，只能从`@Bean`注解提取出字符串格式的方法名称，因此，存储在`BeanDefinition`的方法名称与方法，其中总有一个为null。

最后，仔细编写`BeanDefinition`的`toString()`方法，使之能打印出详细的信息。我们编写测试，运行，打印出每个`BeanDefinition`如下：

```java
16:10:29.552[main]DEBUG top.kelecc.context.AnnotationConfigApplicationContext-定义了bean:BeanDefinition[name=dateTimeConfig,beanClass=top.kelecc.config.DateTimeConfig,factory=null,init-method=null,destroy-method=null,primary=false,instance=null]
16:10:29.552[main]DEBUG top.kelecc.context.AnnotationConfigApplicationContext-定义了bean:BeanDefinition[name=local,beanClass=java.time.LocalDateTime,factory=DateTimeConfig.local(),init-method=null,destroy-method=null,primary=false,instance=null]
16:10:29.554[main]DEBUG top.kelecc.context.AnnotationConfigApplicationContext-定义了bean:BeanDefinition[name=zoned,beanClass=java.time.ZonedDateTime,factory=DateTimeConfig.zoned(),init-method=null,destroy-method=null,primary=false,instance=null]
```

现在，我们已经能扫描并创建所有的`BeanDefinition`，只是目前每个`BeanDefinition`内部的`instance`还是`null`
，因为我们后续才会创建真正的Bean。

## 4. 创建Bean实例

当我们拿到所有`BeanDefinition`之后，就可以开始创建Bean的实例了。

在创建Bean实例之前，我们先看看Spring支持的4种依赖注入模式：

1.构造方法注入，例如：

```java

@Component
public class Hello {
    JdbcTemplate jdbcTemplate;

    public Hello(@Autowired JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
}
```

2.工厂方法注入，例如：

```java

@Configuration
public class AppConfig {
    @Bean
    Hello hello(@Autowired JdbcTemplate jdbcTemplate) {
        return new Hello(jdbcTemplate);
    }
}
```

3.Setter方法注入，例如：

```java

@Component
public class Hello {
    JdbcTemplate jdbcTemplate;

    @Autowired
    void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
}
```

4.字段注入，例如：

```java

@Component
public class Hello {
    @Autowired
    JdbcTemplate jdbcTemplate;
}
```

然而我们仔细分析，发现这4种注入方式实际上是有区别的。

区别就在于，前两种方式，即构造方法注入和工厂方法注入，Bean的创建与注入是一体的，我们无法把它们分成两个阶段，因为无法中断方法内部代码的执行。而后两种方式，即Setter方法注入和属性注入，Bean的创建与注入是可以分开的，即先创建Bean实例，再用反射调用方法或字段，完成注入。

我们再分析一下循环依赖的问题。循环依赖，即A、B互相依赖，或者A依赖B，B依赖C，C依赖A，形成了一个闭环。IoC容器对Bean进行管理，可以解决部分循环依赖问题，但不是所有循环依赖都能解决。

我们先来看不能解决的循环依赖问题，假定下列代码定义的A、B两个Bean

```java
class A {
    final B b;

    A(B b) {
        this.b = b;
    }
}

class B {
    final A a;

    B(A a) {
        this.a = a;
    }
}
```

这种通过构造方法注入依赖的两个Bean，如果存在循环依赖，是无解的，因为我们不用IoC，自己写Java代码也写不出正确创建两个Bean实例的代码。

因此，我们把构造方法注入和工厂方法注入的依赖称为强依赖，不能有强依赖的循环依赖，否则只能报错。

后两种注入方式形成的依赖则是弱依赖，假定下列代码定义的A、B两个Bean：

```java
class A {
    B b;
}

class B {
    A a;
}
```

这种循环依赖则很容易解决，因为我们可以分两步，先分别实例化Bean，再注入依赖：

```java
// 第一步,实例化:
A a=new A();
B b=new B();
// 第二步,注入:
a.b=b;
b.a=a;
```

所以，对于IoC容器来说，创建Bean的过程分两步：

1. 创建Bean的实例，此时必须注入强依赖；
2. 对Bean实例进行Setter方法注入和字段注入。
   第一步如果遇到循环依赖则直接报错，第二步则不需要关心有没有循环依赖。

我们先实现第一步：创建Bean的实例，同时注入强依赖。

在上一节代码中，我们已经获得了所有的`BeanDefinition`：

```java
public class AnnotationConfigApplicationContext {
    PropertyResolver propertyResolver;
    Map<String, BeanDefinition> beans;

    public AnnotationConfigApplicationContext(Class<?> configClass, PropertyResolver propertyResolver) {
        this.propertyResolver = propertyResolver;
        // 扫描获取所有Bean的Class类型:
        Set<String> beanClassNames = scanForClassNames(configClass);
        // 创建Bean的定义:
        this.beans = createBeanDefinitions(beanClassNames);
    }
}
```

下一步是创建Bean的实例，同时注入强依赖。此阶段必须检测循环依赖。检测循环依赖其实非常简单，就是定义一个`Set<String>`
跟踪当前正在创建的所有Bean的名称：

```java
public class AnnotationConfigApplicationContext {
    Set<String> creatingBeanNames;
    ...
}
```

创建Bean实例我们用方法`createBeanAsEarlySingleton()`实现，在方法开始处检测循环依赖：

```java
// 创建一个Bean，但不进行字段和方法级别的注入。如果创建的Bean不是Configuration，则在构造方法/工厂方法中注入的依赖Bean会自动创建
public Object createBeanAsEarlySingleton(BeanDefinition def){
        if(!this.creatingBeanNames.add(def.getName())){
            // 检测到重复创建Bean导致的循环依赖:
            throw new UnsatisfiedDependencyException();
        }
        ...
}
```

由于`@Configuration`标识的Bean实际上是工厂，它们必须先实例化，才能实例化其他普通Bean，所以我们先把`@Configuration`
标识的Bean创建出来，再创建普通Bean：

```java
public AnnotationConfigApplicationContext(Class<?> configClass,PropertyResolver propertyResolver){
        this.propertyResolver=propertyResolver;
        //1.扫描获取所有的class类型
        Set<String> classNames=scanForClassNames(configClass);
        //2.扫描所有字节码中的注解，创建BeanDefinition
        this.beans=createBeanDefinitions(classNames);
        this.creatingBeanNames=new HashSet<>();
        //3.创建@Configuration的工厂类,由于@Configuration标识的Bean实际上是工厂，它们必须先实例化，才能实例化其他普通Bean，所以我们先把@Configuration标识的Bean创建出来，再创建普通Bean。
        createConfigurationBean();
        //4.创建BeanPostProcessor
        createBeanPostProcessor();
        //5.创建普通bean
        createNormalBeans();
}
/**
 * 创建@Configuration的工厂类
 */
private void createConfigurationBean(){
        this.beans.values().stream()
        .filter(this::isConfigurationDefinition)
        .sorted()
        .forEach(this::createBeanAsEarlySingleton);
}

/**
 * 创建BeanPostProcessor
 */
private void createBeanPostProcessor(){
        List<BeanPostProcessor> collect=this.beans.values().stream()
        .filter(this::isBeanPostProcessor)
        .sorted()
        .map(beanDefinition->(BeanPostProcessor)createBeanAsEarlySingleton(beanDefinition))
        .collect(Collectors.toList());
        this.beanPostProcessors.addAll(collect);
}

/**
 * 创建普通bean
 */
private void createNormalBeans(){
        this.beans.values().stream()
        .filter(def->Objects.isNull(def.getInstance()))
        .sorted()
        .forEach(def->{
            // 如果Bean未被创建(可能在其他Bean的构造方法注入前被创建):
            if(Objects.isNull(def.getInstance())){
                this.createBeanAsEarlySingleton(def);
            }
        });
}
```

剩下的工作就是把`createBeanAsEarlySingleton()`补充完整：

```java
    /**
     * 创建早期单例
     *
     * @param beanDefinition
     * @return
     */
    private Object createBeanAsEarlySingleton(BeanDefinition beanDefinition) {
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

        return instance;
    }
```

注意到递归调用：

```java
public Object createBeanAsEarlySingleton(BeanDefinition def){
        ...
        Object[]args=new Object[parameters.length];
        for(int i=0;i<parameters.length;i++){
            ...
            // 获取依赖Bean的实例:
            Object autowiredBeanInstance=dependsOnDef.getInstance();
            if(autowiredBeanInstance==null&&!isConfiguration){
                // 当前依赖Bean尚未初始化，递归调用初始化该依赖Bean:
                autowiredBeanInstance=createBeanAsEarlySingleton(dependsOnDef);
            }
            ...
        }
        ...
}
```

假设如下的Bean依赖：

```java

@Component
class A {
    // 依赖B,C:
    A(@Autowired B, @Autowired C) {
    }
}

@Component
class B {
    // 依赖C:
    B(@Autowired C) {
    }
}

@Component
class C {
    // 无依赖:
    C() {
    }
}
```

如果按照A、B、C的顺序创建Bean实例，那么系统流程如下：

1. 准备创建A；
2. 检测到依赖B：未就绪；
    1. 准备创建B：
    2. 检测到依赖C：未就绪；
        1. 准备创建C；
        2. 完成创建C；
    3. 完成创建B；
3. 检测到依赖C，已就绪；
4. 完成创建A。
   如果按照B、C、A的顺序创建Bean实例，那么系统流程如下：


1. 准备创建B；
2. 检测到依赖C：未就绪；
    1. 准备创建C；
    2. 完成创建C；
3. 完成创建B；
4. 准备创建A；
5. 检测到依赖B，已就绪；
6. 检测到依赖C，已就绪；
7. 完成创建A。
   可见无论以什么顺序创建，C总是最先被实例化，A总是最后被实例化。
## 5.初始化bean
在创建Bean实例的过程中，我们已经完成了强依赖的注入。下一步，是根据Setter方法和字段完成弱依赖注入，接着调用用`@PostConstruct`标注的init方法，就完成了所有Bean的初始化。

这一步相对比较简单，因为只涉及到查找依赖的`@Value`和`@Autowired`，然后用反射完成调用即可：
```java
    public AnnotationConfigApplicationContext(Class<?> configClass, PropertyResolver propertyResolver) {
        ...
        //6.通过set方法和字段注入
        logger.debug("================开始set方法和字段注入================");
        this.beans.values().forEach(this::injectBean);
        //7.调用所有bean的init方法
        logger.debug("================开始调用所有bean的init方法================");
        this.beans.values().stream().filter(beanDefinition -> !beanDefinition.isInit()).forEach(this::initBean);
        ...
    }
```
使用Setter方法和字段注入时，要注意一点，就是不仅要在当前类查找，还要在父类查找，因为有些`@Autowired`写在父类，所有子类都可使用，这样更方便。注入弱依赖代码如下：
```java
// 在当前类及父类进行字段和方法注入:
void injectProperties(BeanDefinition def, Class<?> clazz, Object bean) {
    // 在当前类查找Field和Method并注入:
    for (Field f : clazz.getDeclaredFields()) {
        tryInjectProperties(def, clazz, bean, f);
    }
    for (Method m : clazz.getDeclaredMethods()) {
        tryInjectProperties(def, clazz, bean, m);
    }
    // 在父类查找Field和Method并注入:
    Class<?> superClazz = clazz.getSuperclass();
    if (superClazz != null) {
        // 递归调用:
        injectProperties(def, superClazz, bean);
    }
}

// 注入单个属性
void tryInjectProperties(BeanDefinition def, Class<?> clazz, Object bean, AccessibleObject acc) {
    ...
}
```
弱依赖注入完成后，再循环一遍所有的`BeanDefinition`，对其调用`init`方法，完成最后一步初始化：
```java
    /**
     * 调用init方法
     *
     * @param beanDefinition
     */
    private void initBean(BeanDefinition beanDefinition) {
        Object instance = getProxyInstance(beanDefinition);
        callInitMethod(beanDefinition, instance);

        //调用BeanPostProcessor.postProcessAfterInitialization():
        beanPostProcessors.forEach(beanPostProcessor -> {
            //Todo BeanProcessor处理bean
        });
    }
```
处理`@PreDestroy`方法更简单，在`ApplicationContext`关闭时遍历所有`Bean`，调用`destroy`方法即可:
```java
    //ApplicationContext接口继承了AutoCloseable接口
    @Override
    public void close() {
        logger.debug("IoC容器关闭，开始执行bean的destroy方法进行销毁...");
        this.beans.values().forEach(beanDefinition -> {
            Object instance = getProxyInstance(beanDefinition);
            callDestroyMethod(beanDefinition, instance);
        });
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
```
## 6. 实现BeanPostProcessor
现在，我们已经完成了扫描Class名称、创建BeanDefinition、创建Bean实例、初始化Bean，理论上一个可用的IoC容器就已经就绪。

然而，`BeanPostProcessor`的出现改变了这一切。Spring允许用户自定义一种特殊的Bean，即实现了`BeanPostProcessor`接口，它有什么用呢？其实就是替换Bean。我们举个例子，下面的代码是基于Spring代码：
```java
@Configuration
@ComponentScan
public class AppConfig {

    public static void main(String[] args) {
        var ctx = new AnnotationConfigApplicationContext(AppConfig.class);
        // 可以获取到ZonedDateTime:
        ZonedDateTime dt = ctx.getBean(ZonedDateTime.class);
        System.out.println(dt);
        // 错误:NoSuchBeanDefinitionException:
        System.out.println(ctx.getBean(LocalDateTime.class));
    }

    // 创建LocalDateTime实例
    @Bean
    public LocalDateTime localDateTime() {
        return LocalDateTime.now();
    }

    // 实现一个BeanPostProcessor
    @Bean
    BeanPostProcessor replaceLocalDateTime() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
                // 将LocalDateTime类型实例替换为ZonedDateTime类型实例:
                if (bean instanceof LocalDateTime) {
                    return ZonedDateTime.now();
                }
                return bean;
            }
        };
    }
}
```
运行可知，我们定义的`@Bean`类型明明是`LocalDateTime`类型，但却被另一个`BeanPostProcessor`替换成了`ZonedDateTime`，于是，调用`getBean(ZonedDateTime.class)`可以拿到替换后的Bean，调用`getBean(LocalDateTime.class)`会报错，提示找不到Bean。那么原始的Bean哪去了？答案是被`BeanPostProcessor`扔掉了。

可见，`BeanPostProcessor`是一种特殊Bean，它的作用是根据条件替换某些Bean。上述的例子中，`LocalDateTime`被替换为`ZonedDateTime`其实没啥意义，但实际应用中，把原始Bean替换为代理后的Bean是非常常见的，比如下面的基于Spring的代码：
```java
@Configuration
@ComponentScan
public class AppConfig {

    public static void main(String[] args) {
        var ctx = new AnnotationConfigApplicationContext(AppConfig.class);
        UserService u = ctx.getBean(UserService.class);
        System.out.println(u.getClass().getSimpleName()); // UserServiceProxy
        u.register("bob@example.com", "bob12345");
    }

    @Bean
    BeanPostProcessor createProxy() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
                // 实现事务功能:
                if (bean instanceof UserService u) {
                    return new UserServiceProxy(u);
                }
                return bean;
            }
        };
    }
}

@Component
class UserService {
    public void register(String email, String password) {
        System.out.println("INSERT INTO ...");
    }
}

// 代理类:
class UserServiceProxy extends UserService {
    UserService target;

    public UserServiceProxy(UserService target) {
        this.target = target;
    }

    @Override
    public void register(String email, String password) {
        System.out.println("begin tx");
        target.register(email, password);
        System.out.println("commit tx");
    }
}
```
如果执行上述代码，打印出的Bean类型不是`UserService`，而是`UserServiceProxy`，因此，调用`register()`会打印出`begin tx`和`commit tx`，说明“事务”生效了。

迄今为止，创建`Proxy`似乎没有什么影响。让我们把代码再按实际情况扩展一下，`UserService`是用户编写的业务代码，需要注入`JdbcTemplate`：
```java
@Component
class UserService {
    @Autowired JdbcTemplate jdbcTemplate;
    
    public void register(String email, String password) {
        jdbcTemplate.update("INSERT INTO ...");
    }
}
```
而`PostBeanProcessor`一般由框架本身提供事务功能，所以它会动态创建一个`UserServiceProxy`：
```java
class UserServiceProxy extends UserService {
    UserService target;

    public UserServiceProxy(UserService target) {
        this.target = target;
    }

    @Override
    public void register(String email, String password) {
        System.out.println("begin tx");
        target.register(email, password);
        System.out.println("commit tx");
    }
}
```
调用用户注册的页面由`MvcController`控制，因此，将`UserService`注入到`MvcController`：
```java
@Controller
class MvcController {
    @Autowired UserService userService;
    
    @PostMapping("/register")
    void register() {
        userService.register(...);
    }
}
```
一开始，由IoC容器创建的Bean包括：

* JdbcTemplate
* UserService
* MvcController
接着，由于`BeanPostProcessor`的介入，原始的`UserService`被替换为`UserServiceProxy`：

* JdbcTemplate
* UserServiceProxy
* MvcController
那么问题来了：注意到`UserServiceProxy`是从`UserService`继承的，它也有一个`@Autowired JdbcTemplate`，那`JdbcTemplate`实例应注入到原始的`UserService`还是`UserServiceProxy`？

从业务逻辑出发，`JdbcTemplate`实例必须注入到原始的`UserService`，否则，代理类`UserServiceProxy`执行`target.register()`时，相当于对原始的`UserService`调用`register()`方法，如果`JdbcTemplate`没有注入，将直接报`NullPointerException`错误。

这时第二个问题又来了：`MvcController`需要注入的`UserService`，应该是原始的`UserService`还是`UserServiceProxy`？

还是从业务逻辑出发，`MvcController`需要注入的`UserService`必须是`UserServiceProxy`，否则，事务不起作用。

我们用图描述一下注入关系：

┌───────────────┐
│MvcController  │
├───────────────┤   ┌────────────────┐
│- userService ─┼──▶│UserServiceProxy│
└───────────────┘   ├────────────────┤
                    │- jdbcTemplate  │
                    ├────────────────┤   ┌────────────────┐
                    │- target       ─┼──▶│UserService     │
                    └────────────────┘   ├────────────────┤   ┌────────────┐
                                         │- jdbcTemplate ─┼──▶│JdbcTemplate│
                                         └────────────────┘   └────────────┘
注意到上图的`UserService`已经脱离了IoC容器的管理，因为此时`UserService`对应的`BeanDefinition`中，存放的`instance`是`UserServiceProxy`。

可见，引入`BeanPostProcessor`可以实现Proxy机制，但也让依赖注入变得更加复杂。
但是我们仔细分析依赖关系，还是可以总结出两条原则：

1. 一个Bean如果被Proxy替换，则依赖它的Bean应注入Proxy，即上图的`MvcController`应注入`UserServiceProxy`；

2. 一个Bean如果被Proxy替换，如果要注入依赖，则应该注入到原始对象，即上图的`JdbcTemplate`应注入到原始的`UserService`。

基于这个原则，要满足条件1是很容易的，因为只要创建Bean完成后，立刻调用`BeanPostProcessor`就实现了替换，后续其他Bean引用的肯定就是Proxy了。先改造创建Bean的流程，在创建`@Configuration`后，接着创建`BeanPostProcessor`，再创建其他普通Bean：
```java
public AnnotationConfigApplicationContext(Class<?> configClass, PropertyResolver propertyResolver) {
    ...
    // 创建@Configuration类型的Bean:
    this.beans.values().stream()
            // 过滤出@Configuration:
            .filter(this::isConfigurationDefinition).sorted().map(def -> {
                createBeanAsEarlySingleton(def);
                return def.getName();
            }).collect(Collectors.toList());

    // 创建BeanPostProcessor类型的Bean:
    List<BeanPostProcessor> processors = this.beans.values().stream()
            // 过滤出BeanPostProcessor:
            .filter(this::isBeanPostProcessorDefinition)
            // 排序:
            .sorted()
            // 创建BeanPostProcessor实例:
            .map(def -> {
                return (BeanPostProcessor) createBeanAsEarlySingleton(def);
            }).collect(Collectors.toList());
    this.beanPostProcessors.addAll(processors);

    // 创建其他普通Bean:
    createNormalBeans();
    ...
}
```
再继续修改`createBeanAsEarlySingleton()`，创建Bean实例后，调用`BeanPostProcessor`处理：
```java
public Object createBeanAsEarlySingleton(BeanDefinition def) {
    ...

    // 创建Bean实例:
    Object instance = ...;
    def.setInstance(instance);

    // 调用BeanPostProcessor处理Bean:
    for (BeanPostProcessor processor : beanPostProcessors) {
        Object processed = processor.postProcessBeforeInitialization(def.getInstance(), def.getName());
        // 如果一个BeanPostProcessor替换了原始Bean，则更新Bean的引用:
        if (def.getInstance() != processed) {
            def.setInstance(processed);
        }
    }
    return def.getInstance();
}
```
现在，如果一个Bean被替换为Proxy，那么`BeanDefinition`中的`instance`已经是Proxy了，这时，对这个Bean进行依赖注入会有问题，因为注入的是Proxy而不是原始Bean，怎么办？

这时我们要思考原始Bean去哪了？原始Bean实际上是被`BeanPostProcessor`给丢了！如果`BeanPostProcessor`能保存原始Bean，那么，注入前先找到原始Bean，就可以把依赖正确地注入给原始Bean。我们给`BeanPostProcessor`加一个`postProcessOnSetProperty()`方法，让它返回原始Bean：
```java
public interface BeanPostProcessor {
    // 注入依赖时,应该使用的Bean实例:
    default Object postProcessOnSetProperty(Object bean, String beanName) {
        return bean;
    }
}
```
再继续把`injectBean()`改一下，不要直接拿`BeanDefinition.getInstance()`，而是拿到原始Bean：
```java
void injectBean(BeanDefinition def) {
    // 获取Bean实例，或被代理的原始实例:
    Object beanInstance = getProxiedInstance(def);
    try {
        injectProperties(def, def.getBeanClass(), beanInstance);
    } catch (ReflectiveOperationException e) {
        throw new BeanCreationException(e);
    }
}
```
`getProxiedInstance()`就是为了获取原始Bean：
```java
Object getProxiedInstance(BeanDefinition def) {
    Object beanInstance = def.getInstance();
    // 如果Proxy改变了原始Bean，又希望注入到原始Bean，则由BeanPostProcessor指定原始Bean:
    List<BeanPostProcessor> reversedBeanPostProcessors = new ArrayList<>(this.beanPostProcessors);
    Collections.reverse(reversedBeanPostProcessors);
    for (BeanPostProcessor beanPostProcessor : reversedBeanPostProcessors) {
        Object restoredInstance = beanPostProcessor.postProcessOnSetProperty(beanInstance, def.getName());
        if (restoredInstance != beanInstance) {
            beanInstance = restoredInstance;
        }
    }
    return beanInstance;
}
```
这里我们还能处理多次代理的情况，即一个原始Bean，比如`UserService`，被一个事务处理的`BeanPostProcessor`代理为`UserServiceTx`，又被一个性能监控的`BeanPostProcessor`代理为`UserServiceMetric`，还原的时候，对`BeanPostProcessor`做一个倒序，先还原为`UserServiceTx`，再还原为`UserService`。
**测试**
我们可以写一个测试来验证Bean的注入是否正确。先定义原始Bean：
```java
@Component
public class OriginBean {
    @Value("${app.title}")
    public String name;

    @Value("${app.version}")
    public String version;

    public String getName() {
        return name;
    }
}
```
通过`FirstProxyBeanPostProcessor`代理为`FirstProxyBean`：
```java
@Order(100)
@Component
public class FirstProxyBeanPostProcessor implements BeanPostProcessor {
    // 保存原始Bean:
    Map<String, Object> originBeans = new HashMap<>();

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        if (OriginBean.class.isAssignableFrom(bean.getClass())) {
            // 检测到OriginBean,创建FirstProxyBean:
            FirstProxyBean proxy = new FirstProxyBean((OriginBean) bean);
            // 保存原始Bean:
            originBeans.put(beanName, bean);
            // 返回Proxy:
            return proxy;
        }
        return bean;
    }

    @Override
    public Object postProcessOnSetProperty(Object bean, String beanName) {
        Object origin = originBeans.get(beanName);
        if (origin != null) {
            // 存在原始Bean时,返回原始Bean:
            return origin;
        }
        return bean;
    }
}

// 代理Bean:
class FirstProxyBean extends OriginBean {
    final OriginBean target;

    public FirstProxyBean(OriginBean target) {
        this.target = target;
    }

    @Override
    public String getVersion() {
        return target.getVersion();
    }

    @Override
    public String getName() {
        return target.getName();
    }
}
```
通过`SecondProxyBeanPostProcessor`代理为`SecondProxyBean`：
```java
@Order(200)
@Component
public class SecondProxyBeanPostProcessor implements BeanPostProcessor {
    // 保存原始Bean:
    Map<String, Object> originBeans = new HashMap<>();

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        if (OriginBean.class.isAssignableFrom(bean.getClass())) {
            // 检测到OriginBean,创建SecondProxyBean:
            SecondProxyBean proxy = new SecondProxyBean((OriginBean) bean);
            // 保存原始Bean:
            originBeans.put(beanName, bean);
            // 返回Proxy:
            return proxy;
        }
        return bean;
    }

    @Override
    public Object postProcessOnSetProperty(Object bean, String beanName) {
        Object origin = originBeans.get(beanName);
        if (origin != null) {
            // 存在原始Bean时,返回原始Bean:
            return origin;
        }
        return bean;
    }
}

// 代理Bean:
class SecondProxyBean extends OriginBean {
    final OriginBean target;

    public SecondProxyBean(OriginBean target) {
        this.target = target;
    }

    @Override
    public String getName() {
        return target.getName();
    }

    @Override
    public String getVersion() {
        return target.getVersion();
    }
}
```
测试代码如下：
```java
    @Test
    public void InjectProxyTest() {
        Map<String, Object> map = YamlUtils.loadYamlAsPlainMap("application.yaml");
        Properties properties = new Properties();
        properties.putAll(map);
        PropertyResolver propertyResolver = new PropertyResolver(properties);
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(App.class, propertyResolver);
        // 获取OriginBean的实例,此处获取的应该是SendProxyBeanProxy:
        OriginBean proxy = ctx.getBean(OriginBean.class);
        assertSame(SecondProxyBean.class, proxy.getClass());

        // proxy的name和version字段并没有被注入:
        assertNull(proxy.name);
        assertNull(proxy.version);

        // 但是调用proxy的getName()会最终调用原始Bean的getName(),从而返回正确的值:
        assertEquals("WinterFramework", proxy.getName());
        assertEquals("1.0", proxy.getVersion());

        // 获取InjectProxyOnConstructorBean实例:
        InjectProxyOnConstructorBean inject = ctx.getBean(InjectProxyOnConstructorBean.class);
        // 注入的OriginBean应该为Proxy，而且和前面返回的proxy是同一实例:
        assertSame(proxy, inject.injected);
        ctx.close();
    }
```
从上面的测试代码我们也能看出，对于使用Proxy模式的Bean来说，正常的方法调用对用户是透明的，但是，直接访问Bean注入的字段，如果获取的是Proxy，则字段全部为`null`，因为注入并没有发生在Proxy，而是原始Bean。这也是为什么当我们需要访问某个注入的Bean时，总是调用方法而不是直接访问字段：
```java
@Component
public class MailService {
    @Autowired
    UserService userService;

    public String sendMail() {
        // 错误:不要直接访问UserService的字段,因为如果UserService被代理,则返回null:
        ZoneId zoneId = userService.zoneId;
        // 正确:通过方法访问UserService的字段,无论是否被代理,返回值均是正确的:
        ZoneId zoneId = userService.getZoneId();
        ...
    }
}
```
## 7. 完成IoC容器
现在，我们已经完成了IoC容器的基本功能。最后的收尾工作主要是提取接口。先定义给用户使用的`ApplicationContext`接口：
```java
public interface ApplicationContext extends AutoCloseable {

    // 是否存在指定name的Bean？
    boolean containsBean(String name);

    // 根据name返回唯一Bean，未找到抛出NoSuchBeanDefinitionException
    <T> T getBean(String name);

    // 根据name返回唯一Bean，未找到抛出NoSuchBeanDefinitionException
    <T> T getBean(String name, Class<T> requiredType);

    // 根据type返回唯一Bean，未找到抛出NoSuchBeanDefinitionException
    <T> T getBean(Class<T> requiredType);

    // 根据type返回一组Bean，未找到返回空List
    <T> List<T> getBeans(Class<T> requiredType);

    // 关闭并执行所有bean的destroy方法
    void close();
}
```
让`AnnotationConfigApplicationContext`实现接口：
```java
public class AnnotationConfigApplicationContext implements ConfigurableApplicationContext {
    ...
}
```

最后加一个ApplicationUtils类，目的是能通过getRequiredApplicationContext()方法随时获取到ApplicationContext实例。

搞定winter-context模块！

为什么我们用了不到1000行核心代码，就实现了`ApplicationContext`？如果查看Spring的源码，可以看到，光是层次结构，就令人眼花缭乱：
```
BeanFactory
  HierarchicalBeanFactory
    ConfigurableBeanFactory
      AbstractBeanFactory
        AbstractAutowireCapableBeanFactory
          DefaultListableBeanFactory
    ApplicationContext
      ConfigurableApplicationContext
        AbstractApplicationContext
          AbstractRefreshableApplicationContext
            AbstractXmlApplicationContext
              ClassPathXmlApplicationContext
              FileSystemXmlApplicationContext
          GenericApplicationContext
            AnnotationConfigApplicationContext
            GenericXmlApplicationContext
            StaticApplicationContext
```
其实根本原因是我们大幅简化了需求。Spring最早提供了`BeanFactory`和`ApplicationContext`两种容器，前者是懒加载，后者是立刻初始化所有Bean。懒加载的特性会导致依赖注入变得更加复杂，虽然`BeanFactory`在实际项目中并没有什么卵用。然而一旦发布了接口，处于兼容性考虑，就没法再收回去了。再考虑到Spring最早采用XML配置，后来采用`Annotation`配置，还允许混合配置，这样一来，早期发布的`XmlApplicationContext`不能动，新的`Annotation`配置就必须添加新的实现类，所以，代码的复杂度随着需求增加而增加，保持兼容性又会导致需要更多的代码来实现新功能。

所以，没事不要瞎提需求。
# 实现AOP
实现了IoC容器后，我们继续实现AOP功能。
AOP即Aspect Oriented Programming，面向切面编程，它本质上就是一个Proxy模式，只不过可以让IoC容器在运行时再组合起来，而不是事先自己用Proxy模式写死了。而实现Proxy模式的核心是拦截目标Bean的方法调用。

既然原理是方法拦截，那么AOP的实现方式不外乎以下几种：

1. 编译期：在编译时，由编译器把切面调用编译进字节码，这种方式需要定义新的关键字并扩展编译器，AspectJ就扩展了Java编译器，使用关键字aspect来实现织入；
2. 类加载器：在目标类被装载到JVM时，通过一个特殊的类加载器，对目标类的字节码重新“增强”；
3. 运行期：目标对象和切面都是普通Java类，通过JVM的动态代理功能或者第三方库实现运行期动态织入。
从复杂度看，最简单的是方案3，因为不涉及到任何JVM底层。

方案3又有两种实现方式：

1. 使用Java标准库的动态代理机制，不过仅支持对接口代理，无法对具体类实现代理；
2. 使用CGLIB或Javassist这些第三方库，通过动态生成字节码，可以对具体类实现代理。
那么Spring的实现方式是啥？Spring实际上内置了多种代理机制，如果一个Bean声明的类型是接口，那么Spring直接使用Java标准库实现对接口的代理，如果一个Bean声明的类型是Class，那么Spring就使用CGLIB动态生成字节码实现代理。

除了实现代理外，还得有一套机制让用户能定义代理。Spring又提供了多种方式：

用AspectJ的语法来定义AOP，比如`execution(public * com.itranswarp.service.*.*(..))`；
用注解来定义AOP，比如用`@Transactional`表示开启事务。
用表达式匹配，很容易漏掉或者打击面太大。用注解无疑是最简单的，因为这样被装配的Bean自己能清清楚楚地知道自己被安排了。因此，在Winter Framework中，我们只支持Annotation模式的AOP机制，并且采用动态生成字节码的方式实现。

明确了需求，我们来看如何实现动态生成字节码。Spring采用的是CGLIB，但是CGLIB已经不维护了，建议使用ByteBuddy。既然如此，我们就选择ByteBuddy实现AOP吧。
比较一下Spring Framework和Winter Framework对AOP的支持：

|              | Spring Framework | 	Winter Framework |
|:------------:|:----------------:|-------------------|
|  AspectJ方式   |       	支持        | 	不支持              |
| Annotation方式 |       	支持        | 	支持               |
|     代理接口     |       	支持        | 	不支持              |
|     代理类      |       	支持        | 	支持               |
|     实现机制     |      	CGLIB      | 	ByteBuddy        |
下面我们就来准备实现AOP。
## 1. 实现ProxyResolver
为了实现AOP，我们先思考如何在IoC容器中实现一个动态代理。

在IoC容器中，实现动态代理需要用户提供两个Bean：

1. 原始Bean，即需要被代理的Bean；
2. 拦截器，即拦截了目标Bean的方法后，会自动调用拦截器实现代理功能。
拦截器需要定义接口，这里我们直接用Java标准库的`InvocationHandler`，免去了自定义接口。

假定我们已经从IoC容器中获取了原始Bean与实现了`InvocationHandler`的拦截器Bean，那么就可以编写一个`ProxyResolver`来实现AOP代理。

从[ByteBuddy](http://bytebuddy.net/#/tutorial-cn)的官网上搜索很容易找到相关代码，我们整理为`createProxy()`方法：
```java
public class ProxyResolver {
    // ByteBuddy实例:
    ByteBuddy byteBuddy = new ByteBuddy();

    // 传入原始Bean、拦截器，返回代理后的实例:
    public <T> T createProxy(T bean, InvocationHandler handler) {
        // 目标Bean的Class类型:
        Class<?> targetClass = bean.getClass();
        // 动态创建Proxy的Class:
        Class<?> proxyClass = this.byteBuddy
                // 子类用默认无参数构造方法:
                .subclass(targetClass, ConstructorStrategy.Default.DEFAULT_CONSTRUCTOR)
                // 拦截所有public方法:
                .method(ElementMatchers.isPublic()).intercept(InvocationHandlerAdapter.of(
                        // 新的拦截器实例:
                        new InvocationHandler() {
                            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                                // 将方法调用代理至原始Bean:
                                return handler.invoke(bean, method, args);
                            }
                        }))
                // 生成字节码:
                .make()
                // 加载字节码:
                .load(targetClass.getClassLoader()).getLoaded();
        // 创建Proxy实例:
        Object proxy;
        try {
            proxy = proxyClass.getConstructor().newInstance();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return (T) proxy;
    }
}
```
注意`InvocationHandler`有两层：外层的`invoke()`传入的Object是Proxy实例，内层的`invoke()`将调用转发至原始Bean。

一共大约50行代码，我们就实现了AOP功能。有点不敢相信，赶快写个测试看看效果。

先定义一个`OriginBean`：
```java
public class OriginBean {
    public String name;

    @Polite
    public String hello() {
        return "Hello, " + name + ".";
    }

    public String morning() {
        return "Morning, " + name + ".";
    }
}
```
我们要实现的AOP功能是增强带@Kele注解的方法，把返回值Hello, bavov14.改为Hello, bavov14!(被可乐修改!)，因此，编写一个`InvocationHandler`：
```java
public class PoliteInvocationHandler implements InvocationHandler {
    @Override
    public Object invoke(Object bean, Method method, Object[] args) throws Throwable {
        // 修改标记了@Kele的方法返回值:
        if (method.getAnnotation(Kele.class) != null) {
            String ret = (String) method.invoke(bean, args);
            if (ret.endsWith(".")) {
                ret = ret.substring(0, ret.length() - 1) + "!(被可乐修改！)";
            }
            return ret;
        }
        return method.invoke(bean, args);
    }
}
```
测试代码：
```java
        //原始Bean
        OriginBean originBean = new OriginBean();
        originBean.name = "bavov14";
        //调用原始Bean的hello();
        assertEquals("Hello, bavov14.", originBean.hello());

        //创建proxy
        ProxyResolver proxyResolver = ProxyResolver.getInstance();
        OriginBean proxyBean = proxyResolver.creatProxy(originBean, new PoliteInvocationHandler());

        System.out.println(proxyBean.getClass().getName());

        //proxy类和OriginBean不同
        assertNotSame(OriginBean.class, proxyBean.getClass());

        //proxyBean的name字段为null
        assertNull(proxyBean.name);

        //调用带@Kele的方法
        assertEquals("Hello, bavov14!(被可乐修改！)", proxyBean.hello());
        //不带@Kele的方法
        assertEquals("Morning, bavov14.", proxyBean.morning());
```
测试通过，本节结束。
