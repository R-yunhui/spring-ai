package com.ral.young.spring.ai.practice;

import java.util.List;

/**
 * @author renyh
 * @description jdk21新特性学习
 * @date 2025/5/27 14:00
 * @since 1.0.0
 */
public class FeatureMain {

	public static void main(String[] args) {
		System.out.println("----- record -----");
		User user = new User("张三", 18);
		System.out.println(user);
		user.sayAge();

		/*
		 * var 关键字
		 * 1. 是一个关键字，用于声明一个局部变量
		 * 2. 编译的时候自动推断变量的类型
		 * 3. 只能用于局部变量
		 */
		System.out.println("----- var -----");
		var name = "李斯";
		var userList = List.of(new User("张三", 18), new User("李四", 19));
		var age = 21;
		System.out.println(name);
		System.out.println(userList);
		System.out.println(age);
	}

	public static record User(String name, int age) {
		/*
		 * record关键字
		 * 1. 是一个关键字，用于定义一个不可变的数据类
		 * 2. 可以自动生成构造方法、getter方法、equals方法、hashCode方法、toString方法
		 *
		 * 适用场景：
		 * 1. 创建对象时，对象属性不可变，且属性数量少时使用（当类仅用于存储数据（没有复杂行为）时，record 可以替代传统 POJO。）
		 * 2. 不可变配置或参数（如果类的字段在创建后不允许修改，record 的默认不可变性非常合适。）
		 * 3.值对象（当对象的相等性由字段值决定时（如日期、金额、坐标），record 自动生成的 equals() 和 hashCode() 非常有用。）
		 * 4.模式匹配的配合使用（record 与 switch 模式匹配（JDK 21+）结合时，代码更简洁。）
		 *
		 */

		public void sayAge() {
			System.out.println("年龄是：" + age);
		}
	}
}
