package fr.umlv.mjolnir.amber;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.stream.Stream;

import static java.lang.invoke.MethodType.methodType;

public class PatternMatchingMetaFactory {
  public static MethodHandle indy(Lookup lookup, Pattern pattern) {
    return pattern.create();
  }

  public static MethodHandle component(Lookup lookup, MethodType type, int index, Pattern pattern) {
    TupleHandle handle = pattern.form().createAs(type);
    return handle.component(index);
  } 

  public static Pattern condy(Lookup lookup, Class<?>... classes) {
    Pattern pattern = Pattern.noMatch();
    for(int i = 0; i < classes.length; i++) {
      Pattern match = Pattern.match(lookup, classes[i], i);
      pattern = pattern.or(match);
    }
    return pattern;
  }

  public static MethodType type(Class<?>... classes) {
    return methodType(void.class, Stream.concat(Stream.of(int.class, boolean.class), Arrays.stream(classes)).toArray(Class<?>[]::new));
  }
}