import static com.github.forax.pro.Pro.*
import static com.github.forax.pro.builder.Builders.*

pro.loglevel("verbose")
pro.exitOnError(true)

resolver.remoteRepositories(list(
    uri("https://oss.sonatype.org/content/repositories/snapshots")
))
resolver.dependencies(list(
    // ASM API
    "org.objectweb.asm.all.debug=org.ow2.asm:asm-debug-all:6.0_BETA",
    
    // JUnit 5 API
    "org.opentest4j=org.opentest4j:opentest4j:1.0.0-M2",
    "org.junit.platform.commons=org.junit.platform:junit-platform-commons:1.0.0-M5",
    "org.junit.jupiter.api=org.junit.jupiter:junit-jupiter-api:5.0.0-M5"
))

compiler.lint("all,-varargs,-overloads")

packager.moduleMetadata(list(
    "fr.umlv.mjolnir@1.0/fr.umlv.mjolnir.bytecode.Rewriter",
    "fr.umlv.mjolnir.agent@1.0/fr.umlv.mjolnir.agent.Main",
    "fr.umlv.mjolnir.amber@1.0/fr.umlv.mjolnir.amber.Main"
))
packager.rawArguments(list(
    "--manifest=MANIFEST.MF"
))

// the runner will rewrite the bytecode when called
runner.module("fr.umlv.mjolnir")

tester.timeout(99)

// run the test once without bytecode modification and again after bytecode rewriting
run(resolver, modulefixer, compiler, packager, tester, runner, tester)


// test agent
runner.module("fr.umlv.mjolnir.agent")
runner.rawArguments(list(
    "-javaagent:target/main/artifact/fr.umlv.mjolnir.agent-1.0.jar"
))
run(runner)

/exit