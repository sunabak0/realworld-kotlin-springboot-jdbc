package com.example.realworldkotlinspringbootjdbc.arch

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeJars
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.TestInstance

/**
 * 層の依存関係のテスト
 *
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AnalyzeClasses(
    packages = ["com.example.realworldkotlinspringbootjdbc"],
    importOptions = [
        DoNotIncludeTests::class, DoNotIncludeJars::class
    ],
)
class LayerTest {
    companion object {
        private const val DOMAIN_PACKAGE = "..domain.."
        private const val USECASE_PACKAGE = "..usecase.."
        private const val PRESENTATION_PACKAGE = "..presentation.."
        private const val INFRA_PACKAGE = "..infra.."
    }

    @ArchTest
    fun `ドメイン層はプレゼンテーション層、インフラ層、ユースケース層を参照しない`(importClasses: JavaClasses) {
        noClasses()
            .that()
            .resideInAPackage(DOMAIN_PACKAGE)
            .should()
            .accessClassesThat()
            .resideInAnyPackage(USECASE_PACKAGE, PRESENTATION_PACKAGE, INFRA_PACKAGE)
            .check(importClasses)
    }

    @ArchTest
    fun `ユースケース層はプレゼンテーション層を参照しない`(importClasses: JavaClasses) {
        noClasses()
            .that()
            .resideInAPackage(USECASE_PACKAGE)
            .should()
            .accessClassesThat()
            .resideInAPackage(PRESENTATION_PACKAGE)
            .check(importClasses)
    }
}
