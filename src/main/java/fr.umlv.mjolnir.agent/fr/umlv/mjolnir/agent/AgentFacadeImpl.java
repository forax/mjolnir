package fr.umlv.mjolnir.agent;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import fr.umlv.mjolnir.AgentFacade;
import fr.umlv.mjolnir.bytecode.Rewriter;

public class AgentFacadeImpl implements AgentFacade {
  private Instrumentation checkInstrumentation() {
    Instrumentation instrumentation = Agent.instrumentation;
    if (instrumentation == null) {
      throw new IllegalStateException("no instrumentation");
    }
    return instrumentation;
  }
  
  @Override
  public void rewriteIfPossible(Class<?> declaringClass) throws IllegalStateException {
    Instrumentation instrumentation = checkInstrumentation();
    
    /*
    byte[] bytecode;
    ClassLoader loader = declaringClass.getClassLoader();
    try(InputStream input = loader.getResourceAsStream(declaringClass.getName().replace('.', '/') + ".class")) {
      if (input == null) {
        throw new IllegalStateException("no input");
      }

      Function<String, Optional<InputStream>> classFileFinder = internalName -> Optional.ofNullable(loader.getResourceAsStream(internalName + ".class"));
      bytecode = Rewriter.rewrite(input, classFileFinder);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
    
    try {
      instrumentation.redefineClasses(new ClassDefinition(declaringClass, bytecode));
    } catch (ClassNotFoundException | UnmodifiableClassException e) {
      throw new IllegalStateException(e);
    }*/
    
    try {
      instrumentation.retransformClasses(declaringClass);
    } catch (UnmodifiableClassException e) {
      throw new IllegalStateException(e);
    }
  }
  
  @Override
  public void addReads(Module source, Module destination) throws IllegalStateException {
    Instrumentation instrumentation = checkInstrumentation();
    instrumentation.redefineModule(source, Set.of(destination), Map.of(), Map.of(), Set.of(), Map.of());
  }
  
  @Override
  public void addOpens(Module source, String packaze, Module destination) {
    Instrumentation instrumentation = checkInstrumentation();
    instrumentation.redefineModule(source, Set.of(), Map.of(), Map.of(packaze, Set.of(destination)), Set.of(), Map.of());
  }
}
