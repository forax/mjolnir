package fr.umlv.mjolnir.amber;

import static fr.umlv.mjolnir.Mjolnir.get;
import static fr.umlv.mjolnir.amber.PatternMatchingMetaFactory.component;
import static fr.umlv.mjolnir.amber.PatternMatchingMetaFactory.condy;
import static fr.umlv.mjolnir.amber.PatternMatchingMetaFactory.indy;
import static fr.umlv.mjolnir.amber.PatternMatchingMetaFactory.type;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.invoke.MethodHandle;

import org.junit.jupiter.api.Test;

import fr.umlv.mjolnir.Mjolnir.Bootstrap;

@SuppressWarnings("static-method")
public class PatternMatchingTests {
  public static class Point {
    private final int x;
    private final int y;
    
    public Point(int x, int y) {
      this.x = x;
      this.y = y;
    }

    @Deconstruct({ int.class, int.class })
    public Object deconstructor(MethodHandle carrier) throws Throwable {
      if (x == y) {
        return carrier.invokeExact(false, 0, 0);  // reject
      }
      return carrier.invokeExact(true, x, y);
    }
  }
  
  public static class User {
    private final String name;
    private final int age;
    
    public User(String message, int age) {
      this.name = message;
      this.age = age;
    }

    @Deconstruct({ String.class, int.class })
    public Object deconstructor(MethodHandle carrier) throws Throwable {
      return carrier.invokeExact(true, name, age);
    }
  }
  
  private static String match(Object o) throws Throwable {
    Bootstrap<Pattern> condyLocation = lookup -> condy(lookup, Point.class, User.class);
    Object carrier = get(lookup -> indy(lookup, get(condyLocation))).invokeExact(o);
    switch((int)get(lookup -> component(lookup, type(), 0, get(condyLocation))).invokeExact(carrier)) {
    case 0: {
      int x = (int)get(lookup -> component(lookup, type(int.class, int.class), 2, get(condyLocation))).invokeExact(carrier);
      int y = (int)get(lookup -> component(lookup, type(int.class, int.class), 3, get(condyLocation))).invokeExact(carrier);
      return "Point " + x + ' ' + y;
    }
    case 1: {
      String name = (String)get(lookup -> component(lookup, type(String.class), 2, get(condyLocation))).invokeExact(carrier);
      return "User " + name;
    }
    default:
      return "no match";
    }
  }
  
  @Test
  void test() throws Throwable {
    assertEquals("Point 99 747", match(new Point(99, 747)));
    assertEquals("User bob", match(new User("bob", 18)));
    
    assertEquals("no match", match(new Point(67, 67)));
  }
}
