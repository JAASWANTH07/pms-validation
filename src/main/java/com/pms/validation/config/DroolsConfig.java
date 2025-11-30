package com.pms.validation.config;

import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.builder.Message;
import org.kie.api.runtime.KieContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;

@Configuration
public class DroolsConfig {

    private static final String RULES_LOCATION = "rules/trade-validation.drl";

    @Bean
    public KieContainer kieContainer() {
        try {
            KieServices ks = KieServices.Factory.get();
            KieFileSystem kfs = ks.newKieFileSystem();

            kfs.write(ks.getResources().newClassPathResource(RULES_LOCATION, StandardCharsets.UTF_8.name()));

            KieBuilder kb = ks.newKieBuilder(kfs);
            kb.buildAll();

            if (kb.getResults().hasMessages(Message.Level.ERROR)) {
                String errors = kb.getResults().getMessages().stream()
                        .filter(m -> m.getLevel() == Message.Level.ERROR)
                        .map(Message::toString)
                        .reduce("", (a, b) -> a + "\n" + b);
                throw new RuntimeException("DRL compilation errors:\n" + errors);
            }

            KieModule kModule = kb.getKieModule();
            return ks.newKieContainer(kModule.getReleaseId());
        } catch (Exception ex) {
            throw new RuntimeException("Failed to initialize Drools rules engine: " + ex.getMessage(), ex);
        }
    }
}
