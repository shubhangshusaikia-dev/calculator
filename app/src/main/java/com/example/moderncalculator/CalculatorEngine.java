package com.example.moderncalculator;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/** Model: validates and evaluates calculator expressions without Android UI dependencies. */
public class CalculatorEngine {
    private static final MathContext MC = new MathContext(16, RoundingMode.HALF_UP);

    public String evaluate(String expression) {
        if (expression == null || expression.trim().isEmpty()) return "0";
        try {
            List<String> postfix = toPostfix(tokenize(expression));
            BigDecimal value = evalPostfix(postfix);
            return format(value);
        } catch (RuntimeException ex) {
            return "Error";
        }
    }

    private List<String> tokenize(String expression) {
        List<String> tokens = new ArrayList<>();
        StringBuilder number = new StringBuilder();
        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            boolean unaryMinus = c == '-' && number.length() == 0 && (tokens.isEmpty() || isBinaryOperator(tokens.get(tokens.size() - 1)));
            if (Character.isDigit(c) || c == '.' || unaryMinus) number.append(c);
            else if (isOperator(c) || c == '%') {
                if (number.length() > 0) { tokens.add(number.toString()); number.setLength(0); }
                tokens.add(String.valueOf(c));
            } else if (!Character.isWhitespace(c)) throw new IllegalArgumentException("Bad character");
        }
        if (number.length() > 0) tokens.add(number.toString());
        return tokens;
    }

    private List<String> toPostfix(List<String> tokens) {
        List<String> output = new ArrayList<>();
        ArrayDeque<String> ops = new ArrayDeque<>();
        for (String token : tokens) {
            if (isNumber(token)) output.add(token);
            else if ("%".equals(token)) output.add(token);
            else {
                while (!ops.isEmpty() && precedence(ops.peek()) >= precedence(token)) output.add(ops.pop());
                ops.push(token);
            }
        }
        while (!ops.isEmpty()) output.add(ops.pop());
        return output;
    }

    private BigDecimal evalPostfix(List<String> postfix) {
        ArrayDeque<BigDecimal> stack = new ArrayDeque<>();
        for (String token : postfix) {
            if (isNumber(token)) stack.push(new BigDecimal(token, MC));
            else if ("%".equals(token)) stack.push(stack.pop().divide(BigDecimal.valueOf(100), MC));
            else {
                if (stack.size() < 2) throw new IllegalArgumentException("Missing operand");
                BigDecimal b = stack.pop(), a = stack.pop();
                switch (token) {
                    case "+": stack.push(a.add(b, MC)); break;
                    case "-": stack.push(a.subtract(b, MC)); break;
                    case "×": stack.push(a.multiply(b, MC)); break;
                    case "÷": if (b.compareTo(BigDecimal.ZERO) == 0) throw new ArithmeticException("Divide by zero"); stack.push(a.divide(b, MC)); break;
                    default: throw new IllegalArgumentException("Bad operator");
                }
            }
        }
        if (stack.size() != 1) throw new IllegalArgumentException("Invalid expression");
        return stack.pop();
    }

    private boolean isNumber(String token) { return token.matches("-?\\d+(\\.\\d+)?|-?\\.\\d+"); }
    private boolean isOperator(char c) { return c == '+' || c == '-' || c == '×' || c == '÷'; }
    private boolean isBinaryOperator(String token) { return token.length() == 1 && isOperator(token.charAt(0)); }
    private int precedence(String op) { return ("×".equals(op) || "÷".equals(op)) ? 2 : 1; }
    private String format(BigDecimal value) { return value.stripTrailingZeros().toPlainString(); }
}
