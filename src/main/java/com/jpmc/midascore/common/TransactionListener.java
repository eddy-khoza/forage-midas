package com.jpmc.midascore.common;

import com.jpmc.midascore.foundation.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TransactionListener {

    private static final Logger logger = LoggerFactory.getLogger(TransactionListener.class);
    private final TransactionProcessor transactionProcessor;
    private int count = 0;

    public TransactionListener(TransactionProcessor transactionProcessor) {
        this.transactionProcessor = transactionProcessor;
    }

    @KafkaListener(topics = "${general.kafka-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void listen(String message) {
        try {
            String[] parts = message.trim().split("\\s*,\\s*");

            if (parts.length == 3) {
                long senderId = Long.parseLong(parts[0].trim());
                long recipientId = Long.parseLong(parts[1].trim());
                float amount = Float.parseFloat(parts[2].trim());

                Transaction transaction = new Transaction(senderId, recipientId, amount);
                count++;

                // Process the transaction
                boolean success = transactionProcessor.process(transaction);

                if (success) {
                    logger.info("Good Transaction #{} processed: senderId={}, recipientId={}, amount={}",
                            count, senderId, recipientId, amount);
                } else {
                    logger.info("Bad Transaction #{} rejected: senderId={}, recipientId={}, amount={}",
                            count, senderId, recipientId, amount);
                }

            } else {
                logger.warn("Unexpected message format (expected 3 parts, got {}): {}", parts.length, message);
            }

        } catch (Exception e) {
            logger.error("Failed to process transaction: {}", message, e);
        }
    }
}