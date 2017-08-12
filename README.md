# Mjolnir
Thor Hammer and secondarily a way to express invokedynamic in Java

[![Mjolnir build status](https://api.travis-ci.org/forax/mjolnir.svg?branch=master)](https://travis-ci.org/forax/mjolnir)

## Goal
   Mjolnir is a Java class allowing to initialize a stable value by calling a bootstrap method once. 
 
   The implementation is optimized so the stable value is very cheap to get.
   A bytecode rewriter is provided to replace the access to the stable value by an invokedynamic making the call even cheaper (mostly free).
   
   Mjolnir has the following properties:
   - Mjolnir.get().invokeExact() is semantically equivalent to an invokedynamic
   - initialize the constant with a bootstrap method
     no bootstrap call in the fast path
   - no boxing of arguments
   - no static analysis requires for the bytecode rewriter
     - crawling the bytecode is enough
   - should work without the bytecode rewriter (for testing)
 
 ## Video
 
   [![Me presenting Mjolnir at JVM Summit 2017](https://img.youtube.com/vi/Rco7hcOM7Ig/0.jpg)](https://www.youtube.com/embed/Rco7hcOM7Ig?list=PLX8CzqL3ArzXJ2EGftrmz4SzS6NRr6p2n "Me presenting Mjolnir at JVM Summit 2017")
 
 ## Examples
 
   The following example implements the equivalent of the macro__LINE__ i.e. it returns the current line number like in C
   ```java
     private static int boostrap(Lookup lookup) {
       String className = lookup.lookupClass().getName();
       int lineNumber = StackWalker.getInstance()
           .walk(s -> s.skip(1).filter(f -> f.getClassName().equals(className)).findFirst())
           .get()
           .getLineNumber();
       return lineNumber;
     }
  
     public static void main(String[] args) {
       int __LINE__ = Mjolnir.get(lookup -> boostrap(lookup));
     }
   ```

   This mechanism can be used to express an invokedynamic in Java, the bootstrap method can return a MethodHandle
   that will be called with invokeExact.
   
   ```java
     private static String hello(String name) {
       return "Hello " + name;
     }
     
     private static MethodHandle initHello(Lookup lookup) throws NoSuchMethodException, IllegalAccessException {
       Class<?> declaringClass = lookup.lookupClass();
       return lookup.findStatic(declaringClass, "hello", methodType(String.class, String.class));
     }
  
     public static void main(String[] args)  hello() throws Throwable {
       String result = (String)Mjolnir.get(lookup -> initHello(lookup)).invokeExact("Mjolnir");
       System.out.println(result);   // Hello Mjolnir
     }
   ```
   