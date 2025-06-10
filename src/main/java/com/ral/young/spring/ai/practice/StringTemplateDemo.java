package com.ral.young.spring.ai.practice;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static java.lang.StringTemplate.RAW;
import static java.util.FormatProcessor.FMT;

/**
 * @author renyh
 * @description 字符串模板示例
 * @date 2025/6/5 17:46
 * @since 1.0.0
 */
@SuppressWarnings("preview")
public class StringTemplateDemo {

	public static void main(String[] args) {
		String name = "张三";
		int age = 30;
		double salary = 12345.6789;
		LocalDate joinDate = LocalDate.of(2020, 5, 15);

		// 1. 使用 STR 处理器 (简单插值)
		String infoSTR = STR."用户信息：\n姓名：\{name}\n年龄：\{age}岁\n月薪：\{salary}";
		System.out.println("--- STR 示例 ---");
		System.out.println(infoSTR);
		// 输出:
		// 用户信息：
		// 姓名：张三
		// 年龄：30岁
		// 月薪：12345.6789

		// STR 也可以处理方法调用和更复杂的表达式
		String greeting = STR."你好，\{name.toUpperCase()}！明年你就\{age + 1}岁了。";
		System.out.println(greeting);
		// 输出: 你好，张三！明年你就31岁了。

		// STR 与文本块结合
		String reportSTR = STR."""
            用户报告
            --------------------
            姓名: \{name}
            年龄: \{age}
            薪资: \{salary}
            入职日期: \{joinDate.format(DateTimeFormatter.ISO_DATE)}
            --------------------
            """;
		System.out.println(reportSTR);
        /* 输出:
           用户报告
           --------------------
           姓名: 张三
           年龄: 30
           薪资: 12345.6789
           入职日期: 2020-05-15
           --------------------
        */


		// 2. 使用 FMT 处理器 (格式化插值)
		// 注意: 在 JDK 21 的第一个预览版中，FMT 可能需要通过 StringTemplate.Processor.FMT 访问
		// 或者确保你正确导入了它（JDK 22 将其移至 java.util.FormatProcessor.FMT）
		// 为了演示，我们假设 FMT 处理器可用
		String formattedInfoFMT = FMT."""
            用户信息（格式化）：
            姓名：%-10s\{name}  -- 字符串左对齐，宽度10
            年龄：%03d\{age}         -- 整数，宽度3，不足补0
            月薪：%,.2f\{salary}    -- 浮点数，带千位分隔符，保留2位小数
            入职日期：%tY年%<tm月%<td日\{joinDate} -- 日期格式化
            """;
		// 上面 FMT 里的 %-10s, %03d, %,.2f, %tY年%<tm月%<td日 是格式说明符
		// 它们会依次作用于后面的 \{name}, \{age}, \{salary}, \{joinDate}
		System.out.println("\n--- FMT 示例 ---");
		System.out.println(formattedInfoFMT);
        /* 输出:
           用户信息（格式化）：
           姓名：张三        -- 字符串左对齐，宽度10
           年龄：030         -- 整数，宽度3，不足补0
           月薪：12,345.68    -- 浮点数，带千位分隔符，保留2位小数
           入职日期：2020年05月15日 -- 日期格式化
        */


		// 3. 使用 RAW 处理器 (获取 StringTemplate 对象)
		StringTemplate st = RAW."用户 \{name} (ID: \{System.currentTimeMillis()}) 尝试登录。";
		System.out.println("\n--- RAW 示例 ---");

		// 获取文本片段
		System.out.println(STR."Fragments: \{st.fragments()}");
		// 输出类似: Fragments: [用户 ,  (ID: , ) 尝试登录。] (第一个和最后一个片段)

		// 获取表达式的值
		System.out.println(STR."Values: \{st.values()}");
		// 输出类似: Values: [张三, 1678886400000] (中间的值)

		// 可以使用 StringTemplate 对象进行自定义处理，例如构建 JSON
		String jsonString = STR."""
            {
              "fragments": "\{st.fragments()}",
              "values": "\{st.values()}",
              "interpolated": "\{st.interpolate()}"
            }
            """;
		// st.interpolate() 默认使用 STR 处理器进行插值
		System.out.println("StringTemplate as JSON (example):");
		System.out.println(jsonString);

		// 也可以自定义处理器
		StringTemplate.Processor<String, RuntimeException> myProcessor =
				template -> {
					StringBuilder sb = new StringBuilder();
					for (int i = 0; i < template.values().size(); i++) {
						sb.append(template.fragments().get(i));
						Object value = template.values().get(i);
						// 自定义处理，例如对字符串进行转义或特殊格式化
						if (value instanceof String s) {
							sb.append(s.replace(" ", "_"));
						} else {
							sb.append(value);
						}
					}
					sb.append(template.fragments().getLast()); // 添加最后一个片段
					return sb.toString();
				};

		String customProcessed = myProcessor."处理: 用户 \{name} 的薪水是 \{salary}。";
		System.out.println(STR."Custom Processed: \{customProcessed}");
		// 输出: Custom Processed: 处理: 用户 张三_的薪水是 12345.6789。
	}
}
