package com.tseday.advert.meta.service;

import com.facebook.ads.sdk.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdNetworkAnalysis {

    private final APIContext apiContext;

    private final ObjectMapper objectMapper;

    @Value("${meta.accountId}")
    String accountId;

    @Value("${meta.productCatalog}")
    String productCatalogId;

    private final StringTemplate.Processor<JsonObject, RuntimeException> JSON;

    @Autowired
    public AdNetworkAnalysis(@Qualifier("metaApiContext") APIContext apiContext, ObjectMapper objectMapper,
                             @Qualifier("jsonProcessor") StringTemplate.Processor<JsonObject, RuntimeException> json) {
        this.apiContext = apiContext;
        JSON = json;
        APIRequest<AdAccount> adAccountAPIRequest = new APIRequest<>(apiContext, "me", "/adaccounts", "GET", AdAccount.getParser());

        this.objectMapper = objectMapper;
    }

    public Object getAnalytics() {

        try {
            Business business = new Business("1006445063866764", apiContext).get().requestIdField().execute();


                 return   business .getAdNetworkAnalytics()
                    .setMetrics(List.of(

                            AdNetworkAnalyticsSyncQueryResult.EnumMetrics.VALUE_FB_AD_NETWORK_BIDDING_REQUEST,
                            AdNetworkAnalyticsSyncQueryResult.EnumMetrics.VALUE_FB_AD_NETWORK_BIDDING_RESPONSE
                    )).setBreakdowns(List.of(
                            AdNetworkAnalyticsSyncQueryResult.EnumBreakdowns.VALUE_AD_SPACE,
                            AdNetworkAnalyticsSyncQueryResult.EnumBreakdowns.VALUE_COUNTRY
                    )).setSince("2023-12-20")
                    .setUntil("2023-12-24").setLimit(2L).execute()
                           .stream().map(AdNetworkAnalyticsSyncQueryResult::getFieldResults)
                           .collect(Collectors.toList());



        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public String createAudience(){
        try{

            String ruleClick =  JSON."""
                    {
                      "inclusions": {
                        "operator": "or",
                        "rules": [
                          {
                            "event_sources": [
                              {
                                "id": "1145756813462829",
                                "type": "canvas"
                              }
                            ],
                            "retention_seconds": 31536000,
                            "filter": {
                              "operator": "and",
                              "filters": [
                                {
                                  "field": "event",
                                  "operator": "eq",
                                  "value": "instant_shopping_element_click"
                                }
                              ]
                            }
                          }
                        ]
                      }
                    }
                    """.toString();


            String instaCtaClick =  JSON."""
                    {
                        "inclusions": {
                          "operator": "or",
                          "rules": [
                            {
                              "event_sources": [
                                {
                                  "id": "1145756813462829",
                                  "type": "ig_business"
                                }
                              ],
                              "retention_seconds": 31536000,
                              "filter": {
                                "operator": "and",
                                "filters": [
                                  {
                                    "field": "event",
                                    "operator": "eq",
                                    "value": "ig_ad_cta_click"
                                  }
                                ]
                              }
                            }
                          ]
                        }
                      }
                    """.toString();





//}

//            CustomAudience clickAudience = new AdAccount(accountId, apiContext).createCustomAudience()
//                    .setName("Collection Engagement Audience click")
//                    .setDescription("People who clicked any links in this Instant Experience")
//                    .setRule(ruleClick)
//                    .execute();


            CustomAudience openAudience = new AdAccount(accountId, apiContext).createCustomAudience()
                    .setName("Collection Engagement insta cta click")
                    .setDescription("People who opened cta")
                    .setRule(instaCtaClick)
                    .execute();

            return openAudience.getFieldId();


        }
        catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public void getAudience(){

        try {


            // new AdAccount(accountId, apiContext).getCustomAudiences()
//         .requestField("name")
//         .requestField("id")
//         .execute().stream()
//                    .map(c -> new Pair(c.getFieldName(),c.getId()))
//         .toList()

            CustomAudience amazonAffiliateAudience = new AdAccount(accountId, apiContext).getCustomAudiences()
                    .execute().getFirst();


            String fieldId = amazonAffiliateAudience.getFieldId();
        }
        catch (Exception e){
            throw new RuntimeException(e.getMessage());
        }
        
    }


    public String createLookAlikeAudience() {
        try {

            CustomAudience affiliateLookAlike = new AdAccount(accountId, apiContext).createCustomAudience()
                    .setName("audibleLookAlike")
                    .setSubtype(CustomAudience.EnumSubtype.VALUE_LOOKALIKE)
                    .setOriginAudienceId("120204436206180117")
                    .setLookalikeSpec(JSON."""
                            {"type":"similarity","country":"US"}
                            """.toString())
                    .execute();
            return affiliateLookAlike.getId();

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }


    public String getLookAlikeAudience(){
        try{
            new CustomAudience("120203350492810117",apiContext).get()
                    .requestLookalikeAudienceIdsField()
                    .requestApproximateCountUpperBoundField()
                    .requestApproximateCountLowerBoundField()
                    .requestOperationStatusField()
                    .execute().getFieldLookalikeSpec();
            return "success";
        }catch (Exception e){
            throw new RuntimeException(e.getMessage());
        }
    }

}

