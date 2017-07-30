package fr.umlv.mjolnir.util.stream;

import static java.lang.invoke.MethodHandles.dropArguments;
import static java.lang.invoke.MethodHandles.zero;
import static java.lang.invoke.MethodType.methodType;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class LoopBuilder {
  private final ArrayList<MethodHandle[]> clauses;
  private final ArrayList<Class<?>> types;
  
  LoopBuilder(ArrayList<MethodHandle[]> clauses, ArrayList<Class<?>> types) {
    this.clauses = clauses;
    this.types = types;
  }
  
  private static final MethodHandle ITERATOR, HAS_NEXT, NEXT, TEST, APPLY, ACCEPT;
  static {
    Lookup lookup = MethodHandles.publicLookup();
    try {
      ITERATOR = lookup.findVirtual(Collection.class, "iterator", methodType(Iterator.class));
      HAS_NEXT = lookup.findVirtual(Iterator.class, "hasNext", methodType(boolean.class));
      NEXT = lookup.findVirtual(Iterator.class, "next", methodType(Object.class));
      TEST = lookup.findVirtual(Predicate.class, "test", methodType(boolean.class, Object.class));
      APPLY = lookup.findVirtual(Function.class, "apply", methodType(Object.class, Object.class));
      ACCEPT = lookup.findVirtual(Consumer.class, "accept", methodType(void.class, Object.class));
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new AssertionError();
    }
  }
  
  public static LoopBuilder create() {
    ArrayList<MethodHandle[]> clauses = new ArrayList<>();
    ArrayList<Class<?>> types = new ArrayList<>();
    
    clauses.add(new MethodHandle[] { ITERATOR, null, HAS_NEXT });
    clauses.add(new MethodHandle[] { null, NEXT });
    types.add(Iterator.class);
    
    return new LoopBuilder(clauses, types);
  }

  public void filter(Predicate<?> predicate) {
    clauses.add(new MethodHandle[] { null, null, dropArguments(TEST.bindTo(predicate), 0, types), zero(void.class) });
  }
  
  public void map(Function<?, ?> mapper) {
    clauses.add(new MethodHandle[] { null, dropArguments(APPLY.bindTo(mapper), 0, types) });
    types.add(Object.class);
  }
  
  public MethodHandle forEach(Consumer<?> consumer) {
    clauses.add(new MethodHandle[] { null, dropArguments(ACCEPT.bindTo(consumer), 0, types) });
    
    return MethodHandles.loop(clauses.toArray(new MethodHandle[0][]));
  }
  
  public static void main(String[] args) throws Throwable {
    /*
    MethodHandle iterator, hasNext, next, test, apply, accept;
    Lookup lookup = MethodHandles.publicLookup();
    try {
      iterator = lookup.findVirtual(Collection.class, "iterator", methodType(Iterator.class));
      hasNext = lookup.findVirtual(Iterator.class, "hasNext", methodType(boolean.class));
      next = lookup.findVirtual(Iterator.class, "next", methodType(Object.class));
      test = lookup.findVirtual(Predicate.class, "test", methodType(boolean.class, Object.class));
      apply = lookup.findVirtual(Function.class, "apply", methodType(Object.class, Object.class));
      accept = lookup.findVirtual(Consumer.class, "accept", methodType(void.class, Object.class));
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new AssertionError();
    }
    
    MethodHandle[] clause0 = new MethodHandle[] { iterator, null, hasNext };
    MethodHandle[] clause1 = new MethodHandle[] { null, next };
    MethodHandle[] clause2 = new MethodHandle[] { null, MethodHandles.dropArguments(
        apply.bindTo((Function<String, String>)x -> x + " !"),
        0, Iterator.class)};
    MethodHandle[] clause3 = new MethodHandle[] { null, null, MethodHandles.dropArguments(
        test.bindTo(((Predicate<String>)x -> true)),
        0, Iterator.class, Object.class),
        MethodHandles.zero(void.class) };
    MethodHandle[] clause4 = new MethodHandle[] { null, MethodHandles.dropArguments(
        accept.bindTo((Consumer<Object>)System.out::println),
        0, Iterator.class, Object.class)};
    
    MethodHandle loop = MethodHandles.loop(clause0, clause1, clause2, clause3, clause4);
    loop.invokeExact((Collection<?>)List.of("a", "b"));
    */
    
    LoopBuilder builder = LoopBuilder.create();
    builder.map(x -> x +  " !");
    builder.filter(x -> true);
    MethodHandle loop = builder.forEach(System.out::println);
    loop.invokeExact((Collection<?>)List.of("a", "b"));
  }
}