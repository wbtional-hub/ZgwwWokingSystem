package com.example.lecturesystem.modules.agent.dto;

public class PolicyDraftRequest {
    private String draftPayload;
    private String actionContent;

    public String getDraftPayload() {
        return draftPayload;
    }

    public void setDraftPayload(String draftPayload) {
        this.draftPayload = draftPayload;
    }

    public String getActionContent() {
        return actionContent;
    }

    public void setActionContent(String actionContent) {
        this.actionContent = actionContent;
    }
}
