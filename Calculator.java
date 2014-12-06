import java.util.*;

public class Calculator
{	//List of Functions: sine, cosine, tangent, to_degrees, to_radians, random_number, square_root, round (needs to be modified, not correct currently), and more!
	final static String [][]func = {{"+",	"1","L"},
					{"-",	"1","L"},
					{"*",	"2","L"},
					{"/",	"2","L"},
					{"^",	"3","R"},
					{"(",	"0","R"},
					{")",	"",""},
					{"sin",	"4","R"},
					{"cos",	"4","R"},
					{"tan",	"4","R"},
					{"degr","4","R"},
					{"rads","4","R"},
					{"rand","4","R"},
					{"sqrt","4","R"},
					{"rnd",	"4","R"},
					{"ln",	"4","R"},
					{"log",	"4","R"},
					{"pi",	"4","R"},
					{"abs",	"4","R"},
					{"=",	"0","R"},
					{"print","4","R"},
					{"<",	"1","L"},
					{">",	"1","L"},
					{"jmpf","4","R"}, //jump-if => with this, and infinite memory, this language is turing-complete! FIXME: bug in index code
					{"in",	"4","R"}};
	//The types of the Token
	private static final int T_NOTYPE = -1, T_NUMBER = 0, T_FUNCTION = 1, T_VARIABLE = 2, T_JUMP = 3;

	//The following are: number of arguments accepted by the functions, and the numeric values of the functions
	final static int funcNumArgs[] = {2,           2,          2,         2,           2,          -1,          -1,         1,         1,         1,           1,           1,           0,           1,          1,         1,          1,         0,          1, /*spec*/0,            1,         2,         2,           2, /*spec*/0};
	private static final int F_PLUS = 0, F_MINUS = 1, F_MULT = 2, F_DIV = 3, F_POWER = 4, F_LBRAC = 5, F_RBRAC = 6, F_SIN = 7, F_COS = 8, F_TAN = 9, F_DEGR = 10, F_RADS = 11, F_RAND = 12, F_SQRT = 13, F_RND = 14, F_LN = 15, F_LOG = 16, F_PI = 17, F_ABS = 18, F_EQ = 19, F_PRINT = 20, F_LT = 21, F_GT = 22, F_JMPF = 23, F_IN = 24;

	//degrees or radians (by default)
	final static boolean trigDegrees = true;

	Vector< Queue<Token> > prog = new Vector< Queue<Token> >();

	double []vars = new double[26];
	public Calculator()
	{
		vars[4] = Math.E; //because e = 2.71...

		//let line numbers start from 1
		index = 1;
		prog.add(null);
	}
/*
	public static void print(Queue<Token> Q) //for debugging
	{
		int ty;
		System.out.print("\nexpr:");
		if (Q == null)
			System.out.print(" null");
		else for (java.util.StringTokenizer st = new java.util.StringTokenizer(Q.toString(), "[], "); st.hasMoreTokens();)
		{
			ty = Integer.parseInt(st.nextToken());
			if (ty == T_NUMBER)
				System.out.print(" " + st.nextToken());
			else if (ty == T_VARIABLE)
				System.out.print(" " + (char)((int)Double.parseDouble(st.nextToken())+'a'));
			else
				System.out.print(" " + func[(int)Double.parseDouble(st.nextToken())][0]);
		}
		System.out.println();
	}
*/
	//Converts the angle from degrees for trigonometry if required
	private static double cnvAng(double a)
	{
		return trigDegrees ? Math.toRadians(a) : a;
	}

	private double cnvNum(Token t)
	{
		return t.type == T_NUMBER ? t.value : vars[(int)t.value];
	}

	//exec parses, compiles, and evaluates the expression in expr
	//it returns a string containing the value, or null if there was an error
	public String exec(String expr)
	{
		String answer = null;
		Queue<Token> Q;

		Q = parse(expr);			//parse expression
		if (Q != null)
			Q = compile(Q);			//compile tokens
		if (Q != null)
			answer = process(Q);	//process tokens

		return answer;
	}
	public String exec(String expr, double x)
	{
		return exec(expr.toLowerCase().replace("x", "("+x+")"));
	}
	public void exec(java.io.File file) throws Exception
	{
		java.util.Scanner src = new java.util.Scanner(file);
		while (src.hasNextLine())
			exec(src.nextLine());
	}

