package fr.umlv.mjolnir;

import static java.lang.invoke.MethodType.methodType;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
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
  
  @SuppressWarnings("unused")
  private static int incr(int value) {
    return value + 1;
  }
  private static MethodHandle init(Lookup lookup) throws NoSuchMethodException, IllegalAccessException {
    return lookup.findStatic(lookup.lookupClass(), "incr", MethodType.methodType(int.class, int.class));
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
