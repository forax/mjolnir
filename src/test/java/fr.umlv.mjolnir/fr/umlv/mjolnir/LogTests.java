package fr.umlv.mjolnir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import fr.umlv.mjolnir.log.Log;

@SuppressWarnings("static-method")
public class LogTests {
  @Test
  void simple() throws Throwable {
    class Test1 {
      void m() {
        Log.log(() -> "simple");
      }
    }
    boolean[] printed = new boolean[] { false };
    Log.config(Test1.class).outputer(msg -> {
      assertEquals("simple", msg, "wrong message");
      printed[0] = true;
    }).commit();
    new Test1().m();
    assertTrue(printed[0], "not printed");
  }
  
  @Test
  void disable() throws Throwable {
    class Test2 {
      void m() {
        Log.log(() -> { fail("should not be called");  return null; });
      }
    }
    Log.config(Test2.class).enable(false).commit();
    new Test2().m();
  }
}
