package fr.umlv.mjolnir;

import static java.lang.invoke.MethodType.methodType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.StringConcatFactory;

import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
public class MjolnirTests {
  @Test
  void lookup() throws Throwable {
    Mjolnir.get(
        lookup -> {
          assertSame(MjolnirTests.class, lookup.lookupClass());
          assertTrue((lookup.lookupModes() & Lookup.PRIVATE) != 0);
          return null;
        });
  }
  
  @Test
  void constant() throws Throwable {
    int value = Mjolnir.get(__ -> 42);
    assertEquals(42, value);
  }
  
  @Test
  void sum() throws Throwable {
    int value = (int)Mjolnir.get(
        lookup -> lookup.findStatic(Integer.class, "sum", methodType(int.class, int.class, int.class))
        ).invokeExact(40, 2);
    assertEquals(42, value);
  }
  
  @Test
  void concat() throws Throwable {
    String s = (String)Mjolnir.get(lookup -> StringConcatFactory.
        makeConcatWithConstants(lookup,
           "concat",
           methodType(String.class, String.class),
           "Hello \u0001").dynamicInvoker())
     .invokeExact("Mjolnir");
    assertEquals("Hello Mjolnir", s);
  }
  
  private static int boostrapLine(Lookup lookup) {
    String className = lookup.lookupClass().getName();
    int lineNumber = StackWalker.getInstance()
        .walk(s -> s.skip(1).filter(f -> f.getClassName().equals(className)).findFirst())
        .get()
        .getLineNumber();
    return lineNumber;
  }
  @Test
  void line() throws Throwable {
    int line = Mjolnir.get(MjolnirTests::boostrapLine);
    assertEquals(61, line);  // this test may fail if you add more tests in front of this one !
  }
  
  private static String boostrapFile(Lookup lookup) {
    String className = lookup.lookupClass().getName();
    String filename = StackWalker.getInstance()
        .walk(s -> s.skip(1).filter(f -> f.getClassName().equals(className)).findFirst())
        .get()
        .getFileName();
    return filename;
  }
  @Test
  void filename() throws Throwable {
    String filename = Mjolnir.get(MjolnirTests::boostrapFile);
    assertEquals("MjolnirTests.java", filename);
  }
  
  @SuppressWarnings("unused")
  private static String hello(String name) {
    return "Hello " + name;
  }
  private static MethodHandle initHello(Lookup lookup) throws NoSuchMethodException, IllegalAccessException {
    return lookup.findStatic(lookup.lookupClass(), "hello", methodType(String.class, String.class));
  }
  @Test
  void hello() throws Throwable {
    String result = (String)Mjolnir.get(MjolnirTests::initHello).invokeExact("Mjolnir");
    assertEquals("Hello Mjolnir", result);
  }
  
  @SuppressWarnings("unused")
  private static int incr(int value) {
    return value + 1;
  }
  private static MethodHandle init(Lookup lookup) throws NoSuchMethodException, IllegalAccessException {
    return lookup.findStatic(lookup.lookupClass(), "incr", methodType(int.class, int.class));
  }
  @Test
  void loop() throws Throwable {
    int i = 0;
    while (i < 10_000_000) {
      i = (int)Mjolnir.get(MjolnirTests::init).invokeExact(i);
    }
    assertEquals(10_000_000, i);
  }
}
