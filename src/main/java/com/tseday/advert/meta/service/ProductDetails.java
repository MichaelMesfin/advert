package com.tseday.advert.meta.service;

public record ProductDetails(String id , String name, String price, String imageUrl, String description) {

    public ProductDetails(String name, String price, String imageUrl, String description){
        this(null,name,price,imageUrl,description);
    }
}
