package com.tseday.advert.meta.service;

import com.facebook.ads.sdk.*;
import com.facebook.ads.sdk.APIRequest.DefaultRequestExecutor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.tseday.advert.meta.MetaCampaignDetails;
import com.tseday.advert.meta.dto.*;
import com.tseday.advert.meta.persistance.*;
import com.tseday.advert.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.StructuredTaskScope;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Service
public class MetaAdService {

    private final APIContext apiContext;
    private final APIRequest<AdAccount> adAccountAPIRequest;

    private final ObjectMapper objectMapper;

    private final GeoLocationRepository geoLocationRepository;

    private final TargetRepository targetRepository;

    private final CampaignRepository campaignRepository;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss-0SSS");

    @Value("${meta.accountId}")
    String accountId;

    @Value("${meta.productCatalog}")
    String productCatalogId;

    @Autowired
    public MetaAdService(@Qualifier("metaApiContext") APIContext apiContext,
                         ObjectMapper objectMapper,
                         GeoLocationRepository geoLocationRepository,
                         TargetRepository targetRepository,
                         CampaignRepository campaignRepository
                                 ) {
        this.apiContext = apiContext;
        adAccountAPIRequest = new APIRequest<>(apiContext, "me", "/adaccounts", "GET", AdAccount.getParser());
        this.objectMapper = objectMapper;
        this.geoLocationRepository = geoLocationRepository;
        this.targetRepository = targetRepository;
        this.campaignRepository = campaignRepository;
    }

    public List<String> getCampaignObjectives() {
        return Arrays.stream(Campaign.EnumObjective.values())
                .map(Enum::name)
                .collect(Collectors.toList());
    }


    public MetaCampaignDetails getCampaignDetails() {
        try {
            AdAccount adAccount = new AdAccount(accountId, apiContext);

            Campaign campaign = adAccount.getCampaigns().requestAllFields().execute().stream()
                    .findFirst().get();


            Optional<AdSet> adSet = campaign.getAdSets().execute()
                    .stream().findFirst();

            return new MetaCampaignDetails(campaign.getFieldName(), campaign.getId(), adAccount.getId(), adSet.map(AdSet::getId).orElse(null));

        } catch (APIException e) {
            throw new RuntimeException(e);
        }
    }

    public MetaCampaignDetails createNewCampaign(CreateCampaignRequest createCampaignRequest) {

        try {
            AdAccount adAccount = new AdAccount(accountId, apiContext);

            Campaign campaign = adAccount.
                    createCampaign()
                    .setName(createCampaignRequest.campaignName())
                    .setObjective(createCampaignRequest.objective())
                    .setPromotedObject("{\"product_catalog_id\":\"258227100404765\"}")
                    .setStatus(Campaign.EnumStatus.VALUE_PAUSED)
                    .setParam("special_ad_categories", "[]")
                    .execute().fetch();

//            campaignRepository.save(new CampaignEntity(campaign.getId(),
//                    campaign.getFieldName(),
//                    CampaignObjectiveEnums.valueOf(campaign.getFieldObjective())));

            return new MetaCampaignDetails(campaign.getFieldName(), campaign.getId(), adAccount.getId(), null);

        } catch (APIException e) {
            throw new RuntimeException(e);
        }
    }


    public void updateCampaign(String name) {

        try {
            AdAccount adAccount = new AdAccount(accountId, apiContext);

            Campaign campaign = adAccount.getCampaigns().requestAllFields().execute().stream()
                    .filter(c -> c.getFieldName().equals(name))
                    .findFirst().get();

            campaign.update()
                    .setObjective(Campaign.EnumObjective.VALUE_OUTCOME_AWARENESS)
                    .execute();

        } catch (APIException e) {
            throw new RuntimeException(e);
        }
    }


