plugins {
    application
}

application {
    mainClassName = "il.ac.technion.cs.softwaredesign.MainKt"
}

val junitVersion: String? by extra
val hamkrestVersion: String? by extra
val guiceVersion: String? by extra
val kotlinGuiceVersion: String? by extra
val externalLibraryVersion: String? by extra
val mockkVersion: String? by extra

dependencies {
    implementation(project(":library"))
    implementation("il.ac.technion.cs.softwaredesign", "primitive-storage-layer", externalLibraryVersion)

    implementation("com.google.inject", "guice", guiceVersion)
    implementation("dev.misfitlabs.kotlinguice4", "kotlin-guice", kotlinGuiceVersion)

    testImplementation("org.junit.jupiter", "junit-jupiter-api", junitVersion)
    testImplementation("org.junit.jupiter", "junit-jupiter-params", junitVersion)
    testImplementation("com.natpryce", "hamkrest", hamkrestVersion)
    implementation("io.mockk", "mockk", mockkVersion) //TODO: CHANGE TO testImplementation
}
