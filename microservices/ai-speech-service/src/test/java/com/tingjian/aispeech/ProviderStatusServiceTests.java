package com.tingjian.aispeech;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderStatusServiceTests {
    @Test
    void reportsCloudProvidersAsConfiguredWhenKeysExist() {
        ProviderStatusResponse response = new ProviderStatusService(
                "qwen-key", "qwen-plus", "asr-key", "paraformer-realtime-v2").current();

        assertThat(response.capabilities()).hasSize(3);
        assertThat(response.capabilities()).filteredOn(ProviderStatusResponse.ProviderCapability::configured)
                .hasSize(3);
    }

    @Test
    void keepsOnDeviceTtsAvailableWithoutCloudKeys() {
        ProviderStatusResponse response = new ProviderStatusService(
                "", "qwen-plus", "", "paraformer-realtime-v2").current();

        assertThat(response.capabilities()).filteredOn(capability -> capability.capability().equals("TTS"))
                .singleElement()
                .satisfies(capability -> {
                    assertThat(capability.configured()).isTrue();
                    assertThat(capability.mode()).isEqualTo("ON_DEVICE");
                });
    }
}
