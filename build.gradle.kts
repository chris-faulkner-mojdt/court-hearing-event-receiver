plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "3.3.8"
  kotlin("plugin.spring") version "1.5.10"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("com.amazonaws:aws-java-sdk-sns:1.11.1024")
  implementation("com.amazonaws:aws-java-sdk-sqs:1.11.1024")

  implementation("io.springfox:springfox-swagger2:2.9.2")
  implementation("io.springfox:springfox-swagger-ui:2.9.2")

  testImplementation("io.jsonwebtoken:jjwt:0.9.1")
}

tasks {
  compileKotlin {
    kotlinOptions {
      jvmTarget = "16"
    }
  }

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
              "${result.skippedTestCount} skipped"
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
