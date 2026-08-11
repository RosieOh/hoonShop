package com.hoonshop.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 아키텍처 규칙을 테스트로 강제합니다.
 *
 * <p>문서에 "도메인은 프레임워크를 몰라야 한다"고 적어두는 것만으로는 지켜지지 않습니다.
 * 급할 때 누군가 도메인에 {@code @Autowired}를 하나 붙이고, 리뷰에서 놓치고, 6개월 뒤에는
 * 도메인이 Spring 없이는 테스트조차 안 되는 상태가 됩니다. 규칙은 CI가 지켜야 합니다.
 */
@DisplayName("아키텍처 규칙")
class LayerDependencyTest {

    private static final String BASE = "com.hoonshop";

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE);
    }

    @Test
    @DisplayName("도메인은 인프라를 모른다")
    void domainMustNotDependOnInfrastructure() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage("..infrastructure..");
        rule.check(classes);
    }

    @Test
    @DisplayName("도메인은 프레젠테이션을 모른다")
    void domainMustNotDependOnPresentation() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage("..presentation..");
        rule.check(classes);
    }

    @Test
    @DisplayName("도메인은 애플리케이션 계층을 모른다 — 의존 방향은 항상 안쪽으로")
    void domainMustNotDependOnApplication() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage("..application..");
        rule.check(classes);
    }

    @Test
    @DisplayName("도메인은 Spring에 의존하지 않는다 — 순수 자바로 테스트할 수 있어야 한다")
    void domainMustNotDependOnSpring() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage("org.springframework..");
        rule.check(classes);
    }

    @Test
    @DisplayName("도메인은 다른 바운디드 컨텍스트의 내부를 모른다 (공유 커널 제외)")
    void boundedContextsMustNotLeakIntoEachOther() {
        String[] contexts = {"catalog", "identity", "order", "payment", "promotion"};

        for (String context : contexts) {
            String[] others = java.util.Arrays.stream(contexts)
                    .filter(c -> !c.equals(context))
                    .map(c -> BASE + "." + c + "..")
                    .toArray(String[]::new);

            ArchRule rule = noClasses()
                    .that().resideInAPackage(BASE + "." + context + ".domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(others)
                    .because("컨텍스트 간 연결은 포트/어댑터(infrastructure)와 도메인 이벤트로만 합니다");

            rule.check(classes);
        }
    }

    @Test
    @DisplayName("프레젠테이션은 저장소를 직접 만지지 않는다 — 반드시 애플리케이션 서비스를 거친다")
    void presentationMustNotUseRepositoriesDirectly() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..presentation..")
                .should().dependOnClassesThat().haveSimpleNameEndingWith("JpaRepository");
        rule.check(classes);
    }
}
