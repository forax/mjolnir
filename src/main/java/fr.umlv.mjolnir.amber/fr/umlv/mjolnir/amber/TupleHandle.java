package fr.umlv.mjolnir.amber;

import static java.lang.invoke.MethodHandles.explicitCastArguments;
import static java.lang.invoke.MethodHandles.filterArguments;
import static java.lang.invoke.MethodHandles.filterReturnValue;
import static java.lang.invoke.MethodHandles.identity;
import static java.lang.invoke.MethodHandles.permuteArguments;
import static java.lang.invoke.MethodType.methodType;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

public final class TupleHandle {
  private static final ConcurrentHashMap<Form, TupleHandle> FORM_CACHE = new ConcurrentHashMap<>();
  
  private final MethodType type;
  private final MethodHandle constructor;
  private final MethodHandle[] components;
  
  TupleHandle(MethodType type, MethodHandle constructor, MethodHandle[] components) {
    this.type = type;
    this.constructor = constructor;
    this.components = components;
  }
  
  public MethodType type() {
    return type;
  }
  
  public Form form() {
    return Form.of(type);
  }
  
  public MethodHandle constructor() {
    return constructor;
  }
  
  public MethodHandle component(int index) {
    return components[index];
  }
  
  public static TupleHandle create(Class<?>... parameterTypes) {
    return create(MethodType.methodType(void.class, parameterTypes));
  }
  
  public static TupleHandle create(MethodType type) {
    return createImpl(Form.of(type), type);
  }
  
  static TupleHandle createImpl(Form form, MethodType type) {
    if (type.returnType() != void.class) {
      throw new IllegalArgumentException();
    }
    TupleHandle handle = FORM_CACHE.get(form);
    if (handle == null) {
      handle = TupleGenerator.generate(form);
      FORM_CACHE.putIfAbsent(form, handle);
    }
    return handle.adapt(type, form);
  }
  
  private static MethodType erase(MethodType methodType) {
    return methodType(methodType.returnType(),
        methodType.parameterList().stream()
        .map(type -> type.isPrimitive()? long.class: Object.class)
        .toArray(Class<?>[]::new));
  }
  
  private static Object[] values(Object value, int length) {
    return IntStream.range(0, length).mapToObj(__ -> value).toArray();
  }
  private static Class<?>[] classes(Class<?> value, int length) {
    return IntStream.range(0, length).mapToObj(__ -> value).toArray(Class<?>[]::new);
  }
  
  private TupleHandle adapt(MethodType type, Form form) {
    if (this.type.equals(type)) {
      return this;
    }
    
    int objects = 0;
    int prims = 0;
    List<Class<?>> parameters = type.parameterList();
    int length = parameters.size();
    int[] reorder = new int[form.objects + form.prims];
    MethodHandle[] cs = new MethodHandle[length];
    for(int i = 0; i < length; i++) {
      Class<?> parameter  = parameters.get(i);
      int index = parameter.isPrimitive()? form.objects + prims++: objects++;
      MethodHandle c = components[index];
      cs[i] = narrow(c, parameter);
      reorder[index] = i;
    }
    
    // need to fill the holes (objects & prims) for the constructor
    int hole = length;
    for(int i = objects; i < form.objects; i++) {
      reorder[i] = hole++;
    }
    for(int i = form.objects + prims; i < form.objects + form.prims; i++) {
      reorder[i] = hole++;
    }
    
    MethodType consType = type.changeReturnType(Object.class);
    MethodHandle cons = constructor;
    MethodType permutedType = erase(consType);
    if (objects < form.objects) {
      permutedType = permutedType.appendParameterTypes(classes(Object.class, form.objects - objects));
    }
    if (prims < form.prims) {
      permutedType = permutedType.appendParameterTypes(classes(long.class, form.prims - prims));
    }
    cons = permuteArguments(cons, permutedType, reorder);
    if (objects < form.objects) {
      cons = MethodHandles.insertArguments(cons, length, values(null, form.objects - objects));
    }
    if (prims < form.prims) {
      cons = MethodHandles.insertArguments(cons, length, values(0L, form.prims - prims));
    }
    cons = widen(cons, consType);
    
    return new TupleHandle(type, cons, cs);
  }
  
