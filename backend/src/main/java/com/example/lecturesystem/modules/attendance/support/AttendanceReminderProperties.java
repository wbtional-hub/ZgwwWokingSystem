package com.example.lecturesystem.modules.attendance.support;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "attendance.reminder")
public class AttendanceReminderProperties {
    private static final String DEFAULT_CHECKIN_CRON = "0 35 8 ? * MON-FRI";
    private static final String DEFAULT_CHECKOUT_CRON = "0 35 17 ? * MON-FRI";
    private static final String ENV_REMINDER_ENABLED = "ATTENDANCE_REMINDER_ENABLED";
    private static final String ENV_REMINDER_ENTRY_URL = "ATTENDANCE_REMINDER_ENTRY_URL";
    private static final String ENV_CHECKIN_TEMPLATE_ID = "ATTENDANCE_REMINDER_CHECKIN_TEMPLATE_ID";
    private static final String ENV_CHECKOUT_TEMPLATE_ID = "ATTENDANCE_REMINDER_CHECKOUT_TEMPLATE_ID";
    private static final String ENV_LEGACY_CHECKIN_TEMPLATE = "ATTENDANCE_REMINDER_TEMPLATE_CHECKIN";
    private static final String ENV_LEGACY_CHECKOUT_TEMPLATE = "ATTENDANCE_REMINDER_TEMPLATE_CHECKOUT";

    private boolean enabled = false;
    private String entryUrl = "";
    private String checkinTemplateId = "";
    private String checkoutTemplateId = "";
    private Cron cron = new Cron();
    private Checkin checkin = new Checkin();
    private Checkout checkout = new Checkout();
    private Template template = new Template();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEntryUrl() {
        return entryUrl;
    }

    public void setEntryUrl(String entryUrl) {
        this.entryUrl = entryUrl;
    }

    public String getCheckinTemplateId() {
        return checkinTemplateId;
    }

    public void setCheckinTemplateId(String checkinTemplateId) {
        this.checkinTemplateId = checkinTemplateId;
    }

    public String getCheckoutTemplateId() {
        return checkoutTemplateId;
    }

    public void setCheckoutTemplateId(String checkoutTemplateId) {
        this.checkoutTemplateId = checkoutTemplateId;
    }

    public Cron getCron() {
        return cron;
    }

    public void setCron(Cron cron) {
        this.cron = cron;
    }

    public Checkin getCheckin() {
        return checkin;
    }

    public void setCheckin(Checkin checkin) {
        this.checkin = checkin;
    }

    public Checkout getCheckout() {
        return checkout;
    }

    public void setCheckout(Checkout checkout) {
        this.checkout = checkout;
    }

    public Template getTemplate() {
        return template;
    }

    public void setTemplate(Template template) {
        this.template = template;
    }

    public String getResolvedCheckinCron() {
        return firstNonBlank(
                cron == null ? null : cron.getCheckin(),
                checkin == null ? null : checkin.getCron(),
                DEFAULT_CHECKIN_CRON
        );
    }

    public String getResolvedCheckoutCron() {
        return firstNonBlank(
                cron == null ? null : cron.getCheckout(),
                checkout == null ? null : checkout.getCron(),
                DEFAULT_CHECKOUT_CRON
        );
    }

    public boolean isDefaultCheckinCronApplied() {
        return isBlank(cron == null ? null : cron.getCheckin())
                && isBlank(checkin == null ? null : checkin.getCron());
    }

    public boolean isDefaultCheckoutCronApplied() {
        return isBlank(cron == null ? null : cron.getCheckout())
                && isBlank(checkout == null ? null : checkout.getCron());
    }

    public String getResolvedEntryUrl() {
        return firstNonBlank(entryUrl, null, "");
    }

    public String getResolvedCheckinTemplateId() {
        return firstNonBlank(
                checkinTemplateId,
                template == null ? null : template.getCheckin(),
                ""
        );
    }

    public String getResolvedCheckoutTemplateId() {
        return firstNonBlank(
                checkoutTemplateId,
                template == null ? null : template.getCheckout(),
                ""
        );
    }

    public String resolveConfigSourceHint() {
        if (hasEnvironmentOverride()) {
            return "env";
        }
        return "application";
    }

    public String getCheckinTemplateIdPreview() {
        return previewValue(getResolvedCheckinTemplateId());
    }

    public String getCheckoutTemplateIdPreview() {
        return previewValue(getResolvedCheckoutTemplateId());
    }

    private String firstNonBlank(String primary, String fallback, String defaultValue) {
        if (!isBlank(primary)) {
            return primary.trim();
        }
        if (!isBlank(fallback)) {
            return fallback.trim();
        }
        return defaultValue;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean hasEnvironmentOverride() {
        return hasEnvText(ENV_REMINDER_ENABLED)
                || hasEnvText(ENV_REMINDER_ENTRY_URL)
                || hasEnvText(ENV_CHECKIN_TEMPLATE_ID)
                || hasEnvText(ENV_CHECKOUT_TEMPLATE_ID)
                || hasEnvText(ENV_LEGACY_CHECKIN_TEMPLATE)
                || hasEnvText(ENV_LEGACY_CHECKOUT_TEMPLATE);
    }

    private boolean hasEnvText(String envName) {
        String value = System.getenv(envName);
        return value != null && !value.trim().isEmpty();
    }

    private String previewValue(String value) {
        if (isBlank(value)) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= 6) {
            return trimmed.substring(0, Math.min(2, trimmed.length())) + "***";
        }
        return trimmed.substring(0, 4) + "***" + trimmed.substring(trimmed.length() - 2);
    }

    public static class Cron {
        private String checkin = "";
        private String checkout = "";

        public String getCheckin() {
            return checkin;
        }

        public void setCheckin(String checkin) {
            this.checkin = checkin;
        }

        public String getCheckout() {
            return checkout;
        }

        public void setCheckout(String checkout) {
            this.checkout = checkout;
        }
    }

    public static class Template {
        private String checkin = "";
        private String checkout = "";

        public String getCheckin() {
            return checkin;
        }

        public void setCheckin(String checkin) {
            this.checkin = checkin;
        }

        public String getCheckout() {
            return checkout;
        }

        public void setCheckout(String checkout) {
            this.checkout = checkout;
        }
    }

    public static class Checkin {
        private String cron = "";

        public String getCron() {
            return cron;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }
    }

    public static class Checkout {
        private String cron = "";

        public String getCron() {
            return cron;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }
    }
}
