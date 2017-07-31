import static com.github.forax.pro.Pro.*

set("pro.loglevel", "verbose")
set("pro.exitOnError", true)

set("resolver.remoteRepositories", list(
    uri("https://oss.sonatype.org/content/repositories/snapshots")
))
set("resolver.dependencies", list(
    // ASM API
    "org.objectweb.asm.all.debug=org.ow2.asm:asm:6.0_BETA",
    
    // JUnit API
    "org.opentest4j=org.opentest4j:opentest4j:1.0.0-M2",
    "org.junit.platform.commons=org.junit.platform:junit-platform-commons:1.0.0-M5",
    "org.junit.jupiter.api=org.junit.jupiter:junit-jupiter-api:5.0.0-M5"
))

set("compiler.lint", "all,-varargs,-overloads")

set("packager.moduleMetadata", list(
    "fr.umlv.mjolnir@1.0/fr.umlv.mjolnir.bytecode.Rewriter"
))

//set("runner.module", "fr.umlv.mjolnir");

set("tester.timeout", 99)

run("resolver", "modulefixer", "compiler", "packager", "tester", "runner", "tester")

/exit