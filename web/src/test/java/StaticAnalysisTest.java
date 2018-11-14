import guru.nidi.codeassert.config.AnalyzerConfig;
import guru.nidi.codeassert.dependency.*;
import org.junit.Test;

import static guru.nidi.codeassert.junit.CodeAssertMatchers.hasNoCycles;
import static guru.nidi.codeassert.junit.CodeAssertMatchers.matchesRulesExactly;
import static org.hamcrest.MatcherAssert.assertThat;

public class StaticAnalysisTest {

    private final AnalyzerConfig config = AnalyzerConfig.maven().main();

    @Test
    public void noCycles() {
        assertThat(new DependencyAnalyzer(config).analyze(), hasNoCycles());
    }

    @Test
    public void dependency() {

        class BeGeoffreyXmlweave extends DependencyRuler {

            private DependencyRule ui, service, coreUsecase;

            @Override
            public void defineRules() {
                base().mustNotUse(base().allSubOf());
                service.mayUse(coreUsecase);
                ui.mayUse(coreUsecase);
            }
        }

        DependencyRules rules = DependencyRules
                .denyAll()
                .withRelativeRules(new BeGeoffreyXmlweave())
                .withExternals("java.*",
                        "com.vaadin.*",
                        "org.springframework.*");

        DependencyResult result = new DependencyAnalyzer(config)
                .rules(rules)
                .analyze();

        assertThat(result, matchesRulesExactly());
    }
}