  private static final Map<Class<?>, MethodHandle> NARROWER_MAP;
  private static final Map<Class<?>, MethodHandle> WIDENER_MAP;
  static {
    MethodHandle longToDouble, intToFloat, doubleToLong, floatToInt;
    Lookup publicLookup = MethodHandles.publicLookup();
    try {
      longToDouble = publicLookup.findStatic(Double.class, "longBitsToDouble", methodType(double.class, long.class));
      intToFloat = publicLookup.findStatic(Float.class, "intBitsToFloat", methodType(float.class, int.class));
      doubleToLong = publicLookup.findStatic(Double.class, "doubleToRawLongBits", methodType(long.class, double.class));
      floatToInt = publicLookup.findStatic(Float.class, "floatToRawIntBits", methodType(int.class, float.class));
    } catch (IllegalAccessException | NoSuchMethodException e) {
      throw (LinkageError)new LinkageError().initCause(e);
    }
    
    MethodHandle IDENTITY = identity(long.class);
    
    NARROWER_MAP =
      Map.of(boolean.class, explicitCastArguments(IDENTITY, methodType(boolean.class, long.class)),
             byte.class,    explicitCastArguments(IDENTITY, methodType(byte.class, long.class)),
             short.class,   explicitCastArguments(IDENTITY, methodType(short.class, long.class)),
             char.class,    explicitCastArguments(IDENTITY, methodType(char.class, long.class)),
             int.class,     explicitCastArguments(IDENTITY, methodType(int.class, long.class)),
             float.class,   filterReturnValue(explicitCastArguments(IDENTITY, methodType(int.class, long.class)), intToFloat),
             double.class,  longToDouble
            );
    WIDENER_MAP =
      Map.of(boolean.class, explicitCastArguments(IDENTITY, methodType(long.class, boolean.class)),
             byte.class,    IDENTITY.asType(methodType(long.class, byte.class)),
             short.class,   IDENTITY.asType(methodType(long.class, short.class)),
             char.class,    IDENTITY.asType(methodType(long.class, char.class)),
             int.class,     IDENTITY.asType(methodType(long.class, int.class)),
             float.class,   filterReturnValue(floatToInt, IDENTITY.asType(methodType(long.class, int.class))),
             double.class,  doubleToLong
            );
  }
  
  private static MethodHandle narrow(MethodHandle mh, Class<?> type) {
    if (type == long.class || type == Object.class) {
      return mh;
    }
    if (!type.isPrimitive()) {
      return mh.asType(methodType(type, Object.class));
    }
    return filterReturnValue(mh, NARROWER_MAP.get(type));
  }
  private static MethodHandle widen(MethodHandle mh, MethodType type) {
    MethodHandle[] filters = new MethodHandle[type.parameterCount()];
    Arrays.setAll(filters, i -> widener(type.parameterType(i)));
    return filterArguments(mh, 0, filters).asType(type);
  }
  private static MethodHandle widener(Class<?> type) {
    if (type == long.class || type == Object.class) {
      return null;  // no-op
    }
    if (!type.isPrimitive()) {
      return null; // no-op, will do a asType at the end
    }
    return WIDENER_MAP.get(type);
  }
  
  public static class Form {
    final int objects;
    final int prims;
    
    private Form(int objects, int prims) {
      this.objects = objects;
      this.prims = prims;
    }
    
    @Override
    public int hashCode() {
      return objects ^ prims;
    }
    
    @Override
    public boolean equals(Object o) {
      if (!(o instanceof Form)) {
        return false;
      }
      Form form = (Form)o;
      return objects == form.objects && prims == form.prims;
    }
    
    @Override
    public String toString() {
      return "Form " + objects + ':' + prims;
    }
    
    private Form accumulate(Class<?> type) {
      return type.isPrimitive()? new Form(objects, prims + 1): new Form(objects + 1, prims);
    }
    
    public Form or(Form form) {
      return new Form(Math.max(objects, form.objects), Math.max(prims, form.prims));
    }
    
    public Form and(Form form) {
      return new Form(objects + form.objects, prims + form.prims);
    }
    
    boolean isAssignableFrom(Form form) {
      return objects >= form.objects && prims >= form.prims;
    }
    
    MethodType asMethodType() {
      Class<?>[] parameters = new Class<?>[objects + prims];
      for(int i = 0; i < objects; i++) {
        parameters[i] = Object.class;
      }
      for(int i = 0; i < prims; i++) {
        parameters[objects + i] = long.class;
      }
      return MethodType.methodType(void.class, parameters);
    }
    
    public TupleHandle createAs(MethodType type) {
      if (!isAssignableFrom(Form.of(type))) {
        throw new IllegalArgumentException("form " + this + " can not contains " + type);
      }
      return createImpl(this, type);
    }
    
    public static Form of(MethodType methodType) {
      if (methodType.returnType() != void.class) {
        throw new IllegalArgumentException();
      }
      return methodType.parameterList().stream()
          .reduce(new Form(0, 0), Form::accumulate, (_1, _2) -> { throw new AssertionError(); });
    }
  }
}