    public List<Map<String, Object>> getGeoLocations(LocationTargetRequest location) {


        Map<String, Object> requestParams = Stream.of(new Pair<>(GeoLocationType.CITY, location.city()),
                        new Pair<>(GeoLocationType.REGION, location.region()),
                        new Pair<>(GeoLocationType.COUNTRY, location.country())
                ).filter(p -> p.right() != null && !p.right().isEmpty())
                .findFirst()
                .map(p -> p.left().getRequestParameters(p.right(), apiContext.getAccessToken()))
                .orElseThrow(() -> new IllegalArgumentException("please fill out one of the location types"));

        var defaultRequestExecutor = new DefaultRequestExecutor();

        try {
            var responseWrapper = getResponseWrapper(defaultRequestExecutor, requestParams);
            Map<String, List<Map<String, Object>>> geLocationDetail = objectMapper.readValue(responseWrapper.getBody(), Map.class);


             return geLocationDetail.get("data")
                   .stream()
                   .filter(m -> m.get("type").equals("city"))
                      .collect(Collectors.toList());
//                    .stream()
//                    .collect(Collectors.collectingAndThen(
//                            Collectors.partitioningBy(d -> d.get("geo_hierarchy_level") == null),
//                            m -> m.get(true).stream()
//                                    .filter(d -> d.get("type").equals("country"))
//                                    .collect(Collectors.groupingBy(d -> new Pair<>(d.get("region").toString(),
//                                            (Integer) d.get("region_id"))))
//                    ));

        } catch (APIException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Map<String, Object>> fetchInterest(List<String> interest) {

        try (var executor = new StructuredTaskScope.ShutdownOnFailure()) {

            List<StructuredTaskScope.Subtask<Map<String, List<Map<String, Object>>>>> subtaskList = interest.stream().map(String::trim)
                    .map(q -> Map.<String, Object>of(
                            "type", "adinterest",
                            "q", q,
                            "access_token", apiContext.getAccessToken()))
                    .map(requestParams -> executor.fork(() -> {
                                        Map<String, List<Map<String, Object>>> interestData = objectMapper.readValue(
                                                getResponseWrapper(new DefaultRequestExecutor(), requestParams).getBody(),
                                                Map.class);
                                        return interestData;
                                    }

                            )
                    )
                    .toList();

            executor.join();

            return subtaskList.stream()
                    .map(StructuredTaskScope.Subtask::get)
                    .map(m -> m.get("data"))
                    .flatMap(List::stream)
                    .map(d -> Map.of(d.get("id").toString(), d.get("name")))
                    .collect(Collectors.toList());

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    public List<Pair<String, String>> fetchIndustries() {
        List<String> matchList = List.of(
                "6012901802383"
             
        );


        DefaultRequestExecutor defaultRequestExecutor = new DefaultRequestExecutor();
        Map<String, Object> requestParams = Map.of(
                "type", "adTargetingCategory",
                "class", "industries",
                "access_token", apiContext.getAccessToken());

        try {
            var responseWrapper = getResponseWrapper(defaultRequestExecutor, requestParams);
            Map<String, List<Map<String, Object>>> map = objectMapper.readValue(responseWrapper.getBody(), Map.class);

            List<Map<String, Object>> data = map.get("data");
            List<Pair<String, String>> list = data.stream()
                    .map(d -> new Pair<>(d.get("id").toString(), d.get("name").toString())
                    )
                    .filter(p -> {
                        String key = p.left();
                        return matchList.stream().anyMatch(key::contains);
                    })

                    .toList();
            return list;


        } catch (APIException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public List<Pair<String, String>> fetchBehaviours() {

        List<String> matchList = List.of(
                "travel",
                "creators",
                "administrator",
                "admin",
                "business",
                "abroad",
                "Ethiopia"
        );


        DefaultRequestExecutor defaultRequestExecutor = new DefaultRequestExecutor();
        Map<String, Object> requestParams = Map.of(
                "type", "adTargetingCategory",
                "class", "behaviors",
                "access_token", apiContext.getAccessToken());

        try {
            var responseWrapper = getResponseWrapper(defaultRequestExecutor, requestParams);
            Map<String, List<Map<String, Object>>> map = objectMapper.readValue(responseWrapper.getBody(), Map.class);

            List<Map<String, Object>> data = map.get("data");
            List<Pair<String, String>> list = data.stream()
                    .map(d -> new Pair<>(d.get("id").toString(), d.get("name").toString())
                    )
//                    .filter(p -> {
//                        String key = p.right();
//                        return matchList.stream().anyMatch(key::contains);
//                    })

                    .toList();
            return list;


        } catch (APIException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public String createAdMedia(AdMediaUpload adMediaUpload) {
        AdTypeEnum type = adMediaUpload.type();
        String url = adMediaUpload.url();
        try {
            final File file = new File(url);
            return switch (type) {
                case IMAGE -> new AdAccount(accountId, apiContext).createAdImage()
                        .addUploadFile(url.split("/")[4]

                                , file)
                        .execute().getFieldUrl();
                case VIDEO -> new AdAccount(accountId, apiContext).createAdVideo()
                        .setSource(file)
                        .execute()
                        .getFieldId();
            };

        } catch (APIException e) {
            throw new RuntimeException(e);
        }
    }

    private static void chunkUpload(JsonObject response, AdAccount adAccount, String path, String uploadSessionId) throws APIException {
        String startOffset = response.get("start_offset").getAsString();
        String endOffset = response.get("end_offset").getAsString();

        if (startOffset.equals(endOffset)) {
//            System.out.println("start_offset = \{startOffset},  end_offset = \{endOffset}");
            
            adAccount.createAdVideo().setUploadPhase(AdVideo.EnumUploadPhase.VALUE_FINISH)
                    .setUploadSessionId(uploadSessionId).execute();
        } else {

            JsonObject chunkResponse = adAccount.createAdVideo()
                    .setUploadPhase(AdVideo.EnumUploadPhase.VALUE_TRANSFER)
                    .setUploadSessionId(uploadSessionId)
                    .setStartOffset(startOffset)
                    .setVideoFileChunk(path)
                    .execute().getRawResponseAsJsonObject();

            System.out.println("chunk processing");

            chunkUpload(chunkResponse, adAccount, path, uploadSessionId);
        }
    }


    public List<String> getAdImages() {
        try {
            return new AdAccount(accountId, apiContext).getAdImages()
                    .execute()
                    .stream()
                    .map(AdImage::getFieldHash)
                    .collect(Collectors.toList());

        } catch (APIException e) {
            throw new RuntimeException(e);
        }
    }


    public Object createAdCreative() {

        try {
            AdCreative adCreative = new AdAccount(accountId, apiContext)
                    .createAdCreative()
                    .setObjectStorySpec(new AdCreativeObjectStorySpec()
                            .setFieldPageId("107199648954711")
                            .setFieldLinkData(
                                    new AdCreativeLinkData()
                                            .setFieldImageHash("a7d5135ae52952fae7f0e6b3e8d5269b")
                                            .setFieldMessage("JOIN OUT TELEGRAM CHANNEL")
                                            .setFieldLink("https://t.me/endodETH")
                            )
                    ).setDegreesOfFreedomSpec(
                            new Gson().toJson(
                                    Map.of("creative_features_spec", Map.of(
                                            "standard_enhancements", Map.of(
                                                    "enroll_status", "OPT_OUT"
                                            )
                                    ))
                            )).execute();
//                    .setObjectStorySpec(new AdCreativeObjectStorySpec()
//
//                            .setFieldLinkData(
//                                    new AdCreativeLinkData().setFieldLink("https://www.youtube.com/watch?v=yMzESw_hTvg")
//                                            .setFieldImageHash("5c82000827ef4c498b25e38100dd2022")
//                                            .setFieldMessage("check it out")
//                            ).setFieldPageId("101086373072062"))
//                    .setDegreesOfFreedomSpec(
//                            new Gson().toJson(
//                                    Map.of("creative_features_spec",Map.of(
//                                            "standard_enhancements",Map.of(
//                                                    "enroll_status", "OPT_OUT"
//                                            )
//                                    ))
//                            )
//        ).execute();

            return createAd(
                    new CreateAdRequest("endod", "endodTelegram", adCreative.getId(), "endodTelegramChannel")
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteAdSet(AdSetRequest adSetRequest) {
        try {
            AdAccount adAccount = new AdAccount(accountId, apiContext);

            Campaign campaign = adAccount.getCampaigns().requestAllFields().execute().stream()
                    .filter(c -> c.getFieldName().equals(adSetRequest.campaignName()))
                    .findFirst().orElseThrow(() ->
                            new IllegalArgumentException(
                                   String.format("campaing name %s doesnt exit ", adSetRequest.campaignName())
                            ));

            AdSet adSet = campaign.getAdSets().requestAllFields().execute()
                    .stream()
                    .filter(a -> a.getFieldName().equals(adSetRequest.adSetName()))
                    .findFirst()
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                   String.format("adset name %s doesnt exit", adSetRequest.adSetName()
                                   )
                            ));

            adSet.delete().execute();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex.getMessage());
        }
    }


    public String getAdSetByName(String name) {

        try {
            AdAccount adAccount = new AdAccount(accountId, apiContext);

            Campaign campaign = adAccount.getCampaigns().requestAllFields().execute().stream()
                    .findFirst().get();

            APINodeList<AdSet> adSets = campaign.getAdSets()
                    .requestNameField()
                    .requestStartTimeField()
                    .requestEndTimeField()
                    .requestDailyBudgetField()
                    .requestLifetimeBudgetField()
                    .execute();

            return adSets.stream().filter(adSet -> adSet.getFieldName().equals(name))
                    .findFirst()
                    .map(AdSet::getId)
                    .orElseThrow(() -> new IllegalArgumentException(String.format("adset with name s% doesn't exist", name)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public String createAdSet(CreateAdSetRequest createAdSetRequest) {
        try {
            AdAccount adAccount = new AdAccount(accountId, apiContext);

            TargetingGeoLocation targetingGeoLocation;
            Campaign campaign = adAccount.getCampaigns().requestAllFields().execute().stream()
                    .filter(c -> c.getFieldName().equals(createAdSetRequest.campaignName()))
                    .findFirst().get();

            Pair<String, String> timeDuration = getTimeDuration();

            try (var executor = new StructuredTaskScope<>()) {
                StructuredTaskScope.Subtask<TargetingGeoLocation> targetingGeoLocationSubtask =
                        executor.fork(() -> getTargetingGeoLocation(createAdSetRequest));

//                StructuredTaskScope.Subtask<List<Pair<String, String>>> industrySubtask = executor.fork(this::fetchIndustries);


                executor.join();
                targetingGeoLocation = targetingGeoLocationSubtask.get();

            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }



            List<Map<String, Object>> fetchedInterest = fetchInterest(createAdSetRequest.interest());
                List<IDName> fieldInterests = fetchedInterest.stream().flatMap(m -> m.entrySet().stream())
                        .map(m -> new IDName().setFieldId(m.getKey())
                                .setFieldName(m.getValue().toString()))
                        .collect(Collectors.toList());

//                StructuredTaskScope.Subtask<List<Pair<String, String>>> behaviourSubTask = executor.fork(this::fetchBehaviours);

//                executor.join();
//
//                TargetingGeoLocation targetingGeoLocation = targetingGeoLocationSubtask.get();
//                List<IDName> industryTargets = industrySubtask.get().stream().map(p -> new IDName().setFieldName(p.right()).setFieldId(p.left()))
//                        .toList();

//                List<IDName> behaviourTargets = behaviourSubTask.get().stream().map(p -> new IDName().setFieldName(p.right()).setFieldId(p.left()))
//                        .toList();


                AdSet adSet = adAccount.createAdSet()
                        .setName(createAdSetRequest.adSetName())
                        .setLifetimeBudget(createAdSetRequest.budget().totalAmount())
                        .setBidAmount(createAdSetRequest.budget().bidAmount())
//                        .setDailyBudget(createAdSetRequest.budget().dailyAmount())
                        .setStartTime(timeDuration.left())
                        .setEndTime(timeDuration.right())
                        .setBillingEvent(AdSet.EnumBillingEvent.valueOf(createAdSetRequest.bidStrategy().billingEvent()))
                        .setOptimizationGoal(AdSet.EnumOptimizationGoal.valueOf(createAdSetRequest.bidStrategy().optimizationGoal()))
                        .setCampaignId(campaign.getId())
                        .setBidStrategy(AdSet.EnumBidStrategy.valueOf(createAdSetRequest.bidStrategy().strategy()))
//                        .setDestinationType(AdSet.EnumDestinationType.VALUE_MESSENGER)
                        .setPromotedObject(
                                Map.of(
                                        "product_set_id", "1046107546983626"
                                )

                        )
                        .setTargeting(
                                new Targeting()
                                        .setFieldAgeMax(Long.valueOf(createAdSetRequest.ageGroup().maxAge()))
                                        .setFieldAgeMin(Long.valueOf(createAdSetRequest.ageGroup().minAge()))
                                        .setFieldDevicePlatforms(
                                                List.of(Targeting.EnumDevicePlatforms.VALUE_MOBILE,
                                                        
                                                        Targeting.EnumDevicePlatforms.VALUE_DESKTOP
                                                        )
                                        )
                                        .setFieldFacebookPositions(List.of("feed", "facebook_reels"))

                                        .setFieldGenders(List.of(0L,1L))
                                        .setFieldInterests(fieldInterests)


                                        .setFieldGeoLocations(targetingGeoLocation)

//                                        .setFieldCustomAudiences(List.of(
//                                                new RawCustomAudience().setFieldId("120202442429000117"),
//                                                new RawCustomAudience().setFieldId("120202415069930117")
//                                                ))
//
                                        .setFieldPublisherPlatforms(List.of("facebook", "instagram"))
                        )

                        .setStatus(AdSet.EnumStatus.VALUE_PAUSED)
                        .execute();
                return adSet.getId();


        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private TargetingGeoLocation getTargetingGeoLocation(CreateAdSetRequest createAdSetRequest) {


        List<String> city = createAdSetRequest.locationTargetRequest().city();

        List<Map<String, Object>> locations = getGeoLocations(new LocationTargetRequest("ethiopia", null, null));


        List<TargetingGeoLocationCity> geoLocationCities = locations.stream()
                .filter(d -> d.get("type").equals("city"))
                .filter(c -> city.contains(c.get("name").toString()))
                .map(c -> new TargetingGeoLocationCity().setFieldKey(c.get("key").toString())
                        .setFieldName(c.get("name").toString())

                ).toList();

        return new TargetingGeoLocation().setFieldCities(geoLocationCities);

//
//        if (city != null && !city.isEmpty()) {
//            List<TargetingGeoLocationCity> targetingGeoLocationCities = geoLocations.values().stream().flatMap(List::stream)
//                    .filter(m -> city.contains(new CityLocation(m.get("region").toString(), m.get("name").toString())))
//                    .map(m -> new TargetingGeoLocationCity().setFieldKey(m.get("key").toString()).setFieldName(m.get("name").toString()))
//                    .collect(Collectors.toList());
//            targetingGeoLocation.setFieldCities(targetingGeoLocationCities);
//        } else {
//            List<TargetingGeoLocationRegion> targetingGeoLocationRegions;
//            if (region != null && !region.isEmpty()) {
//                targetingGeoLocationRegions = geoLocations.keySet().stream()
//                        .filter(s -> region.contains(s.left()))
//                        .map(s -> new TargetingGeoLocationRegion().setFieldKey(s.right().toString()).setFieldName(s.left()))
//                        .collect(Collectors.toList());
//            } else {
//                targetingGeoLocationRegions = geoLocations.keySet().stream()
//                        .map(s -> new TargetingGeoLocationRegion().setFieldKey(s.right().toString()).setFieldName(s.left())
//                        )
//                        .collect(Collectors.toList());
//            }
//            targetingGeoLocation.setFieldRegions(targetingGeoLocationRegions);
//        }
//        return targetingGeoLocation;
    }

    private Pair<String, String> getTimeDuration() {

        LocalDateTime startTime = LocalDateTime.now().plusHours(3);

        LocalDateTime endTime = startTime.plusDays(5);

        return new Pair<>(String.valueOf(startTime.atZone(ZoneId.systemDefault()).toEpochSecond()),
                String.valueOf(endTime.atZone(ZoneId.systemDefault()).toEpochSecond())
        );

    }

    public void updateAdSet(CreateAdSetRequest createAdSetRequest) {
        try {

            AdAccount adAccount = new AdAccount(accountId, apiContext);

            Campaign campaign = adAccount.getCampaigns().requestAllFields().execute().stream()
                    .filter(c -> c.getFieldName().equals(createAdSetRequest.campaignName()))
                    .findFirst().orElseThrow(() -> new IllegalArgumentException("campaign doesn't exist"));

            APINodeList<AdSet> adSets = campaign.getAdSets()
                    .requestNameField()
                    .requestStartTimeField()
                    .requestEndTimeField()
                    .requestDailyBudgetField()
                    .requestLifetimeBudgetField()
                    .execute();

            AdSet adset = adSets.stream()
                    .filter(adSet -> adSet.getFieldName().equals(createAdSetRequest.adSetName()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("adset with name s% doesn't exist",
                            createAdSetRequest.adSetName())));


//            TargetingGeoLocation targetingGeoLocation = getTargetingGeoLocation(createAdSetRequest);
//            List<Map<String, Object>> fetchedInterest = fetchInterest(createAdSetRequest.interest());
//
//            List<IDName> fieldInterests = fetchedInterest.stream().flatMap(m -> m.entrySet().stream())
//                    .map(m -> new IDName().setFieldId(m.getKey())
//                            .setFieldName(m.getValue().toString()))
//                    .collect(Collectors.toList());

//
            Targeting fieldTargeting = new AdSet(adset.getId(), apiContext).get().requestTargetingField(true)
                    .requestIdField()
                    .execute().getFieldTargeting();

            List<RawCustomAudience> fieldCustomAudiences = fieldTargeting.getFieldCustomAudiences();


            fieldCustomAudiences.addLast(new RawCustomAudience()
                    .setFieldName("Collection Engagement Audience click")
                    .setFieldId("120202721976300117"));




            fieldTargeting.setFieldCustomAudiences(fieldCustomAudiences);

            new AdSet(adset.getId(),apiContext).update().setTargeting(fieldTargeting).execute();


        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String updateAd(String creativeId) {
        try {


            Ad execute = new Ad("120202394861370117", apiContext).update()
                    .setCreative(new AdCreative(creativeId, apiContext).get().requestAllFields().execute())


                    .execute();
            return "successs";
//            AdAccount adAccount = new AdAccount(accountId, apiContext);
//
//            Campaign campaign = adAccount.getCampaigns().requestAllFields().execute().stream()
//                    .filter(c -> c.getFieldName().equals(createAdRequest.campaignName()))
//                    .findFirst().orElseThrow(() -> new IllegalArgumentException("campaign doesn't exist"));
//
//            APINodeList<AdSet> adSets = campaign.getAdSets().requestIdField().requestNameField().execute();
//
//            String adSetId = adSets.stream()
//                    .filter(adSet -> adSet.getFieldName().equals(createAdRequest.adSetName()))
//                    .findFirst()
//                    .map(AdSet::getId)
//                    .orElseThrow(() -> new IllegalArgumentException(String.format("adset with name s% doesn't exist",
//                            createAdRequest.adSetName())));
//
//
//            Ad ad = adAccount
//                    .createAd()
//
//                    .setName(createAdRequest.adName())
//                    .setAdsetId(adSetId)
//                    .setCreative(new AdCreative(createAdRequest.adCreativeId(),apiContext).get().requestAllFields().execute())
//                    .setStatus(Ad.EnumStatus.VALUE_PAUSED)
//                    .execute();
//
//            return ad.getId();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public String createAd(CreateAdRequest createAdRequest) {
        try {

            AdAccount adAccount = new AdAccount(accountId, apiContext);

            Campaign campaign = adAccount.getCampaigns().requestAllFields().execute().stream()
                    .filter(c -> c.getFieldName().equals(createAdRequest.campaignName()))
                    .findFirst().orElseThrow(() -> new IllegalArgumentException("campaign doesn't exist"));

            APINodeList<AdSet> adSets = campaign.getAdSets().requestIdField().requestNameField().execute();

            String adSetId = adSets.stream()
                    .filter(adSet -> adSet.getFieldName().equals(createAdRequest.adSetName()))
                    .findFirst()
                    .map(AdSet::getId)
                    .orElseThrow(() -> new IllegalArgumentException(String.format("adset with name s% doesn't exist",
                            createAdRequest.adSetName())));

            
            AdCreative adCreative = new AdCreative(createAdRequest.adCreativeId(),apiContext);

            Ad ad = adAccount
                    .createAd()
                    .setName(createAdRequest.adName())
                    .setAdsetId(adSetId)
                    .setCreative(adCreative)

                    .setStatus(Ad.EnumStatus.VALUE_PAUSED)
                    .execute();

            return ad.getId();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }


    public String generatePreview(){

        try{
            APINodeList<AdPreview> adPreviews = new AdAccount(accountId, apiContext).getGeneratePreviews()
                    .setCreative(
                            new AdCreative().setFieldId("")
                    ).setAdFormat(AdPreview.EnumAdFormat.VALUE_INSTAGRAM_REELS)
                    .requestAllFields(true)
                    .execute();

            return adPreviews.getRawResponse();

        }catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }


    private APIRequest.ResponseWrapper getResponseWrapper(DefaultRequestExecutor defaultRequestExecutor, Map<String, Object> requestParams) throws APIException, IOException {
        return defaultRequestExecutor.sendGet(" https://graph.facebook.com/v18.0/search",
                requestParams,
                apiContext
        );
    }

    public List<ProductDetails> createBatchProductItems(List<ProductDetails> prodcutDetails) {


        try (var executor = new StructuredTaskScope.ShutdownOnFailure()) {
            ProductCatalog productCatalog = new ProductCatalog(productCatalogId, apiContext);

            List<StructuredTaskScope.Subtask<ProductDetails>> subtasks = 
//                    Stream.of(
//
//                            new ProductDetails("Anua Heartleaf 77% Soothing Toner",
//                                    "1969",
//
//                                    "/home/michael/Documents/affiliate/amazon/1s.png",
//
//                                    STR."""
//                                            Under Eye Patches (20 Pairs) - Golden Under Eye Mask Amino Acid & Collagen,
//                                            Under Eye Mask for Face, Dark Circles and Puffiness, Beauty & Personal Care
//                                            """,
//                                    "https://amzn.to/3Sek0Ae"
//                            ),
//
//                            new ProductDetails("CeraVe Eye Repair Cream",
//                                    "1379",
//
//                                    "/home/michael/Documents/affiliate/amazon/2s.png",
//
//                                    STR."""
//                                            Under Eye Cream for Dark Circles and Puffiness
//                                            |Suitable for Delicate Skin Under Eye Area | 0.5 Ounce
//                                            """, "https://amzn.to/47zLXHj"
//                            ),
//
//                            new ProductDetails("TruSkin Naturals Vitamin C Face Serum",
//                                    "1977",
//
//                                    "/home/michael/Documents/affiliate/amazon/3s.png",
//
//                                    STR."""
//                                            Anti Aging Face & Eye Serum with Vitamin C,
//                                            Hyaluronic Acid, Vitamin E Brightening Serum, Dark Spot Remover, Even Skin Tone 1 Fl Oz
//                                            """,
//                                    "https://amzn.to/3vJ5uYk"
//                            ),
//
//                            new ProductDetails("CeraVe AM Facial Moisturizing Lotion",
//                                    "1449",
//
//                                    "/home/michael/Documents/affiliate/amazon/4s.png",
//
//                                    STR."""
//                                             SPF 30 | Oil-Free Face Moisturizer with Sunscreen | Non-Comedogenic | 3 Ounce
//                                            """,
//                                    "https://amzn.to/3vzTicv"
//                            ),
//
//                            new ProductDetails("Kitsch Dermaplaning Tool - Face Razors for Women",
//                                    "764",
//
//                                    "/home/michael/Documents/affiliate/amazon/5s.png",
//
//                                    STR."""
//                                            Eyebrow Razor & Face Shaver for Women | Facial Hair Removal for Women
//                                            """,
//                                    "https://amzn.to/47yr0MS"
//                            ),
//
//                            new ProductDetails("Under Eye Patches (20 Pairs)",
//                                    "1479",
//
//                                    "/home/michael/Documents/affiliate/amazon/6s.png",
//
//                                    STR."""
//                                            Golden Under Eye Mask Amino Acid & Collagen,
//                                             Under Eye Mask for Face, Dark Circles and Puffiness, Beauty & Personal Care
//                                            """,
//                                    "https://amzn.to/3Sh9CYF"
//                            )
//
//                    )
                    prodcutDetails.stream()
                            .map(p -> executor.fork(()
                            -> new ProductDetails(p.name(),
                                    p.price(),
                                    createAdMedia(new AdMediaUpload(p.mainImage(), AdTypeEnum.IMAGE)),
                                    p.description(), p.url())
                    )).toList();
            executor.join().throwIfFailed();

            List<ProductDetails> productDetailsList = subtasks.stream()
                    .map(StructuredTaskScope.Subtask::get)
                    .toList();

            List<StructuredTaskScope.Subtask<ProductDetails>> subtaskList = productDetailsList.stream()
                    .map(p -> executor.fork(() -> {
                                try {
                                    productCatalog.createProduct()
                                            .setName(p.name())
                                            .setImageUrl(p.mainImage())
//                                            .setAdditionalImageUrls(p.additonalImages().subList(1, p.additonalImages().size()))
                                            .setAvailability(ProductItem.EnumAvailability.VALUE_IN_STOCK)
                                            .setDescription(p.description())
                                            .setRetailerId("RICK".concat(String.valueOf(Math.random())))
                                            .setBrand("zii")
                                            .setCurrency("USD")
                                            .setColor(p.description())
//                                            .setCategory("c")
                                            .setUrl(p.url())
                                            .setPrice(p.price()).execute().getId();
                                    return new ProductDetails(p.name(), p.price(), p.mainImage(), p.description(), p.url());
                                } catch (Exception e) {
                                    throw new RuntimeException(e.getMessage());
                                }
                            })
                    ).toList();
//
            executor.join().throwIfFailed();

            return subtaskList.stream()
                    .map(StructuredTaskScope.Subtask::get)
                    .collect(Collectors.toList());


        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public List<String> updateProductItems() {
//        return getCategories();
//        return updateSetProductType();
        try(var executor = new StructuredTaskScope.ShutdownOnFailure()){

//            ProductCatalog productCatalog = new ProductCatalog(productCatalogId, apiContext);

            List<ProductItem> productItems = new ProductSet("752229306296318", apiContext).getProducts()
                    .requestImageUrlField(true)
                    .requestIdField(true)

                    .execute().stream().toList();

            Map<String, String> idImageMap = Map.of(
                    "6971937166253689", "/home/michael/Documents/affiliate/amazon/1m.png",
                    "7724823697547487", "/home/michael/Documents/affiliate/amazon/2m.png",
                    "7083110528470292", "/home/michael/Documents/affiliate/amazon/3m.png",
                    "7880015052015459", "/home/michael/Documents/affiliate/amazon/4m.png",
                    "6964510783664559", "/home/michael/Documents/affiliate/amazon/5m.png",
                    "24433362406310139", "/home/michael/Documents/affiliate/amazon/6m.png"



            );

            List<StructuredTaskScope.Subtask<ProductItem>> subtasks = productItems.stream()
                    .map(ProductItem::getId)
                    .filter(idImageMap::containsKey)
                    .map(i -> executor.fork(() -> new ProductItem(i, apiContext)
                            .update()
                            .setImageUrl(createAdMedia(new AdMediaUpload(idImageMap.get(i),AdTypeEnum.IMAGE)))
                            .execute()))
                    .toList();

            executor.join();

           return List.of("");
        }
        catch (Exception e){
            throw new RuntimeException(e.getMessage());
        }
    }

    public List<String> updateSetProductType() {
        try (var executor = new StructuredTaskScope.ShutdownOnFailure()) {


            Map<Integer, String> catMap = Map.of(0, "houses",
                    1, "apartments",
                    2, "cars",
                    3, "building"

            );

//            ProductCatalog productCatalog = new ProductCatalog(productCatalogId, apiContext);


            List<ProductItem> productItems = new ProductSet("1058170845332950", apiContext).getProducts().execute().stream().toList();

//            Map<String, String> idImageMap = Map.of("6453008064803029", "/home/michael/Documents/experts_house/global/g3.jpg",
//
//
//                    "6684367151690788", "/home/michael/Documents/experts_house/meskel_flower/mf2.jpg",
//
//                    "7818666151495802", "/home/michael/Documents/experts_house/round_meskel_flower/selected/rmf1.jpg");

//            Map<String, String> typeMap = Map.of("G+1", "Building", "ሚሸጥ ባለ ሁለት መኝታ አፓርታማ", "Apartments");

            List<StructuredTaskScope.Subtask<ProductItem>> subtasks = IntStream.range(0, productItems.size())

//                    .filter( p -> typeMap.containsKey(p.getFieldName()))
                    .mapToObj(i -> executor.fork(() -> new ProductItem(productItems.get(i).getId(), apiContext)
                            .update()
                            .setProductType(catMap.get(i))
//                            .setCategory("cat"+Math.random())
                            .execute()))
                    .toList();

            executor.join();

            return subtasks.stream().map(StructuredTaskScope.Subtask::get)
                    .map(ProductItem::toString)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public List<String> getCategories() {

        try {
            ProductCatalog productCatalog = new ProductCatalog(productCatalogId, apiContext);


//
//            .getCategories()
//                    .execute().stream()
//                    .collect(Collectors.groupingBy(ProductCatalogCategory::getFieldCriteriaValue))
//

            ProductCatalogCategory execute = productCatalog.createCategory().setData(
                    List.of(
                            Map.of(
                                    "categorization_criteria", "product_type",
                                    "criteria_value", "houses",
                                    "name", "houses",
                                    "description", "Description",
                                    "destination_uri", "https://developers.facebook.com/docs/marketing-api/advantage-catalog-ads/get-started"
                            ),

                            Map.of(
                                    "categorization_criteria", "product_type",
                                    "criteria_value", "apartments",
                                    "name", "apartments",
                                    "description", "Description",
                                    "destination_uri", "https://developers.facebook.com/docs/marketing-api/advantage-catalog-ads/get-started"
                            ),

                            Map.of(
                                    "categorization_criteria", "product_type",
                                    "criteria_value", "cars",
                                    "name", "cars",
                                    "description", "Description",
                                    "destination_uri", "https://developers.facebook.com/docs/marketing-api/advantage-catalog-ads/get-started"
                            ),

                            Map.of(
                                    "categorization_criteria", "product_type",
                                    "criteria_value", "building",
                                    "name", "building",
                                    "description", "Description",
                                    "destination_uri", "https://developers.facebook.com/docs/marketing-api/advantage-catalog-ads/get-started"
                            )


                    )).execute();

            return List.of(execute.getId());
//           return productCatalog.getCategories().
//                    setCategorizationCriteria(ProductCatalogCategory.EnumCategorizationCriteria.VALUE_PRODUCT_TYPE)
//                    .requestCriteriaValueField(true)
//                    .requestNameField(true)
//                                .execute()
//                                .stream()
//                                .map(p -> STR."\{p.getFieldCriteriaValue()} , \{p.getFieldName()}")
//                                .collect(Collectors.toList());


        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }


    public List<String> createProductSet(String name) {


        try (var executor = new StructuredTaskScope.ShutdownOnFailure()) {
            List<StructuredTaskScope.Subtask<ProductItem>> subtasks = new ProductSet("305146685239862", apiContext).getProducts().execute().stream()
                    .map(p -> executor.fork(() -> p.get().requestOrderingIndexField(true)
                            .requestNameField(true)
                            .requestPriceField(true)
                            .requestPriceField(true)
                            .execute())


                    ).toList();

            executor.join();


//
            Map<Boolean, List<ProductItem>> cars = subtasks.stream()
                    .map(StructuredTaskScope.Subtask::get)
                    .filter(p -> p.getFieldProductType() != null)
                    .collect(Collectors.partitioningBy(p -> p.getFieldProductType().equals("CARS")));


            List<ProductItem> productItems = cars.get(false);
            productItems.addAll(cars.get(true));

            IntStream.range(0, productItems.size())
                    .forEach(i -> {
                        try {
                            productItems.get(i).update()
                                    .setOrderingIndex((long) i)
                                    .execute();
                        } catch (APIException e) {
                            throw new RuntimeException(e);
                        }
                    });

            List<StructuredTaskScope.Subtask<ProductItem>> subtaskList = new ProductSet("314373991467477", apiContext).getProducts().execute().stream()
                    .map(p -> executor.fork(() -> p.get()
                            .requestOrderingIndexField(true)
                            .requestNameField(true)
                            .execute())


                    ).toList();

            executor.join();

            return subtaskList.stream()
                    .map(StructuredTaskScope.Subtask::get)

                    .map(p -> String.format(" %s %d", p.getFieldName(),p.getFieldOrderingIndex()))
                                .toList();


//

//                ProductSet productSet = new ProductCatalog(productCatalogId, apiContext).
//                    createProductSet()
//                    .setName("sortedExperts4")
////                        .set
////                    .setOrderingInfo(list)
//                    .setFilter(
//                          JSON."""
//                                  "price": {"gt": 20000000}
//                                  """.toString())
//                    .execute();
//
//            return List.of(productSet.getId());
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
