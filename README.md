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








