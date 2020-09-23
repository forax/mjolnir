import static com.github.forax.pro.Pro.*
import static com.github.forax.pro.builder.Builders.*

pro.loglevel("verbose")
pro.exitOnError(true)

resolver.
    checkForUpdate(true).
    dependencies(
        // ASM 9
        "org.objectweb.asm=org.ow2.asm:asm:9.0",
        "org.objectweb.asm.util=org.ow2.asm:asm-util:9.0",
        "org.objectweb.asm.tree=org.ow2.asm:asm-tree:9.0",
        "org.objectweb.asm.tree.analysis=org.ow2.asm:asm-analysis:9.0",
    
        // JUnit 5
        "org.junit.jupiter.api=org.junit.jupiter:junit-jupiter-api:5.7.0",
        "org.junit.platform.commons=org.junit.platform:junit-platform-commons:1.7.0",
        "org.apiguardian.api=org.apiguardian:apiguardian-api:1.1.0",
        "org.opentest4j=org.opentest4j:opentest4j:1.2.0"
    )

compiler.lint("all,-varargs,-overloads")

packager.moduleMetadata(
    "fr.umlv.mjolnir@1.0/fr.umlv.mjolnir.bytecode.Rewriter",
    "fr.umlv.mjolnir.agent@1.0/fr.umlv.mjolnir.agent.Main",
    "fr.umlv.mjolnir.amber@1.0/fr.umlv.mjolnir.amber.Main"
)
packager.rawArguments(
    "--manifest=MANIFEST.MF"
)

// the runner will rewrite the bytecode when called
runner.module("fr.umlv.mjolnir")

tester.timeout(99)

// run the test once without bytecode modification and again after bytecode rewriting
run(resolver, modulefixer, compiler, packager, tester, runner, tester)


// test agent
runner.module("fr.umlv.mjolnir.agent")
runner.rawArguments(
    "-javaagent:target/main/artifact/fr.umlv.mjolnir.agent-1.0.jar"
)
run(runner)

/exit