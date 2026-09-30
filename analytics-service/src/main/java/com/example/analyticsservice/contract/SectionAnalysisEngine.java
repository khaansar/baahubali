package com.example.analyticsservice.contract;

public interface SectionAnalysisEngine {

    SectionAnalysisResult analyze(AttemptSubmittedEvent event);
}
