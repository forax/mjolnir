package fr.umlv.mjolnir.agent;

import java.util.function.IntUnaryOperator;

import fr.umlv.mjolnir.Mjolnir;
import fr.umlv.mjolnir.Mjolnir.Bootstrap;

public class Main {
  private static int incr(int i) {
    return Mjolnir.get((Bootstrap<IntUnaryOperator>)__ -> v -> v + 1).applyAsInt(i);
  }
  
  private static void loop() {
    int i = 0;
    while (i < 10_000_000) {
      i = incr(i);
    }
  }

  public static void main(String[] args) {
    String message = Mjolnir.get(lookup -> "Hello Mjolnir");
    System.out.println(message);
    
    loop();
  }
}
