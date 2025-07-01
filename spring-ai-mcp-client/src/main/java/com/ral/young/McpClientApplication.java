package com.ral.young;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * @author renyh
 * @description mcp client application
 * @date 2025/6/30 10:13
 * @since 1.0.0
 */
@SpringBootApplication
@SuppressWarnings("preview")
public class McpClientApplication {

	public static void main(String[] args) {
		SpringApplication.run(McpClientApplication.class, args);
	}

	private final String userInput = """
			 提示：
			 你是一个数据分析师，你需要根据用户的问题，回答用户的问题， 当前数据库里面的 event_detail 表里面记录了所有的告警事件，你需要根据 event_detail 表里面的告警事件，
			 精准分析用户的意图进行一一的回复。
			 event_detail 表里面的 flow_node_number 字段的值的含义如下：
			 有些存在歧义的语义是：
			 比如已处理，处理了等，意味着【事件当前处于审核节点】或者【事件当前属于误报节点并且是从处理节点流转到误报节点的】
			 比如已研判，研判了等，意味着【事件当前处于处理节点】或者【事件当前属于误报节点并且是从研判节点流转到误报节点的】
			 比如已审核，审核了等，意味着【事件当前处于真实告警节点】或者【事件当前属于处理节点并且是从审核节点流转到处理节点的】
			 "allAuthNodeInfo": {
			          "3": "研判",
			          "4": "是否研判通过",
			          "7": "处理",
			          "12": "是否处理通过",
			          "13": "审核",
			          "14": "误报",
			          "20": "是否审核通过",
			          "21": "真实告警",
			      }
			 \s
			 1.我想知道最近半年产生了多少告警事件？
			 2.分别都是什么类型的告警事件？
			 3.哪种类型的告警事件最多，最少，各占多少，占比多少？
			 4.最近半年研判了多少数据。
			 5.最近半年处理了多少数据。
			 6.最近半年误报了多少数据。
			 7.最近半年审核了多少数据。
			 8.当前待审核的数据有多少。
			 9.当前待处理的数据有多少。
			 10.当前待研判的数据有多少。
			""";

	// @Bean
	public CommandLineRunner predefinedQuestions(ChatClient.Builder chatClientBuilder, ToolCallbackProvider tools,
												 ConfigurableApplicationContext context) {

		return _ -> {

			var chatClient = chatClientBuilder
					.defaultToolCallbacks(tools)
					.build();

			System.out.println(STR."""
				>>> QUESTION: \{userInput}""");

			System.out.println(STR."""
				>>> ASSISTANT: \{chatClient.prompt(userInput).call().content()}""");

			context.close();
		};
	}
}