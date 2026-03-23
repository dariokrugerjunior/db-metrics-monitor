package br.com.dariokruger.dbmetricsmonitor.dto;

import java.math.BigDecimal;

public record CacheHitRatioResponse(
        BigDecimal cacheHitRatioPercent,
        long heapBlksRead,
        long heapBlksHit
) {
}
