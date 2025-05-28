# JDK 21 新特性详解（对比 JDK 8）

## 1. 记录模式（Record Patterns）
JDK 21 引入了记录模式，这是对 JDK 16 中引入的记录类的增强。

### JDK 8 写法
```java
public class Person {
    private String name;
    private int age;
    
    // 需要手动编写构造函数、getter、equals、hashCode 等方法
}
```

### JDK 21 写法
```java
// 使用记录类
public record Person(String name, int age) {}

// 使用记录模式
if (obj instanceof Person(String name, int age)) {
    System.out.println(name + " is " + age + " years old");
}
```

## 2. 模式匹配（Pattern Matching）
### switch 表达式增强
#### JDK 8 写法
```java
String result;
switch (obj) {
    case String s:
        result = "String: " + s;
        break;
    case Integer i:
        result = "Integer: " + i;
        break;
    default:
        result = "Unknown";
}
```

#### JDK 21 写法
```java
String result = switch (obj) {
    case String s -> "String: " + s;
    case Integer i -> "Integer: " + i;
    case null -> "null";
    default -> "Unknown";
};
```

## 3. 虚拟线程（Virtual Threads）
JDK 21 引入了虚拟线程，这是对并发编程的重大改进。

### JDK 8 写法
```java
// 使用传统线程
Thread thread = new Thread(() -> {
    // 任务代码
});
thread.start();
```

### JDK 21 写法
```java
// 使用虚拟线程
Thread.startVirtualThread(() -> {
    // 任务代码
});

// 或者使用 ExecutorService
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    executor.submit(() -> {
        // 任务代码
    });
}
```

## 4. 字符串模板（String Templates）
JDK 21 预览特性，提供了更强大的字符串插值功能。

### JDK 8 写法
```java
String name = "World";
String greeting = "Hello, " + name + "!";
```

### JDK 21 写法
```java
String name = "World";
String greeting = STR."Hello, \{name}!";
```

## 5. 序列化集合（Sequenced Collections）
JDK 21 引入了新的集合接口，使集合操作更加统一和直观。

### JDK 8 写法
```java
List<String> list = new ArrayList<>();
String first = list.get(0);
String last = list.get(list.size() - 1);
```

### JDK 21 写法
```java
SequencedCollection<String> list = new ArrayList<>();
String first = list.getFirst();
String last = list.getLast();
```

## 6. 结构化并发（Structured Concurrency）
JDK 21 预览特性，提供了更好的并发任务管理方式。

### JDK 8 写法
```java
ExecutorService executor = Executors.newFixedThreadPool(2);
Future<String> future1 = executor.submit(() -> "Task 1");
Future<String> future2 = executor.submit(() -> "Task 2");
// 需要手动管理线程池和异常处理
```

### JDK 21 写法
```java
try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
    Future<String> future1 = scope.fork(() -> "Task 1");
    Future<String> future2 = scope.fork(() -> "Task 2");
    scope.join();
    scope.throwIfFailed();
}
```

## 7. 外部函数和内存 API（Foreign Function & Memory API）
JDK 21 正式发布了外部函数和内存 API，用于与本地代码交互。

### JDK 8 写法
```java
// 需要使用 JNI 和复杂的本地方法
public native void nativeMethod();
```

### JDK 21 写法
```java
// 使用 Foreign Function & Memory API
import java.lang.foreign.*;

try (var session = MemorySession.openConfined()) {
    var memory = session.allocate(100);
    // 直接操作内存
}
```

## 8. 未命名模式和变量（Unnamed Patterns and Variables）
JDK 21 预览特性，允许使用下划线（_）作为未使用的模式或变量。

### JDK 8 写法
```java
try {
    // 必须声明异常变量
} catch (Exception e) {
    // 不使用异常变量
}
```

### JDK 21 写法
```java
try {
    // 使用下划线忽略异常变量
} catch (Exception _) {
    // 代码
}
```

## 9. 作用域值（Scoped Values）
JDK 21 预览特性，提供了更好的线程局部变量管理方式。

### JDK 8 写法
```java
ThreadLocal<String> context = new ThreadLocal<>();
context.set("value");
```

### JDK 21 写法
```java
ScopedValue<String> context = ScopedValue.newInstance();
ScopedValue.where(context, "value").run(() -> {
    // 在作用域内使用 context
});
```

## 10. 分代 ZGC（Generational ZGC）
JDK 21 对 ZGC 垃圾收集器进行了分代优化。

### 配置方式
```bash
# JDK 8 使用 G1GC
-XX:+UseG1GC

# JDK 21 使用分代 ZGC
-XX:+UseZGC -XX:+ZGenerational
```

## 总结
JDK 21 相比 JDK 8 带来了许多重要的改进：
1. 更现代的语法特性（记录模式、模式匹配）
2. 更强大的并发支持（虚拟线程、结构化并发）
3. 更好的性能（分代 ZGC）
4. 更安全的内存管理（外部函数和内存 API）
5. 更简洁的代码编写方式（字符串模板、未命名模式）

这些新特性不仅提高了开发效率，也带来了更好的性能和可维护性。建议根据项目需求逐步采用这些新特性，以提升代码质量和开发体验。 