package com.iispl.intefaces;



/**
 * Processable — implemented by any entity that can move through a processing pipeline.
 * Member 2 (Transaction) and Member 3 (Settlement) both implement this.
 */
public interface Processable {
    void validate() throws IllegalStateException;
    boolean isReadyForProcessing();
    String getProcessingStatus();
}