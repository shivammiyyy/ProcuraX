package com.procurax.quotation.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.procurax.quotation.domain.VendorScore;
import java.math.BigDecimal;

public record VendorScoreResponse(
        BigDecimal score,
        JsonNode factors,
        JsonNode weights,
        String explanation,
        BigDecimal confidence,
        String modelVersion) {

    public static VendorScoreResponse from(VendorScore score) {
        return new VendorScoreResponse(score.getTotalScore(), score.getFactors(), score.getWeights(),
                score.getExplanation(), score.getConfidence(), score.getModelVersion());
    }
}
