package fr.umlv.mjolnir.util.stream;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public interface Stream<E> {
  public Stream<E> filter(Predicate<? super E> predicate);
  public <R> Stream<R> map(Function<? super E, ? extends R> mapper);
  public void forEach(Consumer<? super E> consumer);
  
  public static <E> Stream<E> from(Collection<E> collection) {
    return stream(collection, LoopBuilder.create());
  }
  
  // should be private but Eclipse generates an accessor with an illegal visibility
  // see https://bugs.eclipse.org/bugs/show_bug.cgi?id=518272
  /*private*/ static <E> Stream<E> stream(Collection<?> collection, LoopBuilder builder) {
    return new Stream<>() {
      @Override
      public Stream<E> filter(Predicate<? super E> predicate) {
        builder.filter(predicate);
        return stream(collection, builder);
      }
      @Override
      public <R> Stream<R> map(Function<? super E, ? extends R> mapper) {
        builder.map(mapper);
        return stream(collection, builder);
      }
      @Override
      public void forEach(Consumer<? super E> consumer) {
        try {
          builder.forEach(consumer).invokeExact(collection);
        } catch(RuntimeException|Error e) {
          throw e;
        } catch (Throwable e) {
          throw new UndeclaredThrowableException(e);
        }
      }
    };
  }
  
  public static void main(String[] args) {
    Stream.from(List.of("a", "b")).map(x -> x + "!").filter(x -> true).forEach(System.out::println);
  }
}
