import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.7.2"
    id("io.spring.dependency-management") version "1.0.13.RELEASE"
    kotlin("jvm") version "1.6.21"
    kotlin("plugin.spring") version "1.7.10"

    /**
     * detekt
     *
     * URL
     * - https://github.com/detekt/detekt
     * GradlePlugins
     * - https://plugins.gradle.org/plugin/io.gitlab.arturbosch.detekt
     * Main用途
     * - Linter/Formatter
     * Sub用途
     * - 無し
     * 概要
     * KotlinのLinter/Formatter
     */
    id("io.gitlab.arturbosch.detekt") version "1.21.0"

    /**
     * dokka
     *
     * URL
     * - https://github.com/Kotlin/dokka
     * GradlePlugins
     * - https://plugins.gradle.org/plugin/org.jetbrains.dokka
     * Main用途
     * - ドキュメント生成
     * Sub用途
     * - 特になし
     * 概要
     * - JDocの代替(=KDoc)
     */
    id("org.jetbrains.dokka") version "1.7.10"

    /**
     * jacoco
     *
     * URL
     * - https://docs.gradle.org/current/userguide/jacoco_plugin.html
     * GitHub
     * - https://github.com/gradle/gradle/blob/master/subprojects/jacoco/src/main/java/org/gradle/testing/jacoco/plugins/JacocoPlugin.java
     * Main用途
     * - コードカバレッジを出すためのツール
     * Sub用途
     * - 特になし
     * 概要
     * - Gradle公式?が管理しているっぽい
     * - versionは jacoco {} の中で指定する
     */
    jacoco
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

    /**
     * Arrow Core
     *
     * URL
     * - https://arrow-kt.io/docs/core/
     * MavenCentral
     * - https://mvnrepository.com/artifact/io.arrow-kt/arrow-core
     * Main用途
     * - Either/Validatedを使ったRailway Oriented Programming
     * Sub用途
     * - Optionを使ったletの代替
     * 概要
     * - Kotlinで関数型プログラミングをするときに便利なライブラリ
     */
    implementation("io.arrow-kt:arrow-core:1.1.2")

    /**
     * AssertJ
     *
     * URL
     * - https://assertj.github.io/doc/
     * MavenCentral
     * - https://mvnrepository.com/artifact/org.assertj/assertj-core
     * Main用途
     * - JUnitでassertThat(xxx).isEqualTo(yyy)みたいな感じで比較時に使う
     * Sub用途
     * - 特になし
     * 概要
     * - JUnit等を直感的に利用するためのライブラリ
     */
    testImplementation("org.assertj:assertj-core:3.23.1")

    /**
     * jqwik
     *
     * URL
     * - https://jqwik.net/
     * MavenCentral
     * - https://mvnrepository.com/artifact/net.jqwik/jqwik
     * - https://mvnrepository.com/artifact/net.jqwik/jqwik-kotlin
     * Main用途
     * - Property Based Testing(pbt)
     * 概要
     * - Property Based Testingをするのに便利なライブラリ
     * 参考
     * - https://medium.com/criteo-engineering/introduction-to-property-based-testing-f5236229d237
     * - https://johanneslink.net/property-based-testing-in-kotlin/#jqwiks-kotlin-support
     */
    testImplementation("net.jqwik:jqwik:1.6.5")
    testImplementation("net.jqwik:jqwik-kotlin:1.6.5")

    /**
     * Springdoc Openapi
     *
     * URL
     * - https://springdoc.org/
     * MavenCentral
     * - https://mvnrepository.com/artifact/org.springdoc/springdoc-openapi-ui
     * - https://mvnrepository.com/artifact/org.springdoc/springdoc-openapi-webmvc-core
     * Main用途
     * - SpringBootとswagger-uiの統合
     * 概要
     * - SpringBootのサーバ起動するとswagger-uiでOpenAPIが見える
     * - http://localhost:8080/swagger-ui.html でswagger uiが見える
     * - http://localhost:8080/api-docs でjsonのOpenAPI
     * - http://localhost:8080/api-docs.yaml でyaml版
     */
    implementation("org.springdoc:springdoc-openapi-ui:1.6.9")
    implementation("org.springdoc:springdoc-openapi-webmvc-core:1.6.9")

    /**
     * Java JWT
     *
     * URL
     * - https://github.com/auth0/java-jwt
     * MavenCentral
     * - https://mvnrepository.com/artifact/com.auth0/java-jwt
     * Main用途
     * - JWTでセッションIDっぽく振る舞わせる
     * 概要
     * - 特になし
     */
    implementation("com.auth0:java-jwt:4.0.0")

    /**
     * Spring JDBC
     *
     * URL
     * - https://spring.pleiades.io/spring-framework/docs/current/javadoc-api/org/springframework/jdbc/core/package-summary.html
     * MavenCentral
     * - https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-starter-jdbc
     * Main用途
     * - DBへ保存
     * 概要
     * - 特になし
     *
     * これを入れるだけで、application.properties/yamlや@ConfigurationによるDB接続設定が必要になる
     */
    implementation("org.springframework.boot:spring-boot-starter-jdbc")

    /**
     * postgresql
     *
     * URL
     * - https://jdbc.postgresql.org/
     * MavenCentral
     * - https://mvnrepository.com/artifact/org.postgresql/postgresql
     * Main用途
     * - DBつなぐ時のドライバ
     * 概要
     * - 特になし
     */
    implementation("org.postgresql:postgresql")

    /**
     * dokkaHtmlPlugin
     *
     * dokka Pluginを適用するのに必要
     */
    dokkaHtmlPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:1.7.10")

    /**
     * Database Rider
     *
     * - Rider Core
     * - Rider Spring
     * - Rider JUnit 5
     *
     * URL
     * - https://database-rider.github.io/database-rider/
     * MavenCentral
     * - https://mvnrepository.com/artifact/com.github.database-rider/rider-core
     * - https://mvnrepository.com/artifact/com.github.database-rider/rider-spring
     * - https://mvnrepository.com/artifact/com.github.database-rider/rider-junit5
     * Main用途
     * - JUnitでDB周りのテスト時のヘルパー
     * 概要
     * - テーブルの事前条件、事後条件を簡潔に設定できる
     */
    val dbRiderVersion = "1.33.0"
    implementation("com.github.database-rider:rider-core:$dbRiderVersion")
    implementation("com.github.database-rider:rider-spring:$dbRiderVersion")
    testImplementation("com.github.database-rider:rider-junit5:$dbRiderVersion")

    /**
     * detektの拡張: detekt-formatting
     *
     * 概要
     * - formattingのルール
     * - 基本はktlintと同じ
     * - format自動適用オプションの autoCorrect が使えるようになる
     */
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.21.0")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}

