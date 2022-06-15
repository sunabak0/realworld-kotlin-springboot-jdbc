import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.7.0"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    kotlin("jvm") version "1.6.21"
    kotlin("plugin.spring") version "1.6.21"

    //
    // ktlint
    //
    // URL
    // - https://github.com/JLLeitschuh/ktlint-gradle
    // GralePlugins
    // - https://plugins.gradle.org/plugin/org.jlleitschuh.gradle.ktlint
    // Main用途
    // - Linter/Formatter
    // Sub用途
    // - 無し
    // 概要
    // KotlinのLinter/Formatter
    //
    id("org.jlleitschuh.gradle.ktlint") version "10.3.0"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    //
    // Arrow Core
    //
    // URL
    // - https://arrow-kt.io/docs/core/
    // MavenCentral
    // - https://mvnrepository.com/artifact/io.arrow-kt/arrow-core
    // Main用途
    // - Either/Validatedを使ったRailway Oriented Programming
    // Sub用途
    // - Optionを使ったletの代替
    // 概要
    // - Kotlinで関数型プログラミングをするときに便利なライブラリ
    //
    implementation("io.arrow-kt:arrow-core:1.1.2")

    //
    // AssertJ
    //
    // URL
    // - https://assertj.github.io/doc/
    // MavenCentral
    // - https://mvnrepository.com/artifact/org.assertj/assertj-core
    // Main用途
    // - JUnitでassertThat(xxx).isEqualTo(yyy)みたいな感じで比較時に使う
    // Sub用途
    // - 特になし
    // 概要
    // - JUnit等を直感的に利用するためのライブラリ
    //
    testImplementation("org.assertj:assertj-core:3.23.1")

    //
    // jqwik
    //
    // URL
    // - https://jqwik.net/
    // MavenCentral
    // - https://mvnrepository.com/artifact/net.jqwik/jqwik
    // - https://mvnrepository.com/artifact/net.jqwik/jqwik-kotlin
    // Main用途
    // - Property Based Testing(pbt)
    // 概要
    // - Property Based Testingをするのに便利なライブラリ
    // 参考
    // - https://medium.com/criteo-engineering/introduction-to-property-based-testing-f5236229d237
    // - https://johanneslink.net/property-based-testing-in-kotlin/#jqwiks-kotlin-support
    //
    testImplementation("net.jqwik:jqwik:1.6.5")
    testImplementation("net.jqwik:jqwik-kotlin:1.6.5")

    //
    // Springdoc Openapi
    //
    // URL
    // - https://springdoc.org/
    // MavenCentral
    // - https://mvnrepository.com/artifact/org.springdoc/springdoc-openapi-ui
    // - https://mvnrepository.com/artifact/org.springdoc/springdoc-openapi-webmvc-core
    // Main用途
    // - SpringBootとswagger-uiの統合
    // 概要
    // - SpringBootのサーバ起動するとswagger-uiでOpenAPIが見える
    // - http://localhost:8080/swagger-ui.html でswagger uiが見える
    // - http://localhost:8080/api-docs でjsonのOpenAPI
    // - http://localhost:8080/api-docs.yaml でyaml版
    //
    implementation("org.springdoc:springdoc-openapi-ui:1.6.9")
    implementation("org.springdoc:springdoc-openapi-webmvc-core:1.6.9")

    //
    // Java JWT
    //
    // URL
    // - https://github.com/auth0/java-jwt
    // MavenCentral
    // - https://mvnrepository.com/artifact/com.auth0/java-jwt
    // Main用途
    // - JWTでセッションIDっぽく振る舞わせる
    // 概要
    // - 特になし
    implementation("com.auth0:java-jwt:3.19.0")

    //
    // Spring JDBC
    //
    // URL
    // - https://spring.pleiades.io/spring-framework/docs/current/javadoc-api/org/springframework/jdbc/core/package-summary.html
    // MavenCentral
    // - https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-starter-jdbc
    // Main用途
    // - DBへ保存
    // 概要
    // - 特になし
    //
    // これを入れるだけで、application.properties/yamlや@ConfigurationによるDB接続設定が必要になる
    implementation("org.springframework.boot:spring-boot-starter-jdbc")

    //
    // postgresql
    //
    // URL
    // - https://jdbc.postgresql.org/
    // MavenCentral
    // - https://mvnrepository.com/artifact/org.postgresql/postgresql
    // Main用途
    // - DBつなぐ時のドライバ
    // 概要
    // - 特になし
    //
    implementation("org.postgresql:postgresql")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}

//
// ./gradlew test
//
tasks.withType<Test> {
    useJUnitPlatform {
        excludeTags("WithLocalDb")
    }
    this.testLogging {
        // Test時に標準出力を出力させる
        this.showStandardStreams = true
    }
}

//
// ./gradlew test withLocalDb
//
task<Test>("withLocalDb") {
    useJUnitPlatform {
        includeTags("WithLocalDb")
    }
    this.testLogging {
        // Test時に標準出力を出力させる
        this.showStandardStreams = true
    }
}
