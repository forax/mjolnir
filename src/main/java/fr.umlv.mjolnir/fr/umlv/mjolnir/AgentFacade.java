package fr.umlv.mjolnir;

public interface AgentFacade {
  void rewriteIfPossible(Class<?> declaringClass);
  void addReads(Module source, Module destination);
  void addOpens(Module source, String packaze, Module destination);
}
