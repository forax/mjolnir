package fr.umlv.mjolnir.agent;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.Optional;
import java.util.function.Function;

import fr.umlv.mjolnir.bytecode.Rewriter;

//import fr.umlv.mjolnir.bytecode.Rewriter;

class Agent {
  static Instrumentation instrumentation;
  
  public static void premain(String agentArgs, Instrumentation instrumentation) {
  
  //public static void agentmain(String agentArgs, Instrumentation instrumentation) {
    //System.out.println("agent started");
    
    
    Agent.instrumentation = instrumentation;
    
    instrumentation.addTransformer(new ClassFileTransformer() {
      @Override
      public byte[] transform(Module module, ClassLoader loader, String className, Class<?> classBeingRedefined,
          ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        
        //System.out.println("transform " + className + " " + classBeingRedefined);
        
        if (classBeingRedefined == null) {  // do not rewrite too early
          return null;
        }
        
        Function<String, Optional<InputStream>> classFileFinder = internalName -> Optional.ofNullable(loader.getResourceAsStream(internalName + ".class"));
        try {
          return Rewriter.rewrite(new ByteArrayInputStream(classfileBuffer), classFileFinder);
        } catch (IOException e) {
          throw (IllegalClassFormatException)new IllegalClassFormatException().initCause(e);
        }
      }
    }, true);
  }
}
