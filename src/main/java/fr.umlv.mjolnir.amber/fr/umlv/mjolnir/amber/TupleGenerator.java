package fr.umlv.mjolnir.amber;

import static java.lang.invoke.MethodType.methodType;
import static org.objectweb.asm.ClassWriter.COMPUTE_MAXS;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.LLOAD;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_9;

import java.io.PrintWriter;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.util.ArrayList;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.util.CheckClassAdapter;

//import sun.misc.Unsafe;

import fr.umlv.mjolnir.amber.TupleHandle.Form;

class TupleGenerator {
  private static final Lookup LOOKUP = MethodHandles.lookup();
  private static final String CLASS_PREFIX = TupleHandle.class.getPackageName().replace('.', '/') + "/FORM";
  
  private static MethodHandle constructor(Class<?> clazz, MethodType methodType) {
    MethodHandle constructor;
    try {
      constructor = LOOKUP.findStatic(clazz, "create", methodType.changeReturnType(clazz));
    } catch (IllegalAccessException | NoSuchMethodException e) {
      throw (LinkageError)new LinkageError().initCause(e);
    }
    return constructor.asType(constructor.type().changeReturnType(Object.class));
  }
  
  private static MethodHandle[] getters(Class<?> clazz, Form form) {
    ArrayList<MethodHandle> mhs = new ArrayList<>();
    for(int i = 0; i < form.objects; i++) {
      mhs.add(getter(clazz, "objects$" + i, Object.class));
    }
    for(int i = 0; i < form.prims; i++) {
      mhs.add(getter(clazz, "prims$" + i, long.class));
    }
    return mhs.toArray(new MethodHandle[0]);
  }
  
  private static MethodHandle getter(Class<?> clazz, String name, Class<?> type) {
    MethodHandle getter;
    try {
      getter = LOOKUP.findGetter(clazz, name, type);
    } catch (IllegalAccessException | NoSuchFieldException e) {
      throw (LinkageError)new LinkageError().initCause(e);
    }
    return getter.asType(methodType(type, Object.class));
  }
  
  
  public static TupleHandle generate(Form form) {
    MethodType methodType = form.asMethodType();
    Class<?> clazz = generate(form, methodType);
    return new TupleHandle(methodType, constructor(clazz, methodType.changeReturnType(clazz)), getters(clazz, form));
  }
  
  private static Class<?> generate(Form form, MethodType methodType) {
    String className = CLASS_PREFIX + form.objects + '_' + form.prims;
    ClassWriter writer = new ClassWriter(COMPUTE_MAXS);
    writer.visit(V1_9, ACC_PUBLIC| ACC_SUPER, className, null, "java/lang/Object", null);
    
    for(int i = 0; i < form.objects; i++) {
      FieldVisitor fv = writer.visitField(ACC_PUBLIC|ACC_FINAL, "objects$" + i, "Ljava/lang/Object;", null, null);
      fv.visitEnd();
    }
    for(int i = 0; i < form.prims; i++) {
      FieldVisitor fv = writer.visitField(ACC_PUBLIC|ACC_FINAL, "prims$" + i, "J", null, null);
      fv.visitEnd();
    }
    
    String desc = methodType.toMethodDescriptorString();    
    {
      MethodVisitor mv = writer.visitMethod(ACC_PRIVATE, "<init>", desc, null, null);
      mv.visitCode();
      mv.visitVarInsn(ALOAD, 0);
      mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
      for(int i = 0; i < form.objects; i++) {
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, i + 1);
        mv.visitFieldInsn(PUTFIELD, className, "objects$" + i, "Ljava/lang/Object;");
      }
      for(int i = 0; i < form.prims; i++) {
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(LLOAD, form.objects + 1 + i * 2);
        mv.visitFieldInsn(PUTFIELD, className, "prims$" + i, "J");
      }
      mv.visitInsn(RETURN);
      mv.visitMaxs(-1, -1);
      mv.visitEnd();
    }
    
    String factoryDesc = desc.substring(0, desc.length() - 1) + 'L' + className + ';';  // replace V by className
    {  
      MethodVisitor mv = writer.visitMethod(ACC_PUBLIC|ACC_STATIC, "create", factoryDesc, null, null);
      mv.visitCode();
      mv.visitTypeInsn(NEW, className);
      mv.visitInsn(DUP);
      for(int i = 0; i < form.objects; i++) {
        mv.visitVarInsn(ALOAD, i);
      }
      for(int i = 0; i < form.prims; i++) {
        mv.visitVarInsn(LLOAD, form.objects + i * 2);
      }
      mv.visitMethodInsn(INVOKESPECIAL, className, "<init>", desc, false);
      mv.visitInsn(ARETURN);
      mv.visitMaxs(-1, -1);
      mv.visitEnd();
    }
    
    writer.visitEnd();
    
    byte[] code = writer.toByteArray();
    
    // DEBUG
    CheckClassAdapter.verify(new ClassReader(code), false, new PrintWriter(System.out));
    
    try {
      return LOOKUP.defineClass(code);
    } catch (IllegalAccessException e) {
      throw (LinkageError)new LinkageError().initCause(e);
    }
  }
}
