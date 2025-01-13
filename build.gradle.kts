plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "6.1.0"
  kotlin("plugin.spring") version "2.0.21"
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

val awsSdkVersion = "1.12.777"
dependencies {
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:5.2.1")

  implementation("com.amazonaws:aws-java-sdk-s3:$awsSdkVersion")
  implementation("com.amazonaws:aws-java-sdk-sts:$awsSdkVersion")
  implementation("com.jayway.jsonpath:json-path:2.9.0")

  // Open API Documentation (swagger)
  // Must implement springdoc-openapi-starter-webmvc-api to support Kotlin https://springdoc.org/#kotlin-support
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.3")
  testImplementation("org.springdoc:springdoc-openapi-starter-webmvc-api:2.7.0")
  testImplementation("io.swagger.parser.v3:swagger-parser:2.1.24") {
    exclude(group = "io.swagger.core.v3")
  }
  testImplementation("io.jsonwebtoken:jjwt:0.12.6")
}

tasks {

  test {
    useJUnitPlatform()
    testLogging.showExceptions = true
    testLogging.showStackTraces = true
    exclude("**/*IntTest*")

    val failedTests = mutableListOf<TestDescriptor>()
    val skippedTests = mutableListOf<TestDescriptor>()

    // See https://github.com/gradle/kotlin-dsl/issues/836
    addTestListener(object : TestListener {
      override fun beforeSuite(suite: TestDescriptor) {}
      override fun beforeTest(testDescriptor: TestDescriptor) {}
      override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {
        when (result.resultType) {
          TestResult.ResultType.FAILURE -> failedTests.add(testDescriptor)
          TestResult.ResultType.SKIPPED -> skippedTests.add(testDescriptor)
          else -> null
        }
      }

      override fun afterSuite(suite: TestDescriptor, result: TestResult) {
        if (suite.parent == null) { // root suite
          logger.lifecycle("----")
          logger.lifecycle("Test result: ${result.resultType}")
          logger.lifecycle(
            "Test summary: ${result.testCount} tests, " +
              "${result.successfulTestCount} succeeded, " +
              "${result.failedTestCount} failed, " +
              "${result.skippedTestCount} skipped",
          )
          if (failedTests.isNotEmpty()) {
            logger.lifecycle("\tFailed Tests:")
            failedTests.forEach {
              parent?.let { parent ->
                logger.lifecycle("\t\t${parent.name} - ${it.name}")
              } ?: logger.lifecycle("\t\t${it.name}")
            }
          }

          if (skippedTests.isNotEmpty()) {
            logger.lifecycle("\tSkipped Tests:")
            skippedTests.forEach {
              parent?.let { parent ->
                logger.lifecycle("\t\t${parent.name} - ${it.name}")
              } ?: logger.lifecycle("\t\t${it.name}")
            }
          }
        }
      }
    })
  }
}

task<Test>("integrationTest") {
  description = "Runs the integration tests"
  group = "verification"
  testLogging.showExceptions = true
  testLogging.showStackTraces = true
  include("**/*IntTest*")
}

tasks.register<Copy>("installGitHooks") {
  from(layout.projectDirectory.dir("hooks"))
  into(layout.projectDirectory.dir(".git/hooks"))
}
