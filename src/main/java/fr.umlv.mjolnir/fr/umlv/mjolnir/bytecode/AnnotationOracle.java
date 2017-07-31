package fr.umlv.mjolnir.bytecode;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import fr.umlv.mjolnir.OverrideEntryPoint;

class AnnotationOracle {
  static final String OVERRIDE_ENTRY_POINT_NAME = 'L' + OverrideEntryPoint.class.getName().replace('.', '/') + ';';
  
  private final HashMap<String,HashSet<String>> cache = new HashMap<>();
  private final Function<String, Optional<InputStream>> classFileFinder;
  
  public AnnotationOracle(Function<String, Optional<InputStream>> classFileFinder) {
    this.classFileFinder = Objects.requireNonNull(classFileFinder);
  }

  public boolean isAnOverrideEntryPoint(String className, String methodName, String methodDescriptor) {
    return cache.computeIfAbsent(className, this::analyzeClass).contains(methodName + methodDescriptor);
  }
  
  private HashSet<String> analyzeClass(String className) {
    HashSet<String> set = new HashSet<>();
    Optional<InputStream> classFileInputStream = classFileFinder.apply(className);
    if (!classFileInputStream.isPresent()) {
      return set;
    }
    try(InputStream input = classFileInputStream.get()) {
      ClassReader reader = new ClassReader(input);
      reader.accept(new ClassVisitor(Opcodes.ASM6) {
        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
          return new MethodVisitor(Opcodes.ASM6) {
            @Override
            public AnnotationVisitor visitAnnotation(String annotationDesc, boolean visible) {
              if (annotationDesc.equals(OVERRIDE_ENTRY_POINT_NAME)) {
                set.add(name + desc);
              }
              return null;
            }
          };
        }
      }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return set;
  }
}