/**
 * ./gradlew test
 */
tasks.withType<Test> {
    useJUnitPlatform {
        excludeTags("WithLocalDb")
    }
    this.testLogging {
        /**
         * Test時に標準出力を出力させる
         */
        this.showStandardStreams = true
    }
    /**
     * testが終わった後にカバレッジレポートを出す
     */
    finalizedBy(tasks.jacocoTestReport)
}
tasks.jacocoTestReport {
    /**
     * jacocoTestReportは必ずテスト終了後に作成するようにする(依存させる)
     */
    dependsOn(tasks.test)
}
jacoco {
    toolVersion = "0.8.8"
}

/**
 * ./gradlew test withLocalDb
 */
task<Test>("withLocalDb") {
    useJUnitPlatform {
        includeTags("WithLocalDb")
    }
    this.testLogging {
        /**
         * Test時に標準出力を出力させる
         */
        this.showStandardStreams = true
    }
}

/**
 * detektの設定
 *
 * 基本的に全て `detekt-override.yml` で設定する
 */
detekt {
    /**
     * ./gradlew detektGenerateConfig でdetekt.ymlが生成される(バージョンが上がる度に再生成する)
     */
    config = files(
        "$projectDir/config/detekt/detekt.yml",
        "$projectDir/config/detekt/detekt-override.yml",
    )
}
