package com.tingjian.server.service;

public interface VerificationDeliveryGateway {
    void send(String channel, String destination, String purpose, String code, int validMinutes);
}
