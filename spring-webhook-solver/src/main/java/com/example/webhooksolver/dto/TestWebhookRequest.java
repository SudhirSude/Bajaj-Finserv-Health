
package com.example.webhooksolver.dto;

public class TestWebhookRequest {
    // according to the task the receiving API expects a key "finalQuery"
    // use the exact JSON key finalQuery
    private String finalQuery;

    public TestWebhookRequest() {}

    public String getFinalQuery() {
        return finalQuery;
    }

    public void setFinalQuery(String finalQuery) {
        this.finalQuery = finalQuery;
    }
}
