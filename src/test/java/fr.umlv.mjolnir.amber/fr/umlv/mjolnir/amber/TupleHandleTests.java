package fr.umlv.mjolnir.amber;

import static java.lang.invoke.MethodType.methodType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.StringConcatFactory;

import org.junit.jupiter.api.Test;

import fr.umlv.mjolnir.amber.TupleHandle.Form;

@SuppressWarnings("static-method")
public class TupleHandleTests {
  @Test
  void createSigInt() {
    TupleHandle handle = TupleHandle.create(int.class);
    assertEquals(methodType(void.class, int.class), handle.type());
  }
  
  @Test
  void createSigString() {
    TupleHandle handle = TupleHandle.create(String.class);
    assertEquals(methodType(void.class, String.class), handle.type());
  }
  
  @Test
  void createSigMixed() {
    TupleHandle handle = TupleHandle.create(String.class, int.class, boolean.class, String.class);
    assertEquals(methodType(void.class, String.class, int.class, boolean.class, String.class), handle.type());
  }
  
  @Test
  void create() throws Throwable {
    TupleHandle handle = TupleHandle.create(String.class, int.class, boolean.class, String.class);
    Object object = handle.constructor().invokeExact("foo", 3, true, "bar");
    assertEquals("foo", (String)handle.component(0).invokeExact(object));
    assertEquals(3, (int)handle.component(1).invokeExact(object));
    assertEquals(true, (boolean)handle.component(2).invokeExact(object));
    assertEquals("bar", (String)handle.component(3).invokeExact(object));
  }
  
  @Test
  void partial() throws Throwable {
    Form form = Form.of(methodType(void.class, int.class, boolean.class, String.class));
    TupleHandle handle = form.createAs(methodType(void.class, int.class, boolean.class));
    Object object = handle.constructor().invokeExact(3, true);
    assertEquals(3, (int)handle.component(0).invokeExact(object));
    assertEquals(true, (boolean)handle.component(1).invokeExact(object));
  }
  
  @Test
  void shareView() throws Throwable {
    try {
    MethodType type1 = methodType(void.class, int.class, double.class, Object.class);
    MethodType type2 = methodType(void.class, String.class, float.class, Object.class);
    Form form1 = Form.of(type1);
    Form form2 = Form.of(type2);
    Form form = form1.or(form2);
    TupleHandle handle1 = form.createAs(type1);
    TupleHandle handle2 = form.createAs(type2);
    
    Object object1 = handle1.constructor().invokeExact(7, 42.0, (Object)"hello");
    Object object2 = handle2.constructor().invokeExact("fuzz", 4f, (Object)null);
    assertSame(object1.getClass(), object2.getClass());
    } catch (Throwable e) {
      e.printStackTrace();
      throw e;
    }
  }
}
