package com.iispl.service;

import java.util.List;
import com.iispl.entity.IncomingTransaction;

public interface NettingService {

    void processNetting(List<IncomingTransaction> transactions);

	void printNettingReport();

}