public class Example
{
	public static void main(String ...args) throws Exception
	{
		Calculator calc = new Calculator();
		String expr = "7+4*3";
		System.out.println(expr + " = " + calc.exec(expr));

//		StringBuilder s = new StringBuilder("1/1");
//		for (int i = 1; i < 100000; i++)
//			s.append("+1/"+i);
//		System.out.println(calc.exec(s.toString()));

		for (int i = 0; i <= 5; i++)
			System.out.println(i + "^2 = " + calc.exec("x^2", i));
		calc.exec(new java.io.File("myprogram.xyz"));

		calc.exec(new java.io.File("fibonacci.xyz"));
	}
}
