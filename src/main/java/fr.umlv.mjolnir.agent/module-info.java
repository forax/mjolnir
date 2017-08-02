open module fr.umlv.mjolnir.agent {
  //requires jdk.attach;
  requires java.instrument;
  requires transitive fr.umlv.mjolnir;
  
  exports fr.umlv.mjolnir.agent;
  
  provides fr.umlv.mjolnir.AgentFacade with fr.umlv.mjolnir.agent.AgentFacadeImpl;
}