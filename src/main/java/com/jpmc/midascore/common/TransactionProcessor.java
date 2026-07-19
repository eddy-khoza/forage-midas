package com.jpmc.midascore.common;

import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.Incentive;
import com.jpmc.midascore.foundation.Transaction;
import com.jpmc.midascore.repository.TransactionRecordRepository;
import com.jpmc.midascore.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TransactionProcessor {

    private static final Logger logger = LoggerFactory.getLogger(TransactionProcessor.class);

    private final UserRepository userRepository;
    private final TransactionRecordRepository transactionRecordRepository;
    private final IncentiveClient incentiveClient;

    public TransactionProcessor(UserRepository userRepository,
                                TransactionRecordRepository transactionRecordRepository,
                                IncentiveClient incentiveClient) {
        this.userRepository = userRepository;
        this.transactionRecordRepository = transactionRecordRepository;
        this.incentiveClient = incentiveClient;
    }

    @Transactional
    public boolean process(Transaction transaction) {
        UserRecord sender = userRepository.findById(transaction.getSenderId());
        if (sender == null) {
            logger.debug("Invalid sender ID: {}", transaction.getSenderId());
            return false;
        }

        UserRecord recipient = userRepository.findById(transaction.getRecipientId());
        if (recipient == null) {
            logger.debug("Invalid recipient ID: {}", transaction.getRecipientId());
            return false;
        }

        if (sender.getBalance() < transaction.getAmount()) {
            logger.debug("Insufficient balance: sender={}, balance={}, amount={}",
                    sender.getName(), sender.getBalance(), transaction.getAmount());
            return false;
        }

        Incentive incentive = incentiveClient.getIncentive(transaction);
        float incentiveAmount = (incentive != null) ? incentive.getAmount() : 0.0f;

        sender.setBalance(sender.getBalance() - transaction.getAmount());
        recipient.setBalance(recipient.getBalance() + transaction.getAmount() + incentiveAmount);

        TransactionRecord record = new TransactionRecord(sender, recipient, transaction.getAmount(), incentiveAmount);
        transactionRecordRepository.save(record);

        userRepository.save(sender);
        userRepository.save(recipient);

        logger.debug("✅ Transaction processed: sender={}, recipient={}, amount={}, incentive={}",
                sender.getName(), recipient.getName(), transaction.getAmount(), incentiveAmount);

        return true;
    }
}