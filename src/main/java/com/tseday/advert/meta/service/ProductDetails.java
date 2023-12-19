package com.tseday.advert.meta.service;

public record ProductDetails(String id , String name, String price, String mainImage, String description,String url) {

    public ProductDetails(String name, String price, String mainImage, String description,String url){
        this(null,name,price,mainImage,description,url);
    }


}
