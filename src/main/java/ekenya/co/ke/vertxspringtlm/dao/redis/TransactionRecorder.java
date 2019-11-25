package ekenya.co.ke.vertxspringtlm.dao.redis;

import ekenya.co.ke.vertxspringtlm.dao.DataStore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.util.List;

/**
 * @Author munialo.roy@ekenya.co.ke
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("transaction_recorder_hash")
public class TransactionRecorder {

    @Id
    private long transactionId;
    private String transactionRefNo;
    private List<DataStore> dataStoreList;

}
