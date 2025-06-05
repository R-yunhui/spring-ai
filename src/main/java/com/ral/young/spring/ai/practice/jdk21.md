```markdown
# JDK 21 全面核心特性详解与示例

## 引言

JDK 21 是一个重要的长期支持 (LTS) 版本，它带来了众多令人兴奋的新特性和改进，旨在提升Java开发者的生产力、应用程序的性能和可维护性。本文档将详细介绍JDK 21中的关键特性，包括虚拟线程、结构化并发、记录模式、switch模式匹配增强、字符串模板、序列化集合等，并提供代码示例以便理解。

---

## 一、并发编程的革新

### 1. 虚拟线程 (Virtual Threads - JEP 444, 正式发布)

#### a. 什么是虚拟线程？
虚拟线程是由 JVM 管理的轻量级线程，它们不直接一对一映射到操作系统内核线程（平台线程）。许多虚拟线程可以运行在少量的平台线程（称为载体线程, Carrier Threads）之上。

**核心特点：**
*   **轻量级：** 创建和销毁成本远低于平台线程，内存占用极小。
*   **高并发量：** 可以轻松创建数百万个虚拟线程。
*   **JVM 管理：** JVM负责调度。
*   **API 兼容：** 实现 `java.lang.Thread` 接口。

#### b. 为了解决什么问题？
解决传统平台线程在处理大量并发I/O密集型任务时，因线程阻塞导致的资源浪费和并发能力受限问题。

#### c. 工作原理：优雅处理I/O阻塞
当虚拟线程执行阻塞I/O操作时，它会“卸载”自己，释放其载体线程去执行其他任务。I/O完成后，虚拟线程再由JVM调度到某个可用载体线程上继续执行。

#### d. 为什么说可以创建“无数个”虚拟线程？这不也受限于CPU核心数吗？
*   **“无数个”：** 源于其轻量级和与OS线程解耦的特性。
*   **CPU核心数限制：** 真正**并行计算**的上限是CPU核心数。虚拟线程的优势在于I/O密集型场景，通过高效利用CPU等待I/O的间隙来处理大量并发任务，而不是提升单个CPU密集型任务的计算速度。

#### e. 适用场景
主要适用于**I/O密集型**应用（如网络服务、微服务网关、消息消费者等），能以简单的同步代码风格实现高吞吐量。对于纯CPU密集型任务，传统平台线程池（数量与CPU核心数匹配）可能依然是最佳选择。

#### f. 示例代码
```java
// 创建并启动一个虚拟线程
Thread virtualThread = Thread.startVirtualThread(() -> {
    System.out.println("Hello from Virtual Thread: " + Thread.currentThread());
    try {
        Thread.sleep(1000); // 模拟I/O操作，此时载体线程会被释放
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
    System.out.println("Virtual Thread finished.");
});

// 使用 ExecutorService 创建虚拟线程池
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    for (int i = 0; i < 5; i++) {
        int taskId = i;
        executor.submit(() -> {
            System.out.println("Task " + taskId + " running in: " + Thread.currentThread());
            // 模拟工作
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
} // executor 会自动关闭并等待任务完成

virtualThread.join(); // 等待第一个虚拟线程结束
```

### 2. 结构化并发 (Structured Concurrency - JEP 453, 正式发布)

#### a. 什么是结构化并发？
一种编程范式，将并发任务的生命周期与代码的语法结构绑定，确保父任务在其派生的所有子任务完成（成功、失败或取消）后才能结束。主要通过 `StructuredTaskScope` API实现。

#### b. 为了解决什么问题？
解决传统并发编程中的任务泄漏、取消复杂、错误处理困难、可观察性差等问题。

#### c. 核心优势
*   **可靠性增强：** 杜绝任务泄漏，可靠的取消传播，健壮的错误处理策略（如 `ShutdownOnFailure`, `ShutdownOnSuccess`）。
*   **简化编码：** 更直观的并发模型，减少模板代码。
*   **提升可维护性与可观察性：** 清晰的控制流和任务层级。

#### d. 示例代码
```java
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.ExecutionException;
import java.time.Duration;

class WeatherService {
    String getWeather() throws InterruptedException {
        Thread.sleep(Duration.ofSeconds(1)); // 模拟网络延迟
        // if (Math.random() > 0.7) throw new RuntimeException("Weather service failed");
        return "Sunny";
    }
}

class UserActivityService {
    String getUserActivity() throws InterruptedException {
        Thread.sleep(Duration.ofSeconds(2)); // 模拟数据库查询
        // if (Math.random() > 0.7) throw new RuntimeException("Activity service failed");
        return "Logged in";
    }
}

public class DashboardController {
    private final WeatherService weatherSvc = new WeatherService();
    private final UserActivityService activitySvc = new UserActivityService();

    public String getDashboardData() throws InterruptedException, ExecutionException {
        // ShutdownOnFailure: 如果任何一个子任务失败，其他子任务会被取消，scope会关闭
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            // Fork 两个并发子任务
            StructuredTaskScope.Subtask<String> weatherTask = scope.fork(weatherSvc::getWeather);
            StructuredTaskScope.Subtask<String> activityTask = scope.fork(activitySvc::getUserActivity);

            // 等待所有子任务完成或 scope 因策略关闭
            scope.join();
            // 如果任何子任务失败，则抛出异常 (由 ShutdownOnFailure 策略收集的第一个异常)
            scope.throwIfFailed();

            // 如果都成功，获取结果
            String weather = weatherTask.get();
            String activity = activityTask.get();

            return String.format("Weather: %s, Activity: %s", weather, activity);

        } // try-with-resources 确保 scope.close() 被调用
    }

    public static void main(String[] args) {
        DashboardController controller = new DashboardController();
        try {
            String data = controller.getDashboardData();
            System.out.println("Dashboard Data: " + data);
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Failed to get dashboard data: " + e.getCause().getMessage());
        }
    }
}
```

### 3. 作用域值 (Scoped Values - JEP 446, 预览阶段第二次)

#### a. 什么是作用域值？
一种在线程内以及线程的子线程（通过结构化并发创建的）之间安全高效共享不可变数据的新机制。它们是线程局部变量 (ThreadLocal) 的一种更优替代方案，尤其适用于虚拟线程。

#### b. 为了解决什么问题？
*   避免传统 `ThreadLocal` 在虚拟线程场景下可能因生命周期管理不当导致的内存泄漏或数据污染问题。
*   提供更清晰的“一次写入，多处读取”的不可变数据共享模式。
*   提升性能，因为作用域值通常被JVM优化，访问速度快。

#### c. 核心特点
*   **不可变性：** 一旦绑定，值在作用域内不可更改。
*   **作用域限定：** 值仅在特定代码块（作用域）内及其子作用域内可见。
*   **性能优化：** 设计上对虚拟线程友好，避免了 `ThreadLocal` 的一些开销。

#### d. 示例代码
```java
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.ExecutionException;

public class ScopedValueExample {

    // 1. 定义一个 ScopedValue
    private final static ScopedValue<String> LOGGED_IN_USER = ScopedValue.newInstance();

    public static void main(String[] args) {
        // 2. 在一个作用域内绑定值并运行代码
        String result = ScopedValue.where(LOGGED_IN_USER, "Alice")
                                   .call(ScopedValueExample::processUserRequest);

        System.out.println("Main thread result: " + result);

        // 尝试在作用域外访问，会抛出 NoSuchElementException
        // System.out.println(LOGGED_IN_USER.get()); // 这会抛异常

        // 结合结构化并发
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            ScopedValue.where(LOGGED_IN_USER, "Bob").run(() -> {
                scope.fork(() -> {
                    System.out.println("Task 1 user: " + LOGGED_IN_USER.get()); // 输出 Bob
                    return "Task 1 done";
                });
                scope.fork(() -> {
                    // 可以在子作用域内重新绑定（覆盖）
                    ScopedValue.where(LOGGED_IN_USER, "Charlie").run(() -> {
                         System.out.println("Task 2 (nested) user: " + LOGGED_IN_USER.get()); // 输出 Charlie
                    });
                    System.out.println("Task 2 (outer) user: " + LOGGED_IN_USER.get()); // 输出 Bob
                    return "Task 2 done";
                });
            });
            scope.join();
            scope.throwIfFailed();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    private static String processUserRequest() {
        // 3. 在作用域内获取值
        if (LOGGED_IN_USER.isBound()) {
            String user = LOGGED_IN_USER.get();
            System.out.println("Processing request for user: " + user);
            return "Data for " + user;
        } else {
            return "No user logged in.";
        }
    }
}
```

---

## 二、语言特性增强

### 1. 记录模式 (Record Patterns - JEP 440, 正式发布)

#### a. 什么是记录模式？
允许在 `instanceof` 检查和 `switch` 语句中对 `record` 类型进行解构，直接提取其组件。

#### b. 为什么引入？
简化对 `record` 组件的访问，减少模板代码，提高代码可读性。

#### c. 示例代码
```java
record Point(int x, int y) {}
record ColoredPoint(Point p, String color) {}

public class RecordPatternExample {

    static void printSum(Object obj) {
        if (obj instanceof Point(int x, int y)) { // 解构 Point
            System.out.println("Sum (Point): " + (x + y));
        }
    }

    static void printColorAndCoords(Object obj) {
        if (obj instanceof ColoredPoint(Point(int x, int y), String color)) { // 嵌套解构
            System.out.println("Color: " + color + ", X: " + x + ", Y: " + y);
        } else if (obj instanceof Point(int x, _)) { // 使用 _ 忽略 y
             System.out.println("Just a Point with x = " + x);
        }
    }

    public static void main(String[] args) {
        printSum(new Point(1, 2)); // Sum (Point): 3

        Point p = new Point(3,4);
        printColorAndCoords(new ColoredPoint(p, "RED")); // Color: RED, X: 3, Y: 4
        printColorAndCoords(new Point(5,6)); // Just a Point with x = 5
    }
}
```

### 2. switch 的模式匹配 (Pattern Matching for switch - JEP 441, 正式发布)

#### a. 什么是 switch 模式匹配？
扩展了 `switch` 语句和表达式，使其能够对多种模式进行匹配，包括类型模式、记录模式，并支持 `when` 子句（守卫模式）以及更完善的 `null` 处理。

#### b. 为什么引入？
使得 `switch` 更加强大、灵活和安全（例如，编译器可以检查 `switch` 表达式的详尽性）。

#### c. 示例代码
```java
public class SwitchPatternExample {

    static String formatter(Object obj) {
        return switch (obj) {
            case null             -> "N/A (null)"; // 直接处理 null
            case Integer i        -> String.format("int %d", i);
            case Long l           -> String.format("long %d", l);
            case Double d         -> String.format("double %f", d);
            case String s         -> String.format("String %s", s);
            // 记录模式
            case Point(int x, int y) -> String.format("Point(x=%d, y=%d)", x, y);
            // 带守卫的记录模式 (when子句)
            case ColoredPoint(Point(int x, int y), String color) when color.equalsIgnoreCase("RED") ->
                String.format("RED Point(x=%d, y=%d)", x, y);
            case ColoredPoint p -> String.format("Other ColoredPoint: %s", p.color()); // 不解构，直接用p
            default               -> "Unknown object";
        };
    }

    // 另一个例子，详尽性检查
    sealed interface Shape permits Circle, Rectangle {}
    record Circle(double radius) implements Shape {}
    record Rectangle(double width, double height) implements Shape {}

    static double getArea(Shape shape) {
        return switch (shape) { // 编译器会检查是否覆盖了所有 Shape 的子类型
            case Circle c    -> Math.PI * c.radius() * c.radius();
            case Rectangle r -> r.width() * r.height();
            // 如果 Shape 是 sealed 且所有 permitter 的类型都已覆盖，则不需要 default
        };
    }


    public static void main(String[] args) {
        System.out.println(formatter(10));                 // int 10
        System.out.println(formatter("Hello"));            // String Hello
        System.out.println(formatter(null));               // N/A (null)
        System.out.println(formatter(new Point(1, 2)));    // Point(x=1, y=2)
        System.out.println(formatter(new ColoredPoint(new Point(3,4), "RED"))); // RED Point(x=3, y=4)
        System.out.println(formatter(new ColoredPoint(new Point(5,6), "BLUE"))); // Other ColoredPoint: BLUE

        System.out.println("Area of Circle: " + getArea(new Circle(5)));
        System.out.println("Area of Rectangle: " + getArea(new Rectangle(4, 6)));
    }
}
```

### 3. 未命名模式和变量 (Unnamed Patterns and Variables - JEP 443, 正式发布)

#### a. 什么是未命名模式和变量？
允许使用下划线 `_` 来表示一个模式变量或局部变量，其值是不需要的，从而提高代码可读性并避免不必要的变量声明。

#### b. 为什么引入？
*   **提高可读性：** 清晰地表明某个值被有意忽略。
*   **减少冗余：** 无需为不使用的变量命名。
*   **避免编译器警告：** 避免“未使用变量”的警告。

#### c. 示例代码
```java
import java.util.List;
import java.io.FileInputStream;
import java.io.IOException;

public class UnnamedVariablesExample {

    record Order(String id, int quantity, double price) {}

    public static void main(String[] args) {
        // 1. 未命名模式变量 (在 instanceof 或 switch case 中)
        Object obj = new Order("order123", 5, 10.99);
        if (obj instanceof Order(String id, int quantity, _)) { // 忽略 price
            System.out.println("Order ID: " + id + ", Quantity: " + quantity);
        }

        List<Order> orders = List.of(new Order("A", 1, 0), new Order("B", 2, 0), new Order("C", 3, 0));
        int totalQuantity = 0;
        for (Order(_, int q, _) : orders) { // 忽略 id 和 price
            totalQuantity += q;
        }
        System.out.println("Total quantity: " + totalQuantity);

        // 2. 未命名局部变量
        try {
            int _ = processSomethingAndGetStatus(); // 结果状态码不关心
            System.out.println("Processing done.");
        } catch (Exception _) { // 异常类型关心，但异常对象本身不使用
            System.err.println("An error occurred during processing.");
        }

        // 3. 未命名变量在 try-with-resources
        try (var _ = new FileInputStream("temp.txt")) { // 资源本身不直接使用，但需要其自动关闭
            // ... 执行一些不直接依赖这个 stream 变量的操作 ...
            System.out.println("File 'temp.txt' might have been processed by other means or just checked for existence.");
        } catch (IOException e) {
            // 忽略，假设文件不存在是可接受的
            System.err.println("IOException (e.g., file not found): " + e.getMessage());
        }
    }

    static int processSomethingAndGetStatus() {
        return 0; // 0 for success, non-zero for error
    }
}
```

### 4. 未命名类和实例 main 方法 (Unnamed Classes and Instance Main Methods - JEP 445, 预览阶段)

#### a. 什么是未命名类和实例 main 方法？
这是一种简化Java程序编写方式的预览特性，特别适用于初学者和小型、一次性程序：
*   **未命名类：** 允许源文件不包含显式的 `class` 声明，编译器会自动推断一个类。
*   **实例 `main` 方法：** 允许 `main` 方法是非静态的，可以直接访问类的实例成员，或者甚至不需要类，直接写 `void main() { ... }`。

#### b. 为什么引入？
降低Java学习的初始门槛，让编写简单的“Hello, World!”或脚本类程序更加直接，减少仪式感代码。

#### c. 示例代码 (保存为 e.g., `HelloWorld.java`，然后用 `java --enable-preview --source 21 HelloWorld.java` 运行)

```java
// 示例 1: 最简单的形式 (顶级 main 方法)
// void main() {
//     System.out.println("Hello from unnamed class and instance main!");
// }

// 示例 2: main 方法在一个隐式类中，可以有字段和辅助方法
String greeting = "Hello";

void main() {
    greet("World (from instance main)");
    System.out.println(STR."Instance field value: \{this.greeting}");
}

void greet(String name) {
    System.out.println(STR."\{greeting}, \{name}!");
}

// 要运行以上代码 (假设保存在 MyProgram.java):
// 1. 编译并运行: javac --release 21 --enable-preview MyProgram.java && java --enable-preview MyProgram
// 2. 或者直接源文件启动: java --enable-preview --source 21 MyProgram.java
// 注意：顶级main方法（示例1）的语法在后续预览中可能有所调整，示例2更接近JEP 445描述。
// 目前（JDK 21），可以直接声明顶级方法，包括 main。
```
*注意：上述示例基于JEP 445的描述。实际预览版本的具体行为和限制请参考对应JDK的官方文档。要运行预览特性，需要添加 `--enable-preview` 和 `--source <version>` 标志。*


### 5. 字符串模板 (String Templates - JEP 430, 预览阶段)

#### a. 什么是字符串模板？
一种新的字面量形式，允许在字符串中嵌入表达式，并通过模板处理器进行处理，生成最终字符串。比传统的字符串拼接或 `String.format()` 更安全、更易读。

#### b. 为什么引入？
*   **可读性：** 将变量和表达式直接嵌入字符串中，类似其他现代语言的插值。
*   **安全性：** 默认的 `STR` 处理器会对插值进行适当转义，有助于防止注入攻击。
*   **灵活性：** 可以自定义模板处理器以实现特定逻辑（如国际化、JSON构建等）。

#### c. 核心组成
*   **模板表达式：** 形如 `STR."My name is \{name}."`，其中 `\{name}` 是嵌入的表达式。
*   **模板处理器：** `STR` 是一个标准的模板处理器。`FMT` 用于格式化（类似 `String.format`）。`RAW` 用于获取原始模板部分。

#### d. 示例代码
```java
import static java.util.FormatProcessor.FMT; // JDK 21 Preview

public class StringTemplateExample {
    public static void main(String[] args) {
        String name = "Duke";
        int age = 28;

        // 1. 使用 STR 模板处理器 (标准，用于简单插值)
        String greeting = STR."Hello, my name is \{name} and I am \{age} years old.";
        System.out.println(greeting);

        // 2. STR 支持多行字符串
        String userInfo = STR."""
            User Profile:
              Name: \{name}
              Age: \{age}
              Status: \{isAdult(age) ? "Adult" : "Minor"}
            """;
        System.out.println(userInfo);

        // 3. 使用 FMT 模板处理器 (用于格式化输出，类似 String.format)
        // 需要 import static java.util.FormatProcessor.FMT; (JDK 21 Preview)
        // 在 JDK 22 中，FMT 变为 java.lang.StringTemplate. આપણે FMT;
        double price = 19.99;
        String formattedPrice = FMT."The price is $%.2f\{price}."; // 保留两位小数
        System.out.println(formattedPrice);

        // 4. 表达式可以是方法调用或更复杂计算
        String calculation = STR."2 + 2 = \{2 + 2}";
        System.out.println(calculation);

        // 5. 防止注入 (STR 处理器会处理特殊字符)
        String maliciousInput = "<script>alert('XSS')</script>";
        String safeHtml = STR."<p>User comment: \{maliciousInput}</p>";
        System.out.println("Safe HTML: " + safeHtml); // 实际输出会是转义后的，如 <p>User comment: &lt;script&gt;alert('XSS')&lt;/script&gt;</p>
                                                    // 注意：具体转义行为取决于 STR 实现，其目标是安全。

        // 要运行预览特性，需要添加 --enable-preview 和 --source 21 标志
        // e.g., java --enable-preview --source 21 StringTemplateExample.java
    }

    static boolean isAdult(int age) {
        return age >= 18;
    }
}
```

---

## 三、API 改进与新增

### 1. 序列化集合 (Sequenced Collections - JEP 431, 正式发布)

#### a. 什么是序列化集合？
引入了一系列新的接口 (`SequencedCollection`, `SequencedSet`, `SequencedMap`)，它们为具有确定遇到顺序 (encounter order) 的集合提供统一的、易于使用的API。

#### b. 为什么引入？
在JDK中，许多集合类型（如 `List`, `LinkedHashSet`, `LinkedHashMap`, `SortedSet`, `SortedMap`）都有明确的顺序，但缺乏一个共同的父接口来定义与顺序相关的操作（如获取第一个/最后一个元素、反向视图等）。这导致API不一致和冗余代码。

#### c. 新接口及其方法
*   `SequencedCollection<E>`:
    *   `void addFirst(E)`
    *   `void addLast(E)`
    *   `E getFirst()`
    *   `E getLast()`
    *   `E removeFirst()`
    *   `E removeLast()`
    *   `SequencedCollection<E> reversed()`: 返回一个反向视图。
*   `SequencedSet<E>`: 继承自 `SequencedCollection` 和 `Set`。
*   `SequencedMap<K,V>`:
    *   `Map.Entry<K,V> firstEntry()`
    *   `Map.Entry<K,V> lastEntry()`
    *   `K firstKey()` (来自 `SortedMap` 的提升)
    *   `K lastKey()` (来自 `SortedMap` 的提升)
    *   `Map.Entry<K,V> pollFirstEntry()`
    *   `Map.Entry<K,V> pollLastEntry()`
    *   `V putFirst(K, V)`
    *   `V putLast(K, V)`
    *   `SequencedMap<K,V> reversed()`: 返回反向视图。
    *   `sequencedKeySet()`: 返回 `SequencedSet<K>`。
    *   `sequencedValues()`: 返回 `SequencedCollection<V>`。
    *   `sequencedEntrySet()`: 返回 `SequencedSet<Map.Entry<K,V>>`。

#### d. 示例代码
```java
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedCollection;
import java.util.SequencedMap;
import java.util.SequencedSet;

public class SequencedCollectionExample {
    public static void main(String[] args) {
        // List (ArrayList implements SequencedCollection)
        SequencedCollection<String> list = new ArrayList<>(List.of("One", "Two", "Three"));
        System.out.println("Original List: " + list);
        list.addFirst("Zero");
        list.addLast("Four");
        System.out.println("Modified List: " + list); // [Zero, One, Two, Three, Four]
        System.out.println("First element: " + list.getFirst()); // Zero
        System.out.println("Last element: " + list.getLast());   // Four
        System.out.println("Reversed List: " + list.reversed()); // [Four, Three, Two, One, Zero]

        // Set (LinkedHashSet implements SequencedSet)
        SequencedSet<Integer> set = new LinkedHashSet<>(List.of(10, 20, 30));
        System.out.println("\nOriginal Set: " + set);
        set.addFirst(5); // 添加成功
        set.addLast(30);  // 添加失败，元素已存在 (Set特性)
        set.addLast(40); // 添加成功
        System.out.println("Modified Set: " + set); // [5, 10, 20, 30, 40]
        System.out.println("Reversed Set: " + set.reversed()); // [40, 30, 20, 10, 5]

        // Map (LinkedHashMap implements SequencedMap)
        SequencedMap<String, Integer> map = new LinkedHashMap<>();
        map.put("Apple", 1);
        map.put("Banana", 2);
        map.put("Cherry", 3);
        System.out.println("\nOriginal Map: " + map);
        map.putFirst("Orange", 0); // 如果Orange已存在，则移到最前并更新值
        map.putLast("Date", 4);    // 如果Date已存在，则移到最后并更新值
        System.out.println("Modified Map: " + map); // {Orange=0, Apple=1, Banana=2, Cherry=3, Date=4}
        System.out.println("First entry: " + map.firstEntry()); // Orange=0
        System.out.println("Last entry: " + map.lastEntry());   // Date=4
        System.out.println("Reversed Map: " + map.reversed()); // {Date=4, Cherry=3, Banana=2, Apple=1, Orange=0}
    }
}
```

### 2. 外部函数与内存API (Foreign Function & Memory API - JEP 442, 正式发布)

#### a. 什么是FFM API？
一个用于Java程序与Java运行时之外的代码和数据进行互操作的API。它允许Java代码安全、高效地调用本地库（如C库）中的函数，并访问本地内存，旨在替代JNI (Java Native Interface)。

#### b. 为什么引入？
JNI复杂、易错且不安全。FFM API提供了更现代化、更安全、更易用的方式来进行本地互操作。

#### c.核心组件
*   `Linker`: 用于链接到本地函数。
*   `FunctionDescriptor`: 描述本地函数的签名（参数类型和返回类型）。
*   `MethodHandle`: 用于调用本地函数。
*   `MemorySegment` 和 `Arena`: 用于分配和管理本地内存。
*   `SymbolLookup`: 用于查找本地库中的符号（函数名）。

#### d. 示例代码 (概念性，调用C标准库的 `strlen`)
```java
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

public class FFMExample {
    public static void main(String[] args) throws Throwable {
        // 1. 获取本地链接器
        Linker linker = Linker.nativeLinker();

        // 2. 查找 C 标准库 (名称因平台而异，通常默认查找器可以找到)
        // 对于 Windows，可能是 "msvcrt.dll"; Linux/macOS 通常是 "libc.so.6" 或 "libc.dylib"
        // SymbolLookup stdlib = SymbolLookup.libraryLookup("msvcrt", Arena.global()); // Windows
        SymbolLookup stdlib = linker.defaultLookup(); // 通常能找到标准C库函数

        // 3. 查找 strlen 函数
        MemorySegment strlen_address = stdlib.find("strlen")
                .orElseThrow(() -> new RuntimeException("strlen not found"));

        // 4. 定义 strlen 函数的描述符: long strlen(Address)
        FunctionDescriptor strlen_descriptor = FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS);

        // 5. 创建一个 MethodHandle 来调用 strlen
        MethodHandle strlen_mh = linker.downcallHandle(strlen_address, strlen_descriptor);

        // 6. 分配本地内存并存储一个 C 字符串 (null-terminated)
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment cString = arena.allocateFrom("Hello, FFM!");

            // 7. 调用 strlen
            long length = (long) strlen_mh.invokeExact(cString);
            System.out.println("Length of 'Hello, FFM!': " + length); // 输出 11

            // 访问本地内存中的数据
            // for (long i = 0; i < length; i++) {
            //     System.out.print((char) cString.get(ValueLayout.JAVA_BYTE, i));
            // }
            // System.out.println();
        }
    }
}
```

---

## 四、其他值得关注的特性与改进

*   **ZGC 分代收集 (Generational ZGC - JEP 439, 实验性):** 为ZGC引入分代垃圾收集能力，旨在降低停顿时间，并减少CPU和内存开销，尤其对于需要低延迟和高吞吐量的应用。
*   **Vector API (第六次孵化 - JEP 448):** 提供一种表达向量计算（SIMD操作）的方式，这些计算在运行时可以编译为CPU架构上最优的向量指令，以获得显著的性能提升。仍在孵化阶段。
*   **密钥封装机制API (Key Encapsulation Mechanism API - JEP 452, 正式发布):** 为密钥封装机制（KEMs）提供了一个API，这是一种使用公钥加密来保护对称密钥的加密技术。
*   **弃用 Windows 32位 x86 移植版本以准备移除 (JEP 449):** 为未来移除对 Windows 32位 x86 平台的支持做准备。
*   **准备禁止动态加载代理 (JEP 451):** 默认情况下，当代理动态附加到正在运行的JVM时，会发出警告，并计划在未来版本中默认禁止，以提高完整性。

---

## 总结

JDK 21 作为一个LTS版本，带来了众多实质性的改进和强大的新功能。虚拟线程和结构化并发彻底改变了Java并发编程的面貌，使得构建高并发、高吞吐应用更加简单和高效。语言层面的增强如记录模式、switch模式匹配、字符串模板等进一步提升了代码的表达力和开发效率。FFM API和序列化集合等API的改进也为特定场景提供了更好的解决方案。开发者应积极学习和探索这些新特性，以便在项目中充分利用它们带来的优势。

**注意：** 对于标记为“预览阶段”或“孵化阶段”的特性，它们在未来的JDK版本中可能会发生变化或被移除。在生产环境中使用这些特性时需要谨慎，并密切关注其后续发展。要启用预览特性，编译和运行时通常需要添加 `--enable-preview --source <version>` 标志。
```

这份文档现在更加全面，涵盖了JDK 21中大部分面向开发者的重要新特性，并为每个特性提供了基本解释、目的、优势以及代码示例。希望对您有所帮助！