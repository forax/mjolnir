module fr.umlv.mjolnir {
  requires org.objectweb.asm.all.debug;
  
  exports fr.umlv.mjolnir;
  exports fr.umlv.mjolnir.bytecode;
  
  uses fr.umlv.mjolnir.AgentFacade;
}