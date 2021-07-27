package jline.internal;

import java.io.IOException;
import java.io.Writer;
import java.util.Stack;

public class Curses {
  private static Object[] sv = new Object[26];
  
  private static Object[] dv = new Object[26];
  
  private static final int IFTE_NONE = 0;
  
  private static final int IFTE_IF = 1;
  
  private static final int IFTE_THEN = 2;
  
  private static final int IFTE_ELSE = 3;
  
  public static void tputs(Writer out, String str, Object... params) throws IOException {
    int index = 0;
    int length = str.length();
    int ifte = 0;
    boolean exec = true;
    Stack<Object> stack = new Stack();
    while (index < length) {
      int start;
      char ch = str.charAt(index++);
      switch (ch) {
        case '\\':
          ch = str.charAt(index++);
          if (ch >= '0' && ch <= '9')
            throw new UnsupportedOperationException(); 
          switch (ch) {
            case 'E':
            case 'e':
              if (exec)
                out.write(27); 
              continue;
            case 'n':
              out.write(10);
              continue;
            case 'r':
              if (exec)
                out.write(13); 
              continue;
            case 't':
              if (exec)
                out.write(9); 
              continue;
            case 'b':
              if (exec)
                out.write(8); 
              continue;
            case 'f':
              if (exec)
                out.write(12); 
              continue;
            case 's':
              if (exec)
                out.write(32); 
              continue;
            case ':':
            case '\\':
            case '^':
              if (exec)
                out.write(ch); 
              continue;
          } 
          throw new IllegalArgumentException();
        case '^':
          ch = str.charAt(index++);
          if (exec)
            out.write(ch - 64); 
          continue;
        case '%':
          ch = str.charAt(index++);
          switch (ch) {
            case '%':
              if (exec)
                out.write(37); 
              continue;
            case 'p':
              ch = str.charAt(index++);
              if (exec)
                stack.push(params[ch - 49]); 
              continue;
            case 'P':
              ch = str.charAt(index++);
              if (ch >= 'a' && ch <= 'z') {
                if (exec)
                  dv[ch - 97] = stack.pop(); 
                continue;
              } 
              if (ch >= 'A' && ch <= 'Z') {
                if (exec)
                  sv[ch - 65] = stack.pop(); 
                continue;
              } 
              throw new IllegalArgumentException();
            case 'g':
              ch = str.charAt(index++);
              if (ch >= 'a' && ch <= 'z') {
                if (exec)
                  stack.push(dv[ch - 97]); 
                continue;
              } 
              if (ch >= 'A' && ch <= 'Z') {
                if (exec)
                  stack.push(sv[ch - 65]); 
                continue;
              } 
              throw new IllegalArgumentException();
            case '\'':
              ch = str.charAt(index++);
              if (exec)
                stack.push(Integer.valueOf(ch)); 
              ch = str.charAt(index++);
              if (ch != '\'')
                throw new IllegalArgumentException(); 
              continue;
            case '{':
              start = index;
              while (str.charAt(index++) != '}');
              if (exec) {
                int v = Integer.valueOf(str.substring(start, index - 1)).intValue();
                stack.push(Integer.valueOf(v));
              } 
              continue;
            case 'l':
              if (exec)
                stack.push(Integer.valueOf(stack.pop().toString().length())); 
              continue;
            case '+':
              if (exec) {
                int v2 = toInteger(stack.pop());
                int v1 = toInteger(stack.pop());
                stack.push(Integer.valueOf(v1 + v2));
              } 
              continue;
            case '-':
              if (exec) {
                int v2 = toInteger(stack.pop());
                int v1 = toInteger(stack.pop());
                stack.push(Integer.valueOf(v1 - v2));
              } 
              continue;
            case '*':
              if (exec) {
                int v2 = toInteger(stack.pop());
                int v1 = toInteger(stack.pop());
                stack.push(Integer.valueOf(v1 * v2));
              } 
              continue;
            case '/':
              if (exec) {
                int v2 = toInteger(stack.pop());
                int v1 = toInteger(stack.pop());
                stack.push(Integer.valueOf(v1 / v2));
              } 
              continue;
            case 'm':
              if (exec) {
                int v2 = toInteger(stack.pop());
                int v1 = toInteger(stack.pop());
                stack.push(Integer.valueOf(v1 % v2));
              } 
              continue;
            case '&':
              if (exec) {
                int v2 = toInteger(stack.pop());
                int v1 = toInteger(stack.pop());
                stack.push(Integer.valueOf(v1 & v2));
              } 
              continue;
            case '|':
              if (exec) {
                int v2 = toInteger(stack.pop());
                int v1 = toInteger(stack.pop());
                stack.push(Integer.valueOf(v1 | v2));
              } 
              continue;
            case '^':
              if (exec) {
                int v2 = toInteger(stack.pop());
                int v1 = toInteger(stack.pop());
                stack.push(Integer.valueOf(v1 ^ v2));
              } 
              continue;
            case '=':
              if (exec) {
                int v2 = toInteger(stack.pop());
                int v1 = toInteger(stack.pop());
                stack.push(Boolean.valueOf((v1 == v2)));
              } 
              continue;
            case '>':
              if (exec) {
                int v2 = toInteger(stack.pop());
                int v1 = toInteger(stack.pop());
                stack.push(Boolean.valueOf((v1 > v2)));
              } 
              continue;
            case '<':
              if (exec) {
                int v2 = toInteger(stack.pop());
                int v1 = toInteger(stack.pop());
                stack.push(Boolean.valueOf((v1 < v2)));
              } 
              continue;
            case 'A':
              if (exec) {
                int v2 = toInteger(stack.pop());
                int v1 = toInteger(stack.pop());
                stack.push(Boolean.valueOf((v1 != 0 && v2 != 0)));
              } 
              continue;
            case '!':
              if (exec) {
                int v1 = toInteger(stack.pop());
                stack.push(Boolean.valueOf((v1 == 0)));
              } 
              continue;
            case '~':
              if (exec) {
                int v1 = toInteger(stack.pop());
                stack.push(Integer.valueOf(v1 ^ 0xFFFFFFFF));
              } 
              continue;
            case 'O':
              if (exec) {
                int v2 = toInteger(stack.pop());
                int v1 = toInteger(stack.pop());
                stack.push(Boolean.valueOf((v1 != 0 || v2 != 0)));
              } 
              continue;
            case '?':
              if (ifte != 0)
                throw new IllegalArgumentException(); 
              ifte = 1;
              continue;
            case 't':
              if (ifte != 1 && ifte != 3)
                throw new IllegalArgumentException(); 
              ifte = 2;
              exec = (toInteger(stack.pop()) != 0);
              continue;
            case 'e':
              if (ifte != 2)
                throw new IllegalArgumentException(); 
              ifte = 3;
              exec = !exec;
              continue;
            case ';':
              if (ifte == 0 || ifte == 1)
                throw new IllegalArgumentException(); 
              ifte = 0;
              exec = true;
              continue;
            case 'i':
              if (params.length >= 1)
                params[0] = Integer.valueOf(toInteger(params[0]) + 1); 
              if (params.length >= 2)
                params[1] = Integer.valueOf(toInteger(params[1]) + 1); 
              continue;
            case 'd':
              out.write(Integer.toString(toInteger(stack.pop())));
              continue;
          } 
          throw new UnsupportedOperationException();
      } 
      if (exec)
        out.write(ch); 
    } 
  }
  
  private static int toInteger(Object pop) {
    if (pop instanceof Number)
      return ((Number)pop).intValue(); 
    if (pop instanceof Boolean)
      return ((Boolean)pop).booleanValue() ? 1 : 0; 
    return Integer.valueOf(pop.toString()).intValue();
  }
}
