package fr.umlv.mjolnir.log;

import static java.lang.invoke.MethodType.methodType;

import java.lang.StackWalker.StackFrame;
import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.StringConcatException;
import java.lang.invoke.StringConcatFactory;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;

public class OldLog {  
  public static void log(String format, Object... args) {
    StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
    StackFrame frame = walker.walk(s -> s.skip(1).findFirst()).get();
    Class<?> declaringClass = frame.getDeclaringClass();
    String location = declaringClass.getName() + '.' + frame.getMethodName() + '(' + frame.getFileName() + ':' + frame.getLineNumber() + ')';
    
    ArrayList<Class<?>> types = new ArrayList<>();
    StringBuilder builder = new StringBuilder();
    
    for(int i = 0; i < format.length(); i++) {
      char c = format.charAt(i);
      if (c == '%') {
        switch(format.charAt(++i)) {
        case 's':
          types.add(String.class);
          builder.append('\u0001');
          continue;
        case 'i':
          types.add(int.class);
          builder.append('\u0001');
          continue;
        default:
          throw new IllegalArgumentException("invalid format " + format);
        }
      } else {
        builder.append(c);
      }
    }
    
    builder.append(" at ").append(location);
    String recipe = builder.toString();
    
    
    Lookup lookup = MethodHandles.lookup();
    CallSite callSite;
    try {
      callSite = StringConcatFactory.makeConcatWithConstants(lookup, "concat", methodType(String.class, types), recipe);
    } catch (StringConcatException e) {
      throw new AssertionError(e);
    }
    
    String message;
    try {
      message =  (String)callSite.getTarget().invokeWithArguments(args);
    } catch (RuntimeException|Error e) {
      throw e;
    } catch(Throwable e) {
      throw new UndeclaredThrowableException(e);
    }
    
    
    
    System.out.println(message);
  }
  
  public static void main(String[] args) {
    log("foo");
    log("foo %s bar %i", "toto", 3);
  }
}
