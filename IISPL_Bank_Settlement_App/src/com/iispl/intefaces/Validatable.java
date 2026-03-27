package com.iispl.intefaces;



import java.util.List;

/**
 * Validatable — implemented by entities and POJOs that need business-rule validation
 * before they enter the settlement pipeline.
 */
public interface Validatable {
    boolean isValid();
    List<String> getValidationErrors();
}