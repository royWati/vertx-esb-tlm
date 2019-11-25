package ekenya.co.ke.vertxspringtlm.services;

import ekenya.co.ke.vertxspringtlm.dao.redis.TransactionRecorder;

/**
 * @Author munialo.roy@ekenya.co.ke
 */
public interface RedisProcessor {

    TransactionRecorder createTransactionRecorder(TransactionRecorder transactionRecorder);

    TransactionRecorder findTransactionRecord(long transactionId);

}
