package fr.umlv.mjolnir.amber;

import static java.lang.invoke.MethodHandles.constant;
import static java.lang.invoke.MethodHandles.dropArguments;
import static java.lang.invoke.MethodHandles.guardWithTest;
import static java.lang.invoke.MethodHandles.identity;
import static java.lang.invoke.MethodHandles.insertArguments;
import static java.lang.invoke.MethodType.methodType;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Function;

import fr.umlv.mjolnir.amber.TupleHandle.Form;

public class Pattern {
  private final Form form;
  private final Function<Form, MethodHandle> mh;
  
  Pattern(Form form, Function<Form, MethodHandle> mh) {
    this.form = form;
    this.mh = mh;
  }

  public Form form() {
    return form;
  }
  
  public MethodHandle create() {
    //System.out.println("create mh: form " + form);
    return mh.apply(form);
  }
  
  private static MethodHandle noMatchMH(Form uberForm) {
    Object gotoDefault;
    try {
      gotoDefault = uberForm.createAs(methodType(void.class, int.class, boolean.class)).constructor().invokeExact(-1, false);
    } catch (Throwable e) {
      throw new AssertionError(e);
    } 
    return dropArguments(constant(Object.class, gotoDefault), 0, Object.class);
  }
  
  public static Pattern noMatch() {
    Form form = Form.of(methodType(void.class, int.class, boolean.class));
    return new Pattern(form, Pattern::noMatchMH);
  }

  public Pattern or(Pattern match) {
    return new Pattern(form.or(match.form),
        uberForm -> MethodHandles.permuteArguments(
                      MethodHandles.filterArguments(
                          guardWithTest(
                            dropArguments(
                              uberForm.createAs(methodType(void.class, int.class, boolean.class)).component(1),
                              0, Object.class),
                            dropArguments(identity(Object.class), 0, Object.class),
                            dropArguments(match.mh.apply(uberForm), 1, Object.class)),
                        1, mh.apply(uberForm)),
                      methodType(Object.class, Object.class), new int[] { 0, 0}));
  }

  public static Pattern match(Lookup lookup, Class<?> type, int selector) {
    Method deconstructor = Arrays.stream(type.getMethods())
          .filter(m -> m.isAnnotationPresent(Deconstruct.class))
          .findFirst()
          .get();  // feel lucky ?
    
    Deconstruct deconstruct = deconstructor.getAnnotation(Deconstruct.class);
    MethodHandle target;
    try {
      target = lookup.unreflect(deconstructor)
                     .asType(methodType(Object.class, Object.class, MethodHandle.class));
    } catch (IllegalAccessException e) {
      throw (LinkageError)new LinkageError().initCause(e);
    }
    MethodType methodType = PatternMatchingMetaFactory.type(deconstruct.value());
    
    //System.out.println("match " + type + " " + methodType);
    //System.out.println("  deconstructor " + target);
    
    return new Pattern(Form.of(methodType),
        uberForm -> guardWithTest(
                    IS_INSTANCE.bindTo(type),
                    insertArguments(target, 1, insertArguments(uberForm.createAs(methodType).constructor(), 0, selector)),
                    noMatchMH(uberForm)));
  }
  
  private static MethodHandle IS_INSTANCE;
  static {
    try {
      IS_INSTANCE = MethodHandles.lookup().findVirtual(Class.class, "isInstance", methodType(boolean.class, Object.class));
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new AssertionError(e);
    }
  }
}