package com.tseday.advert.meta;

import com.facebook.ads.sdk.IDName;
import com.tseday.advert.meta.dto.*;
import com.tseday.advert.meta.service.AdCreativeService;
import com.tseday.advert.meta.service.MetaAdService;
import com.tseday.advert.meta.service.ProductDetails;
import com.tseday.advert.util.Pair;
import com.tseday.advert.util.WebScraper;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Component
public class MetaFunction {

    private final MetaAdService metaAdService;

    private final AdCreativeService adCreativeService;

    public MetaFunction(MetaAdService metaAdService, AdCreativeService adCreativeService) {
        this.metaAdService = metaAdService;
        this.adCreativeService = adCreativeService;
    }

    @Bean
    public Supplier<List<String>> getCampaignObjectives(){
        return metaAdService::getCampaignObjectives;
    }
    @Bean
    public Supplier<MetaCampaignDetails> getCampaigns() {
        return metaAdService::getCampaignDetails;
    }

    @Bean
    public Function<CreateCampaignRequest,MetaCampaignDetails> createCampaign(){
        return metaAdService::createNewCampaign;
    }

    @Bean
    public Function<LocationTargetRequest,Object> targetingGeoLocationSupplier(){
        return metaAdService::getGeoLocations;
    }

    @Bean
    public Function<String,Object> fetInterest(){
        return metaAdService::fetchInterest;
    }

    @Bean
    public Supplier< List<Pair<String, String>> > fetchBehaviour(){
        return metaAdService::fetchBehaviours;
    }

    @Bean
    public Supplier< List<Pair<String, String>> > fetchIndustries(){
        return metaAdService::fetchIndustries;
    }

    @Bean
    public Function<CreateAdSetRequest,String> createdAdSet(){
       return metaAdService::createAdSet;
    }

    @Bean
    public Consumer<AdSetRequest> deleteAdSet(){
        return metaAdService::deleteAdSet;
    }

    @Bean
    Consumer<CreateAdSetRequest> updateAdSet(){
        return metaAdService::updateAdSet;
    }

    @Bean
    public Supplier<Object> createAdCreative(){
        return metaAdService::createAdCreative;
    }

    @Bean
    public Supplier<String> createCarouselAdCreative(){
        return adCreativeService::createCarouselAdCreative;
    }

//    @Bean
//    public Function<String,String> createAdMedia(){
//        return metaAdService::createAdMedia;
//    }

    @Bean
    public Supplier<List<String>> getAdImages(){
        return metaAdService::getAdImages;
    }

    @Bean
    public Supplier<String> createInstantExperience(){
        return adCreativeService::createInstantExperience;
    }

    @Bean
    public Function<CreateAdRequest,String> createAd(){
        return metaAdService::createAd;
    }

    @Bean
    public Function<String,String> getAdSetIdByName(){
        return metaAdService::getAdSetByName;
    }

    @Bean
    Consumer<String> updateCampaign(){
       return metaAdService::updateCampaign;
    }

    @Bean Supplier<List<ProductDetails>> createProductItems(){
        return metaAdService::createBatchProductItems;
    }


    @Bean
    Supplier<List<String>> updateProductItems(){
        return metaAdService::updateProductItems;
    }

    @Bean
    Supplier< List<String> > createCanvas(){
        return adCreativeService::createCanvas;
    }

    @Bean
    Supplier<String> createVideoCollectionAd(){
        return adCreativeService::createVideoCollectionAds;
    }

    @Bean
    Function<AdMediaUpload,String> uploadVideo(){
        return metaAdService::createAdMedia;

    }

    @Bean
    Function<String,List<String>> createProductSet(){
        return metaAdService::createProductSet;
    }

    @Bean
    Supplier<String> createCanvasProductSet(){
        return adCreativeService::createCollectionProductSet;
    }

    @Bean
    Supplier<List<String>> createCanvasElement(){
        return adCreativeService::createCanvasElement;
    }

    @Bean
    Supplier<List<String>> createMultiPhotoPost(){
        return adCreativeService::createMultiPhotoPost;
    }

    @Bean
    Supplier<List<String>> getPhotos(){
        return  adCreativeService::resizeImage;
    }


    @Bean
    Function<String,String> pagePostAd(){
        return adCreativeService::createAdFromPost;
    }


    @Bean Function<List<String>,String> publishPost(){
        return adCreativeService::publishPosts;
    }

    @Bean
    Supplier<String> extract(){
        return WebScraper::extract;
    }
}
