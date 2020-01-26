module fr.umlv.mjolnir {
  requires org.objectweb.asm;
  
  exports fr.umlv.mjolnir;
  exports fr.umlv.mjolnir.bytecode;
  
  uses fr.umlv.mjolnir.AgentFacade;
}