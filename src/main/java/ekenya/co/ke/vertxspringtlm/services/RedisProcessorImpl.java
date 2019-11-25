package ekenya.co.ke.vertxspringtlm.services;

import ekenya.co.ke.vertxspringtlm.dao.redis.TransactionRecorder;
import ekenya.co.ke.vertxspringtlm.dao.redisRepository.TransactionRecorderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author munialo.roy@ekenya.co.ke
 */
@Service
public class RedisProcessorImpl implements RedisProcessor {

    @Autowired private TransactionRecorderRepository transactionRecorderRepository;
    @Override
    public TransactionRecorder createTransactionRecorder(TransactionRecorder transactionRecorder) {
        return transactionRecorderRepository.save(transactionRecorder);
    }

    @Override
    public TransactionRecorder findTransactionRecord(long transactionId) {

        List<TransactionRecorder> recorderList = new ArrayList<>();

        transactionRecorderRepository.findById(transactionId).ifPresent(recorderList::add);
        return recorderList.size() > 0 ? recorderList.get(0) : null;
    }
}