	int index = 0;
	public String exec()
	{
		return exec(new java.util.Scanner(""));
	}
	public String exec(java.util.Scanner sc)
	{
		String answer = null;
		Queue<Token> Q;
		if ((Q = prog.get(index)) != null)
		{
			ArrayDeque<Token> nQ = new ArrayDeque<Token>();

			try {
				for (Token t : Q)
					nQ.add(t.clone());
				answer = process(nQ, sc);
			} catch (Exception e) {} //shouldn't fail
		}
		index++;
		return answer;
	}

	boolean interactive = false;
	public void fill(java.util.Scanner sc, int target)
	{
		while (prog.size()-1 < target) //because prog.size()-1 is the max index
		{
			if (interactive)
				System.out.print("%i" + prog.size() + ": ");
			if (!sc.hasNextLine())
				return;
			if (interactive)
				System.out.print("%o" + prog.size() + ": ");
			Queue<Token> Q = parse(sc.nextLine());
			if (Q != null)
				Q = compile(Q);
			if (Q == null)
				System.err.println("Syntax error when processing " + index);
			prog.add(Q);
		}
	}
	public void run(java.io.File file) throws Exception
	{
		run(new java.util.Scanner(file));
	}
	public void run(java.util.Scanner sc)
	{
		run(sc,sc);
	}
	public void run(java.util.Scanner code, java.util.Scanner in)
	{
		for (fill(code, 1); index > 0 && index < prog.size(); fill(code,index))
		{
			if (interactive)
				System.out.println("\t" + exec(in));
			else
				exec(in);
		}
	}

