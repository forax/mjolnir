package fr.umlv.mjolnir.log;

import static java.lang.invoke.MethodHandles.empty;
import static java.lang.invoke.MethodType.methodType;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MutableCallSite;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import fr.umlv.mjolnir.Mjolnir;
import fr.umlv.mjolnir.OverrideEntryPoint;

public class Log {
  public interface LogConfig {
    LogConfig enable(boolean enable);
    LogConfig outputer(Consumer<? super String> outputer);
    void commit();
  }
  
  static final class Info extends MutableCallSite {
    private boolean enable;
    private Consumer<? super String> outputer;
    private final Object lock = new Object();
    
    private static final MethodHandle DO_LOG;
    static {
      try {
        DO_LOG = MethodHandles.lookup().findStatic(Info.class, "doLog", methodType(void.class, Consumer.class, Supplier.class));
      } catch (NoSuchMethodException | IllegalAccessException e) {
        throw new AssertionError(e);
      }
    }
    
    @SuppressWarnings("unused")
    private static void doLog(Consumer<? super String> consumer, Supplier<String> message) {
      consumer.accept(message.get());
    }
    
    private static MethodHandle handle(boolean enable, Consumer<? super String> outputer) {
      return (enable)? DO_LOG.bindTo(outputer): empty(methodType(void.class, Supplier.class));
    }
    
    Info(boolean enable, Consumer<? super String> outputer) {
      super(methodType(void.class, Supplier.class));
      this.enable = enable;
      this.outputer = outputer;
      setTarget(handle(enable, outputer));
    }
    
    void configure(boolean enable, Consumer<? super String> outputer) {
      //System.out.println("configure " + enable + " " + outputer);
      synchronized(lock) {
        if (enable == this.enable && outputer == this.outputer) {
          return;
        }
        this.enable = enable;
        this.outputer = outputer;
        setTarget(handle(enable, outputer));
      }
    }
    
    void populate(LogConfig config) {
      synchronized(lock) {
        config.enable(enable).outputer(outputer);  
      }
    }
  }
  
  private static final ClassValue<Info> INFOS = new ClassValue<>() {
    @Override
    protected Info computeValue(Class<?> arg0) {
      return new Info(true /* enable */, System.out::println);
    }
  };
  
  public static LogConfig config(Class<?> type) {
    Info info = INFOS.get(type);
    return new LogConfig() {
      private Consumer<? super String> outputer;
      private boolean enable;
      
      {
        info.populate(this);
      }
      
      @Override
      public LogConfig outputer(Consumer<? super String> outputer) {
        this.outputer = outputer;
        return this;
      }
      
      @Override
      public LogConfig enable(boolean enable) {
        this.enable = enable;
        return this;
      }
      
      @Override
      public void commit() {
        info.configure(enable, outputer);
      }
    };
  }
  
  private static MethodHandle init(Lookup lookup) {
    //System.out.println("log init " + lookup.lookupClass());
    return INFOS.get(lookup.lookupClass()).dynamicInvoker();
  }
  
  @OverrideEntryPoint
  public static void log(Supplier<String> message) {
    try {
      Mjolnir.override(message, Log::init).invokeExact(message);
    } catch(RuntimeException | Error e) {
      throw e;
    } catch (Throwable e) {
      throw new UndeclaredThrowableException(e);
    }
  }
}
