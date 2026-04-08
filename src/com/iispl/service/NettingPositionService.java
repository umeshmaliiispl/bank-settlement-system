package com.iispl.service;

import java.util.List;
import com.iispl.entity.IncomingTransaction;

public interface NettingPositionService {
    void calculateAndStore(List<IncomingTransaction> txns);
}