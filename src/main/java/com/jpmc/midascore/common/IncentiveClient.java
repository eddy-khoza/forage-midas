package com.jpmc.midascore.common;

import com.jpmc.midascore.foundation.Incentive;
import com.jpmc.midascore.foundation.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class IncentiveClient {

    private static final Logger logger = LoggerFactory.getLogger(IncentiveClient.class);

    private final RestTemplate restTemplate;
    private final String incentiveApiUrl;

    public IncentiveClient(@Value("${incentive.api.url:http://localhost:8080/incentive}") String incentiveApiUrl) {
        this.restTemplate = new RestTemplate();
        this.incentiveApiUrl = incentiveApiUrl;
    }

    public Incentive getIncentive(Transaction transaction) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            IncentiveRequest request = new IncentiveRequest(
                    transaction.getSenderId(),
                    transaction.getRecipientId(),
                    transaction.getAmount()
            );

            HttpEntity<IncentiveRequest> entity = new HttpEntity<>(request, headers);

            logger.debug("Calling Incentive API at: {}", incentiveApiUrl);
            Incentive incentive = restTemplate.postForObject(
                    incentiveApiUrl,
                    entity,
                    Incentive.class
            );

            if (incentive != null) {
                logger.debug("Received incentive: amount={}", incentive.getAmount());
            }

            return incentive;

        } catch (Exception e) {
            logger.error("Failed to call Incentive API: {}", e.getMessage());
            return new Incentive(0.0f, "No incentive");
        }
    }

    private static class IncentiveRequest {
        private long senderId;
        private long recipientId;
        private float amount;

        public IncentiveRequest(long senderId, long recipientId, float amount) {
            this.senderId = senderId;
            this.recipientId = recipientId;
            this.amount = amount;
        }

        public long getSenderId() { return senderId; }
        public void setSenderId(long senderId) { this.senderId = senderId; }
        public long getRecipientId() { return recipientId; }
        public void setRecipientId(long recipientId) { this.recipientId = recipientId; }
        public float getAmount() { return amount; }
        public void setAmount(float amount) { this.amount = amount; }
    }
}