	//This method converts the expression into a Queue of tokens
	private Queue<Token> parse(String expr)
	{
		expr = ("("+expr+")")
							.replaceAll("\\s"," ")
							.replace("(-","(0-")
							.replace("(+","(");
		Queue<Token> Q = null;

		//this block tokenizes the expression, splitting it into strings of tokens
		String []tokens;								//the final array of tokenized strings
		{
			String []tmp = new String[expr.length()];	//the temporary array of tokenized strings
			int end = 0;								//number of tokens
			for (int i = 0; i < tmp.length; i++)
				tmp[i] = "";
			final String delim = "+-/^*()=<>";			//the delimiters of the tokens
			final String seper = ",";					//token seperators
			for (int i = 0; i < expr.length(); i++)
			{
				char c = expr.charAt(i);
				if (delim.indexOf(c) >= 0)
				{
					if (!tmp[end].equals(""))
						end++;
					tmp[end++] += c;
				}
				else if (seper.indexOf(c) >= 0) //added for seperator support :)
				{
					if (!tmp[end].equals(""))
						end++;
				}
				else
					tmp[end] += c;
			}
			tokens = new String[end];
			for (int i = 0; i < tokens.length; i++)
				tokens[i] = tmp[i].toLowerCase();
		}

		//This block converts the strings into Tokens
		Q = new ArrayDeque<Token>(tokens.length);
		{
			int type = T_NOTYPE;		//the type of a token
			double value = 0.0;			//the value of the token
			for (String s : tokens)		//in this block, we try to convert each string into a Token
			{
				type = T_NOTYPE;
				try { value = Double.parseDouble(s); type = T_NUMBER; } catch (Exception e) {}

				if (type == T_NOTYPE)
					for (int i = 0; i < func.length; i++)
						if (func[i][0].equals(s))
						{
							value = i;
							type = T_FUNCTION;
						}

				if (type != T_NOTYPE)
					Q.add(new Token(type, value));
				else if (s.length() == 1 && s.charAt(0) >= 'a' && s.charAt(0) <= 'z')
					Q.add(new Token(type = T_VARIABLE, s.charAt(0)-'a'));
				else
					break;
			}
			if (type == T_NOTYPE)
				Q = null;
		}
		return Q;
	}
	private Queue<Token> compile (Queue<Token> in)
	{
		Queue<Token> out = new ArrayDeque<Token>(in.size());	//the output queue
		Stack<Token> stack = new Stack<Token>();		//the operator stack
		boolean flag = false;					//flag indicating an error
		Token token;						//temporary Token variable
		while (!in.isEmpty() && !flag)				//convert the infix to postfix
		{
			token = in.remove();
			if (token.type == T_NUMBER || token.type == T_VARIABLE)
				out.add(token);
			else
				switch ((int)token.value)
				{
					//if we have a left bracket, just push it into the stack
					case F_LBRAC:	stack.push(token);
									break;
					//if we have a right bracket, pop off everything until we reach a left bracket, which we discard. If we can't, we have an error
					case F_RBRAC:	flag = true;
									while (!stack.empty())
										if ((int)stack.peek().value == F_LBRAC)
										{
											stack.pop();
											flag = false;
											break;
										}
										else
											out.add(stack.pop());
									break;
					//otherwise, it is a normal operator
					default:		while (!stack.empty())
									{
										int ct = func[ (int)token.value ][1].compareTo(  func[(int)stack.peek().value][1]  );
										if (ct < 0 || (ct == 0 && func[(int)stack.peek().value][2].equals("L"))) //note that we also need associativity
											out.add(stack.pop());
										else
											break;
									}
									stack.push(token);
				}
		}
		while (!stack.empty() && !flag) //we pop off all of the remaining tokens. there should be no left brackets left
		{
			Token t = stack.pop();
			if ((int)t.value == F_LBRAC)
				flag = true;
			else
				out.add(t);
		}
		if (flag)
			out = null;
		return out;
	}
	private String process (Queue<Token> in)
	{
		return process(in, null);
	}
	private String process (Queue<Token> in, java.util.Scanner sc)	//this method evaluates the postfix
	{
		String answer = null;			//the final answer
		Stack<Token> stack = new Stack<Token>();//the stack to evaluate postfix
		boolean flag = false;			//error flag
		while (!in.isEmpty() && !flag)		//use the stack based method to evaluate postfix
		{
			Token token = in.remove();
			switch (token.type)
			{
				//we have a number, just push it onto the stack
				case T_NUMBER:		stack.push(token);
									break;
				//we have a function, call calculate to calculate the result. calculate returns true in case of error
				case T_FUNCTION:	flag = calculate(stack, (int)token.value, sc);
									break;
				//variable, just push it
				case T_VARIABLE:	stack.push(token);
									break;
				//we have something we didn't recognize. This shouldn't happen
				default:			flag = true;
			}
		}
		//if we didn't have an error and we have exactly one value on the stack, that is the answer
		if (!flag && !stack.empty())
		{
			answer = ""+cnvNum(stack.pop());
			if (!stack.empty())
				answer = null;
		}
		return answer;
	}
	private boolean calculate(Stack<Token> stack, int function)
	{
		return calculate(stack, function, null);
	}
	private boolean calculate(Stack<Token> stack, int function, java.util.Scanner sc)	//this method evaluates a function call
	{
		boolean error = false;	//was there an error?
		double []args = null;	//an array of arguments to the function
		if (function >= 0 && function < funcNumArgs.length && funcNumArgs[function] != -1) //if the function is valid
		{
			args = new double[funcNumArgs[function]];	//allocate the array
			for (int i = args.length-1; i >= 0; i--)	//pop the appropriate arguments off, and put them into the array in the correct order
				if (stack.empty())
					return true;
				else
					args[i] = cnvNum(stack.pop());
		}
		else
			return true;
		switch (function)	//for every operator/function, push back the appropriate value
		{
			case F_PLUS:	stack.push(new Token(T_NUMBER, args[0]+args[1]  ));				// add
							break;
			case F_MINUS:	stack.push(new Token(T_NUMBER, args[0]-args[1]  ));				// subtract
							break;
			case F_MULT:	stack.push(new Token(T_NUMBER, args[0]*args[1]  ));				// multiply
							break;
			case F_DIV:		stack.push(new Token(T_NUMBER, args[0]/args[1]  ));				// divide
							break;
			case F_POWER:	stack.push(new Token(T_NUMBER, Math.pow(args[0],args[1])  ));	// exponentiation
							break;
			case F_SIN:		stack.push(new Token(T_NUMBER, Math.sin(cnvAng(args[0]))  ));	// sin, radians
							break;
			case F_COS:		stack.push(new Token(T_NUMBER, Math.cos(cnvAng(args[0]))  ));	// cosine, radians
							break;
			case F_TAN:		stack.push(new Token(T_NUMBER, Math.tan(cnvAng(args[0]))  ));	// tangent, radians
							break;
			case F_DEGR:	stack.push(new Token(T_NUMBER, args[0]*180/Math.PI  ));			// convert to degrees
							break;
			case F_RADS:	stack.push(new Token(T_NUMBER, args[0]*Math.PI/180  ));			// convert to radians
							break;
			case F_RAND:	stack.push(new Token(T_NUMBER, Math.random()  ));				// get a random number. Hooray! a zero argument function!
							break;
			case F_SQRT:	stack.push(new Token(T_NUMBER, Math.sqrt(args[0])  ));			// square root
							break;
			case F_RND:		stack.push(new Token(T_NUMBER, Math.round(args[0])  ));			// round
							break;
			case F_LN:		stack.push(new Token(T_NUMBER, Math.log(args[0])  ));			// natural logarithm
							break;
			case F_LOG:		stack.push(new Token(T_NUMBER, Math.log10(args[0])  ));			// base 10 logarithm
							break;
			case F_PI:		stack.push(new Token(T_NUMBER, Math.PI  ));						// returns pi, the ratio of the circumference to the diameter
							break;
			case F_ABS:		stack.push(new Token(T_NUMBER, Math.abs(args[0])  ));			// take the absolute value
							break;
			case F_EQ:		{
								double val;
								if (stack.empty())
								{
									error = true;
									break;
								}
								val = cnvNum(stack.pop());
								if (stack.peek().type != T_VARIABLE)
								{
									error = true;
									break;
								}
								vars[(int)stack.pop().value] = val;
								stack.push(new Token(T_NUMBER, val));
								break;
							}
			case F_PRINT:	System.out.println(args[0]);
							break;
			case F_LT:		stack.push(new Token(T_NUMBER, (args[0] < args[1]) ? 1 : 0));
							break;
			case F_GT:		stack.push(new Token(T_NUMBER, (args[1] > args[0]) ? 0 : 1));
							break;
			case F_JMPF:	if (args[0] != 0)
								index = (int)args[1]; //FIXME no error checking or bounds checking
							break;
			case F_IN:		{
								if (stack.peek().type != T_VARIABLE)
								{
									error = true;
									break;
								}
								System.out.print("" + ((char)('a'+(int)stack.peek().value)) + "? ");
								stack.push(new Token(T_NUMBER, vars[(int)stack.pop().value] = sc.nextDouble()));
								sc.nextLine();
								break;
							}

			default:		error = true;	//unrecognized function
		}
		return error;
	}

	public static void main(String ...args)
	{
		/*****************
		 * Examples:     *
		 *               *
		 * 5*6^5^4       *
		 * rand()        *
		 * rnd(1.5)      *
		 * sin(rads(45)) *
		 * sin(pi())     *
		 * sin(pi)       *
		 * sin(x)        *
		 *****************/
		System.out.println("Enter expressions, end with EOF:");
		java.util.Scanner sc = new java.util.Scanner(System.in);
		Calculator calc = new Calculator();
		calc.interactive = true;
		calc.run(sc);
/*
		while (true)
		{
			System.out.print("%i" + calc.index + ": ");
			if (!sc.hasNextLine())
				break;
			String expr = sc.nextLine();
			System.out.println("\t" + calc.run(expr) + '\n');
		}
*/
		System.out.println();
	}
}
