package fr.umlv.mjolnir;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
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
  
  private static byte[] rewrite(InputStream input) throws IOException {
    ClassReader reader = new ClassReader(input);
    ClassWriter writer = new ClassWriter(reader, 0);
    reader.accept(new ClassVisitor(Opcodes.ASM6, writer) {
      @Override
      public MethodVisitor visitMethod(int access, String methodName, String methodDesc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, methodName, methodDesc, signature, exceptions);
        return new MethodVisitor(Opcodes.ASM6, mv) {
          @Override
          public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            if (opcode == Opcodes.INVOKESTATIC &&
                itf == false &&
                owner.equals("fr/umlv/mjolnir/Mjolnir") &&
                desc.equals("(Lfr/umlv/mjolnir/Mjolnir$Bootstrap;)Ljava/lang/Object;") &&
                name.equals("get")) {
              super.visitInvokeDynamicInsn(name, desc, BSM);
              
              System.out.println("rewrite in " + reader.getClassName() + '.' + methodName + methodDesc);
              return;
            }
            
            super.visitMethodInsn(opcode, owner, name, desc, itf);
          }
        };
      }
    }, 0);
    return writer.toByteArray();
  }
  
  public static void main(String[] args) throws IOException {
    Path path = Paths.get("target/test/artifact/test-fr.umlv.mjolnir-1.0.jar");
    Path outputPath = Files.createTempFile("", ".jar");
    
    System.out.println("rewrite " + path + " to " + outputPath);
    
    try(ZipFile input = new ZipFile(path.toFile());
        OutputStream fileOutput = Files.newOutputStream(outputPath);
        ZipOutputStream jarOutput = new ZipOutputStream(fileOutput)) {
      for(ZipEntry entry: Collections.list(input.entries())) {
        ZipEntry newEntry = new ZipEntry(entry.getName());
        jarOutput.putNextEntry(newEntry);
        try(InputStream entryStream = input.getInputStream(entry)) {
          if (entry.getName().endsWith(".class")) {
            jarOutput.write(rewrite(entryStream));
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
