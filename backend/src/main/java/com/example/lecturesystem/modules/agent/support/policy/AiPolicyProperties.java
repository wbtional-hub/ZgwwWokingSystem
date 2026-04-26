package com.example.lecturesystem.modules.agent.support.policy;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ai.policy")
public class AiPolicyProperties {
    private boolean enabled = true;
    private Feature router = new Feature();
    private Feature faq = new Feature();
    private Feature eval = new Feature();
    private StrictFeature evidence = new StrictFeature();
    private StrictFeature answer = new StrictFeature();
    private StrictFeature region = new StrictFeature();
    private Diagnosis diagnosis = new Diagnosis();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Feature getRouter() {
        return router;
    }

    public void setRouter(Feature router) {
        this.router = router;
    }

    public Feature getFaq() {
        return faq;
    }

    public void setFaq(Feature faq) {
        this.faq = faq;
    }

    public Feature getEval() {
        return eval;
    }

    public void setEval(Feature eval) {
        this.eval = eval;
    }

    public StrictFeature getEvidence() {
        return evidence;
    }

    public void setEvidence(StrictFeature evidence) {
        this.evidence = evidence;
    }

    public StrictFeature getAnswer() {
        return answer;
    }

    public void setAnswer(StrictFeature answer) {
        this.answer = answer;
    }

    public StrictFeature getRegion() {
        return region;
    }

    public void setRegion(StrictFeature region) {
        this.region = region;
    }

    public Diagnosis getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(Diagnosis diagnosis) {
        this.diagnosis = diagnosis;
    }

    public static class Feature {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class StrictFeature {
        private boolean enabled = true;
        private boolean strict = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isStrict() {
            return strict;
        }

        public void setStrict(boolean strict) {
            this.strict = strict;
        }
    }

    public static class Diagnosis {
        private Panel panel = new Panel();

        public Panel getPanel() {
            return panel;
        }

        public void setPanel(Panel panel) {
            this.panel = panel;
        }
    }

    public static class Panel {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
