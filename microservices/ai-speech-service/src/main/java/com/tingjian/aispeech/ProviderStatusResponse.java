package com.tingjian.aispeech;

import java.util.List;

public record ProviderStatusResponse(List<ProviderCapability> capabilities) {
    public record ProviderCapability(
            String capability,
            String provider,
            String model,
            boolean configured,
            String mode
    ) {
    }
}
