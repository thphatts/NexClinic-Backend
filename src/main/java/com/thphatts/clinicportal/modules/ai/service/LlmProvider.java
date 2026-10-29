package com.thphatts.clinicportal.modules.ai.service;

public interface LlmProvider {

    String generateText(String prompt);

    boolean isAvailable();

    String getProviderName();
}
