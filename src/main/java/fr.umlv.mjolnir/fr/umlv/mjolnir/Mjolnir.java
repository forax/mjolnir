package fr.umlv.mjolnir;

import static java.lang.invoke.MethodHandles.constant;
import static java.lang.invoke.MethodHandles.dropArguments;
import static java.lang.invoke.MethodHandles.lookup;
import static java.lang.invoke.MethodType.methodType;

import java.lang.StackWalker.Option;
import java.lang.StackWalker.StackFrame;
import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.util.Objects;
import java.util.ServiceLoader;

public class Mjolnir {
  static final AgentFacade AGENT_FACADE;
  static {
    AGENT_FACADE = ServiceLoader.load(AgentFacade.class).findFirst().orElse(null);
  }
 
  private static final ClassValue<Object> CONSTS_GET = getConstantValue(0);
  private static final ClassValue<Object> CONSTS_OVERRIDE = getConstantValue(1);
      
  static final Lookup PRIVATE_LOOKUP = MethodHandles.lookup();
  static final StackWalker WALKER = StackWalker.getInstance(Option.RETAIN_CLASS_REFERENCE);
  static final ThreadLocal<Bootstrap<?>> BOOTSTRAP_LOCAL = new ThreadLocal<>();
  static final ThreadLocal<Lookup> LOOKUP_LOCAL = new ThreadLocal<>();
  
  private static ClassValue<Object> getConstantValue(int skipStacks) {
    return new ClassValue<>() {
      @Override
      protected Object computeValue(Class<?> type) {
        if (type.getDeclaredFields().length != 0) {
          throw new IllegalStateException("the bootstrap lambda should not capture any values");
        }
        
        Lookup lookup = LOOKUP_LOCAL.get();
        try {
          if (lookup == null) {
            StackFrame frame = WALKER
                .walk(s -> s.filter(Mjolnir::filterOutMjolnirAndClassValueFrames).skip(skipStacks).findFirst()).get();
            //System.out.println(frame.getDeclaringClass() + "." + frame.getMethodName() + " bci: " + frame.getByteCodeIndex());
            Class<?> declaringClass = frame.getDeclaringClass();
            
            if (!Mjolnir.class.getModule().canRead(declaringClass.getModule())) {
              boolean rescue = true;
              if (AGENT_FACADE != null) {
                try {
                  AGENT_FACADE.addReads(Mjolnir.class.getModule(), declaringClass.getModule());
                  AGENT_FACADE.addOpens(declaringClass.getModule(), declaringClass.getPackage().getName(), Mjolnir.class.getModule());
                  rescue = false;
                } catch(IllegalStateException e) {
                  // do nothing
                }
                if (rescue) {
                  Mjolnir.class.getModule().addReads(declaringClass.getModule());
                }
              }
            }
            lookup = MethodHandles.privateLookupIn(declaringClass, PRIVATE_LOOKUP);
            
            if (AGENT_FACADE != null) {
              try {
                AGENT_FACADE.rewriteIfPossible(declaringClass);
              } catch(IllegalStateException e) {
                System.err.println(e);
              }
            }
          }
          return BOOTSTRAP_LOCAL.get().bootstrap(lookup);
        } catch (Exception e) {
          e.printStackTrace(System.err);
          throw (LinkageError)new LinkageError().initCause(e);
        }
      }
    };
  }
  
  static boolean filterOutMjolnirAndClassValueFrames(StackFrame f) {
    String className = f.getClassName();
    return !className.equals("fr.umlv.mjolnir.Mjolnir") &&
           !className.startsWith("java.lang.") &&
           !className.startsWith("fr.umlv.mjolnir.Mjolnir$");
  }
  
  public interface Bootstrap<T> {
    public T bootstrap(Lookup lookup) throws Exception;
  }
  
  @SuppressWarnings("unchecked")
  public static <T> T get(Bootstrap<? extends T> bootstrap) {
    // equivalent to a call to findConstant(null, bootstrap, CONSTS_GET, bootstrap);
    // inlined for perf
    
    BOOTSTRAP_LOCAL.set(bootstrap);
    try {
      return (T)CONSTS_GET.get(bootstrap.getClass());
    } finally {
      BOOTSTRAP_LOCAL.set(null);  // don't keep bootstrap for too long
    }
  }
  
  @SuppressWarnings("unchecked")
  public static <T> T override(Object location, Bootstrap<? extends T> bootstrap) {
    // equivalent to a call to findConstant(null, location, CONSTS_OVERRIDE, bootstrap);
    // inlined for perf
    
    BOOTSTRAP_LOCAL.set(bootstrap);
    try {
      return (T)CONSTS_OVERRIDE.get(location.getClass());
    } finally {
      BOOTSTRAP_LOCAL.set(null);  // don't keep bootstrap for too long
    }
  }
  
  @SuppressWarnings("unchecked")
  static <T> T findConstant(Lookup lookup, Object location, ClassValue<Object> constants, Bootstrap<? extends T> bootstrap) {
    Objects.requireNonNull(lookup);
    Objects.requireNonNull(location);
    Objects.requireNonNull(constants);
    Objects.requireNonNull(bootstrap);
    BOOTSTRAP_LOCAL.set(bootstrap);
    LOOKUP_LOCAL.set(lookup);
    try {
      return (T)constants.get(location.getClass());
    } finally {
      BOOTSTRAP_LOCAL.set(null);  // don't keep bootstrap for too long
      LOOKUP_LOCAL.set(null);     // reset to null, expected by get() and override()
    }
  }
  
  private static class CS extends MutableCallSite {
    private static final MethodHandle INIT;
    static {
      try {
        INIT = lookup().findVirtual(CS.class, "init", methodType(Object.class, Lookup.class, Bootstrap.class));
      } catch (NoSuchMethodException | IllegalAccessException e) {
        throw new AssertionError(e);
      }
    }

    private final ClassValue<Object> constants;
    
    public CS(Lookup lookup, ClassValue<Object> constants, MethodType type) {
      super(type);
      this.constants = constants;
      setTarget(INIT.bindTo(this).bindTo(lookup));
    }
    
    @SuppressWarnings("unused")
    private <T> T init(Lookup lookup, Bootstrap<? extends T> bootstrap) {
      T value = findConstant(lookup, bootstrap, constants, bootstrap);
      setTarget(dropArguments(constant(Object.class, value), 0, Bootstrap.class));
      return value;
    }
  }
  
  public static CallSite bsm(Lookup lookup, String method, MethodType type) {
    return new CS(lookup, method.equals("get")? CONSTS_GET: CONSTS_OVERRIDE, type);
  }
}
