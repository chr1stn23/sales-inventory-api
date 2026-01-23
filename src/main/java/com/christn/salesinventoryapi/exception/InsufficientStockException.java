package com.christn.salesinventoryapi.exception;

public class InsufficientStockException extends RuntimeException{

    public InsufficientStockException(String productName) {
        super("Stock insuficiente del producto " + productName);
    }
}
