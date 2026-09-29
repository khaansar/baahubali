package com.example.analyticsservice.contract;

public interface RankingEngine {

    PeerComparisonResult processRankAndStats(
            AttemptSubmittedEvent event
    );
}
