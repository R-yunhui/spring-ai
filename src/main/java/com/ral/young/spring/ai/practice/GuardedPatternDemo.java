package com.ral.young.spring.ai.practice;

/**
 * @author renyh
 * @description switch case 守护模式
 * @date 2025/6/5 14:08
 * @since 1.0.0
 */
@SuppressWarnings("preview")
public class GuardedPatternDemo {

	sealed interface Shape permits Circle, Rectangle, Square {
	}

	record Circle(double radius) implements Shape {
	}

	record Rectangle(double length, double width) implements Shape {
	}

	// Square 可以是一个特殊的 Rectangle，或者一个独立的 record
	record Square(double side) implements Shape {
	}

	static void processShape(Shape shape) {
		switch (shape) {
			case null: // 明确处理 null
				System.out.println("Shape is null.");
				break; // 使用传统 switch 语句形式，需要 break

			// 使用守护模式 (when)
			case Circle c when c.radius() < 5:
				System.out.println(STR."Small Circle with radius: \{c.radius()}");
				break;
			case Circle c when c.radius() >= 5 && c.radius() < 10:
				System.out.println(STR."Medium Circle with radius: \{c.radius()}");
				break;
			case Circle c: // 捕获所有其他 Circle (radius >= 10)
				System.out.println(STR."Large Circle with radius: \{c.radius()}");
				break;

			case Rectangle r when r.length() == r.width(): // 实质上是正方形
				System.out.println(STR."Square-like Rectangle with side: \{r.length()}");
				break;
			case Rectangle r:
				System.out.println(STR."Rectangle with length \{r.length()} and width \{r.width()}");
				break;

			case Square s: // 如果 Square 是独立的
				System.out.println(STR."Square with side: \{s.side()}");
				break;

			// default: // 因为 Shape 是 sealed, 并且所有 permit 的子类都被覆盖了 (包括null)
			//          // 对于 switch 语句，default 不是必需的，但对于 switch 表达式，如果不是所有情况都覆盖则必需
			//          // 这里如果 Shape 不是 sealed，或者没有覆盖所有情况，就需要 default
			//  System.out.println("Unknown shape or unhandled specific case.");
			//  break;
		}
	}

	// 使用 switch 表达式形式，代码更简洁
	static String describeShape(Shape shape) {
		return switch (shape) {
			case null -> "Shape is null."; // null case

			// 使用守护模式 (when)
			case Circle c when c.radius() < 5 -> STR."Small Circle with radius: \{c.radius()}";
			case Circle c when c.radius() >= 5 && c.radius() < 10 -> STR."Medium Circle with radius: \{c.radius()}";
			case Circle c -> STR."Large Circle with radius: \{c.radius()}"; // 捕获所有其他 Circle

			case Rectangle r when r.length() == r.width() -> STR."Square-like Rectangle with side: \{r.length()}";
			case Rectangle r -> STR."Rectangle with length \{r.length()} and width \{r.width()}";

			case Square s -> STR."Square with side: \{s.side()}";
			// 因为 Shape 是 sealed，并且所有 permit 的子类都被覆盖 (包括 null case),
			// 所以对于 switch 表达式，这里不需要 default。
			// 如果 Shape 不是 sealed，则需要一个 default 分支。
		};
	}


	public static void main(String[] args) {
		processShape(new Circle(3));    // Small Circle...
		processShape(new Circle(7));    // Medium Circle...
		processShape(new Circle(12));   // Large Circle...
		processShape(new Rectangle(5, 5)); // Square-like Rectangle...
		processShape(new Rectangle(5, 8)); // Rectangle...
		processShape(new Square(4));       // Square...
		processShape(null);               // Shape is null.

		System.out.println("--- Using switch expression ---");
		System.out.println(describeShape(new Circle(3)));
		System.out.println(describeShape(new Rectangle(5, 5)));
		System.out.println(describeShape(null));
	}
}
