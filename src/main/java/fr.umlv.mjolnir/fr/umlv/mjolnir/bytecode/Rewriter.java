package fr.umlv.mjolnir.bytecode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class Rewriter {
  static final Handle BSM = new Handle(Opcodes.H_INVOKESTATIC, "fr/umlv/mjolnir/Mjolnir", "bsm",
      "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;", false);
  
  public static byte[] rewrite(InputStream input, Function<String, Optional<InputStream>> classFileFinder) throws IOException {
    AnnotationOracle annotationOracle = new AnnotationOracle(classFileFinder);
    ClassReader reader = new ClassReader(input);
    ClassWriter writer = new ClassWriter(reader, 0);
    reader.accept(new ClassVisitor(Opcodes.ASM7, writer) {
      @Override
      public MethodVisitor visitMethod(int access, String methodName, String methodDesc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, methodName, methodDesc, signature, exceptions);
        return new MethodVisitor(Opcodes.ASM7, mv) {
          @Override
          public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            // rewrite calls to Mjolnir.get()
            if (opcode == Opcodes.INVOKESTATIC &&
                itf == false &&
                owner.equals("fr/umlv/mjolnir/Mjolnir") &&
                desc.equals("(Lfr/umlv/mjolnir/Mjolnir$Bootstrap;)Ljava/lang/Object;") &&
                name.equals("get")) {
              super.visitInvokeDynamicInsn("get", desc, BSM);
              
              System.out.println("rewrite get: " + reader.getClassName() + '.' + methodName + methodDesc);
              return;
            }
            
            // rewrite calls to a method marked with @OverrideEntryPoint
            if (annotationOracle.isAnOverrideEntryPoint(owner, name, desc)) {
              String newDesc = desc;
              if (opcode != Opcodes.INVOKESTATIC) {
                newDesc = "(L" + owner + ';' + desc.substring(1);  // prepend the receiver type
              }
              
              super.visitInvokeDynamicInsn("override", newDesc, BSM);
              System.out.println("rewrite override: " + reader.getClassName() + '.' + methodName + methodDesc);
              return;
            }
            
            super.visitMethodInsn(opcode, owner, name, desc, itf);
          }
        };
      }
    }, 0);
    return writer.toByteArray();
  }
  
  private static Optional<InputStream> findClassFile(ZipFile input, String className) {
    ZipEntry entry = new ZipEntry(className + ".class");
    try {
      return Optional.ofNullable(input.getInputStream(entry));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
  
  public static void main(String[] args) throws IOException {
    Path path = Paths.get("target/test/artifact/test-fr.umlv.mjolnir-1.0.jar");
    Path outputPath = Files.createTempFile("", ".jar");
    
    System.out.println("rewrite " + path + " to " + outputPath);
    
    
    try(ZipFile input = new ZipFile(path.toFile());
        OutputStream fileOutput = Files.newOutputStream(outputPath);
        ZipOutputStream jarOutput = new ZipOutputStream(fileOutput)) {
      Function<String, Optional<InputStream>> classFileFinder = className -> findClassFile(input, className);
      for(ZipEntry entry: Collections.list(input.entries())) {
        ZipEntry newEntry = new ZipEntry(entry.getName());
        jarOutput.putNextEntry(newEntry);
        try(InputStream entryStream = input.getInputStream(entry)) {
          if (entry.getName().endsWith(".class")) {
            jarOutput.write(rewrite(entryStream, classFileFinder));
          } else {
            entryStream.transferTo(jarOutput);
          }
        }
      }
    }
    
    System.out.println("move " + outputPath + " to " + path);
    
    Files.move(outputPath, path, StandardCopyOption.REPLACE_EXISTING);
  }
}
