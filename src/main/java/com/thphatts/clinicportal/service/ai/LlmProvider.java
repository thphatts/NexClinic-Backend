package com.thphatts.clinicportal.service.ai;

public interface LlmProvider {

    String generateText(String prompt);

    boolean isAvailable();

    String getProviderName();
}
