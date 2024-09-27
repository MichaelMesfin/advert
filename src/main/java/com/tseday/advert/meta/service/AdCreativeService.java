package com.tseday.advert.meta.service;

import com.facebook.ads.sdk.*;
import com.facebook.ads.sdk.Canvas;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.tseday.advert.meta.dto.AdMediaUpload;
import com.tseday.advert.meta.dto.CanvasCollectionCreativeData;
import com.tseday.advert.meta.dto.CreateAdRequest;
import com.tseday.advert.meta.dto.PostContent;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.StructuredTaskScope;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AdCreativeService {

    @Value("${meta.accountId}")
    String accountId;

    @Value("${meta.userAccessToken}")
    String userAccessToken;

    private final APIContext apiContext;

    private final MetaAdService metaAdService;

    private final ObjectMapper objectMapper;

    private static final Logger LOG = LoggerFactory.getLogger(AdCreativeService.class);

    private final String canvasLink;

    public AdCreativeService(@Qualifier("metaApiContext") APIContext apiContext,
            MetaAdService metaAdService,
            ObjectMapper objectMapper) {
        this.canvasLink = "https://fb.com/canvas_doc/";
        this.apiContext = apiContext;
        this.metaAdService = metaAdService;
        this.objectMapper = objectMapper;
    }

    public String createAdFromPost(String pagePostId) {

        try {
            AdCreative adCreative = new AdAccount(accountId, apiContext).createAdCreative()
                    .setObjectStoryId(pagePostId)
                    .setCallToAction(
                            AdCreative.EnumCallToActionType.VALUE_MESSAGE_PAGE
                    )
                    .execute();

            String id = adCreative.getId();

            return metaAdService.createAd(
                    new CreateAdRequest("testCampaign2",
                            "testAdset2", id, "testAd")
            );

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public String createCanvasCollectionAd() {

        try (var executor = new StructuredTaskScope<String>()) {

            Page page = new Page("170337056170312", apiContext);

            String buttonId = page.createCanvasElement().setCanvasButton(
                    Map.of("rich_text", Map.of("plain_text", "Shop Now"),
                            "open_url_action", Map.of(
                                    "url", "https://amzn.to/3Sek0Ae"
                            )
                    )
            ).execute().getRawResponseAsJsonObject().get("id").getAsString();

//
            String photoPath;

            try (Stream<Path> stream = Files.walk(Paths.get("/home/michael/Documents/affiliate/images"))) {
                photoPath = stream.map(Path::normalize)
                        .filter(Files::isRegularFile)
                        .map(Path::toString)
                        .sorted()
                        .findFirst().orElse(null);

            }

            StructuredTaskScope.Subtask<String> stringSubtask = executor.fork(() -> metaAdService.createAdMedia(new AdMediaUpload(photoPath, AdTypeEnum.IMAGE)));

            executor.join();

            String link = page.createPhoto()
                    .setUrl(stringSubtask.get())
                    .setPublished(false).execute().getFieldLink();

//
//            String canvasPhotoId = page.createCanvasElement().setCanvasPhoto(Map.of("photo_id", photoId))
//                    .execute().getRawResponseAsJsonObject().get("id").getAsString();
//
            String canvasVideoId = page.createCanvasElement().setCanvasVideo(Map.of("video_id", "894732325512013"))
                    .execute().getRawResponseAsJsonObject().get("id").getAsString();

            List<String> list = new ProductSet("1483606819163045", apiContext).getProducts()
                    .execute().stream().map(ProductItem::getId)
                    .toList();

            Map<String, Object> productList = Map.of(
                    "name", "Medicinal Garden Kit",
                    "product_id_list", list,
                    "top_padding", 24
            );

            String canvasProductListElement = page.createCanvasElement().setCanvasProductList(
                    productList
            )
                    .execute().getRawResponseAsJsonObject().get("id").getAsString();

            String footerId = page.createCanvasElement().setCanvasFooter(
                    Map.of("child_elements", List.of(buttonId,
                            "background_color", "blue"
                    ))
            ).execute().getRawResponseAsJsonObject().get("id").getAsString();

            //try with canvasPhotoIds
            String canvasId
                    = page.createCanvase()
                            .setBodyElementIds(
                                    List.of(canvasVideoId, canvasProductListElement, footerId)
                            ).setName("videoCanvas").setIsPublished(true).execute().getRawResponseAsJsonObject().get("id").getAsString();

            String elementImageCrops = """
                                       {
                                                               "100x100": [
                                                                   [
                                                                       0,
                                                                       0
                                                                   ],
                                                                   [
                                                                       100,
                                                                       100
                                                                   ]
                                                               ]
                                                           }""";

            AdCreative adCreative = new AdAccount(accountId, apiContext).createAdCreative()
                    .setName("videoCreative")
                    .setObjectStorySpec(
                            new AdCreativeObjectStorySpec()
                                    .setFieldPageId("170337056170312")
                                    .setFieldLinkData(
                                            new AdCreativeLinkData()
                                                    .setFieldCallToAction(
                                                            new AdCreativeLinkDataCallToAction()
                                                                    .setFieldType(AdCreativeLinkDataCallToAction.EnumType.VALUE_LEARN_MORE)
                                                    //                                                                    .setFieldValue(
                                                    //                                                                            new AdCreativeLinkDataCallToActionValue()
                                                    //                                                                                    .setFieldLink(STR."https://fb.com/canvas_doc/\{canvasId}")
                                                    //                                                                    )
                                                    ).setFieldLink(
                                                            "https://fb.com/canvas_doc/" + canvasId
                                                    )
                                                    .setFieldMessage(
                                                            "  Browse the product details below"
                                                    )
                                                    .setFieldName("A Complete Natural Pharmacy in Your Backyard")
                                                    .setFieldCollectionThumbnails(
                                                            List.of(
                                                                    new AdCreativeCollectionThumbnailInfo()
                                                                            .setFieldElementCrops(
                                                                                    elementImageCrops
                                                                            )
                                                                            //                                                                            .setFieldElementChildIndex(0L)
                                                                            .setFieldElementId(canvasProductListElement)
                                                                            .setFieldElementChildIndex(0L),
                                                                    new AdCreativeCollectionThumbnailInfo()
                                                                            .setFieldElementChildIndex(1L)
                                                                            .setFieldElementId(canvasProductListElement),
                                                                    new AdCreativeCollectionThumbnailInfo()
                                                                            .setFieldElementChildIndex(2L)
                                                                            .setFieldElementId(canvasProductListElement),
                                                                    new AdCreativeCollectionThumbnailInfo()
                                                                            .setFieldElementChildIndex(3L)
                                                                            .setFieldElementId(canvasProductListElement)
                                                            )
                                                    )
                                    )
                    )
                    .setObjectType("VIDEO")
                    .setDegreesOfFreedomSpec(
                            new Gson().toJson(
                                    Map.of("creative_features_spec", Map.of(
                                            "standard_enhancements", Map.of(
                                                    "enroll_status", "OPT_OUT"
                                            )
                                    ))
                            ))
                    .execute();
            String id = adCreative.getId();

//            ProductItem df = new ProductItem("df", apiContext);
//            //do this 3 more times
//            df.copyFrom(df).update().setColor("dfd");
            return metaAdService.createAd(
                    new CreateAdRequest("universal", "unviersalAdset", id, "medicinal_garden_kit")
            );

        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage());
        }
    }

    public List<String> createCanvasElement() {
        try {

            Page page = new Page("101086373072062", apiContext);

            CanvasBodyElement bodyElement = page
                    .createCanvasElement().setCanvasHeader(
                            Map.of("child_elements", List.of("993873058430432"))
                    )
                    .execute();
            //            page.createCanvasElement().setCanvasButton(Map.of("type",))

            ;

            return page.getCanvasElements().execute().stream()
                    .map(CanvasBodyElement::toString)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    //create vide with timeout asynchronously
//
//            String videoId = "2028427567513967";
//            String canvasVideo = page.createCanvasElement().setCanvasVideo(Map.of("video_id",videoId))
//                    .execute().getRawResponseAsJsonObject().get("id").getAsString();
//
//            String buttonId = page.createCanvasElement().setCanvasButton(
//                    Map.of("rich_text", Map.of("plain_text","Visit Page"),
//
//                            "open_url_action", Map.of(
//                                    "url", "https://www.facebook.com/profile.php?id=61554086474231"
//                )
//                    )
//            ).execute().getRawResponseAsJsonObject().get("id").getAsString();
    public String createVideoCollectionAds(CanvasCollectionCreativeData creativeData) {
        try {

//
            Page page = new Page(creativeData.pageId(), apiContext);

//            String url = metaAdService.createAdMedia(new AdMediaUpload("/home/michael/Documents/affiliate/amazon/bannerb.png", AdTypeEnum.IMAGE));
//
//            String photoId = page.createPhoto().setUrl(url).setPublished(false)
//                    .execute().getId();
//            String canvasPhoto = page.createCanvasElement().setCanvasPhoto(Map.of("photo_id", photoId))
//                    .execute().getRawResponseAsJsonObject().get("id").getAsString();
            String videoId = page.createCanvasElement()
                    .setCanvasVideo(Map.of("video_id", creativeData.videoId()))
                    .execute().getRawResponseAsJsonObject().get("id").getAsString();

            String buttonId = page.createCanvasElement().setCanvasButton(
                    Map.of("rich_text", Map.of("plain_text", "CONTACT US"),
                            "open_url_action", Map.of(
                                    "url", "https://t.me/Ziiipapi"
                            )
                    )
            ).execute().getRawResponseAsJsonObject().get("id").getAsString();

            String footerId = page.createCanvasElement().setCanvasFooter(
                    Map.of(
                            "child_elements", List.of(buttonId),
                            "background_color", "blue"
                    )
            ).execute().getRawResponseAsJsonObject().get("id").getAsString();

            String canvasProductSet = createCollectionProductSet(
                    creativeData.productSetId(),
                    creativeData.pageId()
            );

            String canvasId
                    = page.createCanvase()
                            .setBodyElementIds(
                                    List.of(
                                            videoId,
                                            canvasProductSet,
                                            footerId
                                    )
                            ).setName("ziiCanvas")
                            .setIsPublished(true)
                            .execute().getRawResponseAsJsonObject().get("id").getAsString();

            String id = new AdAccount(accountId, apiContext).createAdCreative()
                    .setName("zii_ie2")
                    .setObjectStorySpec(
                            new AdCreativeObjectStorySpec()
                                    .setFieldVideoData(
                                            new AdCreativeVideoData()
                                                    .setFieldCallToAction(
                                                            new AdCreativeLinkDataCallToAction()
                                                                    .setFieldType(AdCreativeLinkDataCallToAction.EnumType.VALUE_LEARN_MORE)
                                                                    .setFieldValue(
                                                                            new AdCreativeLinkDataCallToActionValue()
                                                                                    .setFieldLink(
                                                                                            canvasLink.concat(canvasId)
                                                                                    )
                                                                    )
                                                    )
                                                    .setFieldMessage(creativeData.message())
                                                    .setFieldTitle(creativeData.title())
                                    )
                                    .setFieldPageId(creativeData.pageId())
                    //                                    .setFieldLinkData(
                    //                                            new AdCreativeLinkData()
                    //                                                    .setFieldLink(STR."https://fb.com/canvas_doc/\{canvasId}")
                    //                                                    .setFieldMessage(STR."""
                    //                                                            New Year New You, SHOP BEST SELLING PERSONAL CARES UNDER $20
                    //                                                            """.trim())
                    //                                                    .setFieldName("Browse available products below")
                    //                                    ).setFieldPageId("101086373072062")
                    )
                    .setObjectType("VIDEO")
                    //                    .setDegreesOfFreedomSpec(
                    //                            new Gson().toJson(
                    //                                    Map.of("creative_features_spec", Map.of(
                    //                                            "standard_enhancements", Map.of(
                    //                                                    "enroll_status", "OPT_OUT"
                    //                                            )
                    //                                    ))
                    //                            ))
                    .execute().getId();

            return id;
//

//            return metaAdService.createAd(
//                    new CreateAdRequest("productSales",
//                            "salesCostCap",
//                            adCreative.getId(),
//                            "productSales"));
//            return metaAdService.updateAd(adCreative.getId());
//            );
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public String videoAd() {

        try {
            AdAccount adAccount = new AdAccount(accountId, apiContext);

            AdCreative test = adAccount.createAdCreative()
                    .setObjectStorySpec(new AdCreativeObjectStorySpec().setFieldVideoData(
                            new AdCreativeVideoData()
                                    .setFieldVideoId("3235763586555092")
                                    .setFieldImageHash("3517f7029eda388a7b667a49f009f928")
                                    .setFieldTitle("ባቋራጭ")
                                    .setFieldCallToAction(new AdCreativeLinkDataCallToAction()
                                            .setFieldType(AdCreativeLinkDataCallToAction.EnumType.VALUE_WATCH_VIDEO)
                                            .setFieldValue(
                                                    new AdCreativeLinkDataCallToActionValue()
                                                            .setFieldLink("https://www.youtube.com/watch?v=bjPPAa3-GDs")
                                            ))
                    ).setFieldPageId("1573747589539364")
                    )
                    .setDegreesOfFreedomSpec(
                            new Gson().toJson(
                                    Map.of("creative_features_spec", Map.of(
                                            "standard_enhancements", Map.of(
                                                    "enroll_status", "OPT_OUT"
                                            )
                                    ))
                            ))
                    .execute();

            return test.getId();

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

    public String createCollectionProductSet(String prodcutSetId, String pageId) {
        try {

            Page page = new Page(pageId, apiContext);

            CanvasBodyElement canvasBodyElement = page.createCanvasElement().setCanvasProductSet(
                    Map.of(
                            "product_set_id", prodcutSetId,
                            "item_headline", "{{product.name}}",
                            "item_description", "{{product.description}}",
                            "image_overlay_spec", Map.of(
                                    "overlay_template", "triangle_with_text",
                                    "text_type", "custom",
                                    "custom_text_type", "free_shipping",
                                    "text_font", "lato_regular",
                                    "position", "top_left",
                                    "theme_color", "background_e50900_text_ffffff",
                                    "float_with_margin", true),
                            "storefront_setting", Map.of(
                                    "enable_sections", true,
                                    "customized_section_titles", List.of(
                                            Map.of(
                                                    "title_id", "popular",
                                                    "customized_title", "Populars"
                                            ),
                                            Map.of(
                                                    "title_id", "favorites",
                                                    "customized_title", "Favorites"
                                            )
                                    ),
                                    "product_set_layout", Map.of("layout_type", "GRID_3COL")
                            ),
                            "show_in_feed", true,
                            "retailer_item_ids", List.of(0, 0, 0)
                    )
            )
                    .execute();

            return canvasBodyElement.getRawResponseAsJsonObject().get("id").getAsString();

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public String createInstantExperience() {
        try {

            AdCreative adCreative = new AdAccount(accountId, apiContext).createAdCreative()
                    .setObjectStorySpec(
                            new AdCreativeObjectStorySpec()
                                    .setFieldTemplateData(
                                            new AdCreativeLinkData()
                                                    .setFieldCallToAction(
                                                            new AdCreativeLinkDataCallToAction()
                                                                    .setFieldType(AdCreativeLinkDataCallToAction.EnumType.VALUE_LEARN_MORE)
                                                    )
                                                    .setFieldLink("https://fb.com/canvas_doc/1482864599168394")
                                                    .setFieldMessage("English Creative message")
                                                    .setFieldName("English Creative title")
                                    //                                                    .setFieldRetailerItemIds(List.of("0", "0", "0", "0"))
                                    )
                                    .setFieldPageId("101086373072062")
                    )
                    //                    .setProductSetId("1992695311102821")
                    .execute();

//
//                    .setDegreesOfFreedomSpec(
//                            new Gson().toJson(
//                                    Map.of("creative_features_spec", Map.of(
//                                            "standard_enhancements", Map.of(
//                                                    "enroll_status", "OPT_OUT"
//                                            )
//                                    ))
//                            ))
//                    .execute();
            String id = adCreative.getId();

            return metaAdService.createAd(
                    new CreateAdRequest("archAngleCampaign", "archAngleAdSet", id, "itemAd")
            );

//            Page page = new Page("101086373072062", apiContext);
//             page.createCanvasElement()
//                    .setCanvasButton(
//                            Map.of("rich_text", Map.of("plain_text", "CONTACT US"),
//                                    "open_url_action", Map.of("url", "https://www.abc.com",
//                                            "tel", "+251933586527")
//                            )).execute();
//
//           page.createCanvasElement()
//                    .setCanvasPhoto(Map.of("photo_id","112072025289453"))
//                    .execute();
//
//             return page.getCanvasElements().execute().stream()
//                     .map(CanvasBodyElement::toString)
//                    .collect(Collectors.toList());
        } catch (Exception e) {

            throw new RuntimeException(e.getMessage());

        }
    }

    public List<String> createMultiPhotoPost(PostContent postContent) {

        Page page = new Page(postContent.pageId(), apiContext);

        return Map.of(
                postContent.directory(),
                postContent.postMessage()
        )
                .entrySet().stream()
                .map(e -> {
                    List<String> photoPath;

                    try (var executor = new StructuredTaskScope<String>()) {

                        try (Stream<Path> stream = Files.walk(Paths.get(e.getKey()))) {
                            photoPath = stream.map(Path::normalize)
                                    .filter(Files::isRegularFile)
                                    .map(Path::toString)
                                    .sorted().toList();
                        }
                        List<StructuredTaskScope.Subtask<String>> subtaskList = photoPath.stream().map(p -> executor
                                .fork(() -> metaAdService.createAdMedia(new AdMediaUpload(p, AdTypeEnum.IMAGE))))
                                .toList();

                        executor.join();

                        List<StructuredTaskScope.Subtask<String>> subtaskList1 = subtaskList.stream()
                                .map(StructuredTaskScope.Subtask::get)
                                .map(url -> executor.fork(() -> page.createPhoto().setUrl(url).setPublished(false)
                                .execute().getId()
                        )).toList();

                        executor.join();

                        List<Map<String, String>> mediaFbid = subtaskList1.stream()
                                .map(StructuredTaskScope.Subtask::get)
                                .map(p -> Map.of("media_fbid", p))
                                .collect(Collectors.toList());

                        JsonArray arrayBuilder = Json.createArrayBuilder(mediaFbid).build();

//                        new Post("",apiContext).
                        return page.createFeed()
                                .setLink("")
                                //                                .setPicture()
                                //                                .setCtaType("MESSAGE")
                                //                                .setOgObjectId("101086373072062")
                                //                                .setOgActionTypeId("1294635240572763")
                                .setMessage(e.getValue())
                                //                                .setAttachedMedia(arrayBuilder.toString())
                                .setPublished(false)
                                //                                .setFormatting(Page.EnumFormatting.VALUE_MARKDOWN)
                                .execute().getId();

                    } catch (Exception ex) {
                        ex.printStackTrace();
                        throw new RuntimeException(ex);
                    }

                }).collect(Collectors.toList());
    }

    public String createSingleImagePost() {
        try {

            //hash 18eecca1538bb9f4b8d2fa2de926758a
//            Page page = new Page("101086373072062", apiContext);
            String filePath = "/home/michael/Documents/affiliate/amazon/audible/audibleDropShadowBlackMidnightBlueZephyrusText.png";

            String imageUrl
                    = metaAdService.createAdMedia(new AdMediaUpload(filePath, AdTypeEnum.IMAGE));

            AdAccount adAccount = new AdAccount(accountId, apiContext);

            AdCreative test = adAccount.createAdCreative()
                    .setObjectStorySpec(new AdCreativeObjectStorySpec().setFieldLinkData(
                            new AdCreativeLinkData()
                                    .setFieldImageHash(imageUrl)
                                    .setFieldLink("https://amzn.to/3IoEl09")
                                    .setFieldCallToAction(new AdCreativeLinkDataCallToAction()
                                            .setFieldType(AdCreativeLinkDataCallToAction.EnumType.VALUE_SIGN_UP))
                                    .setFieldMessage("test")
                    ).setFieldPageId("101086373072062")
                    )
                    .setDegreesOfFreedomSpec(
                            new Gson().toJson(
                                    Map.of("creative_features_spec", Map.of(
                                            "standard_enhancements", Map.of(
                                                    "enroll_status", "OPT_OUT"
                                            )
                                    ))
                            ))
                    .execute();

            return test.getId();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public String createVideoPost() {
        try {

            Page page = new Page("383979581458542", apiContext);

            return page.createFeed()
                    .setThumbnail(new File("/home/michael/work/beakuarach/0.jpg"))
                    .setLink("https://www.youtube.com/watch?v=bjPPAa3-GDs")
                    .setSource("https://www.youtube.com/embed/bjPPAa3-GDs")
                    .setMessage("ባቋራጭ")
                    .setPublished(true)
                    .execute().getId();

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);

        }
    }

    public String generatePreview() {

        try {

//            new AdCreative("120204205429290117",apiContext).getPreviews().execute();
            String rawResponse = new AdAccount(accountId, apiContext).getGeneratePreviews()
                    .setCreative(
                            new AdCreative().setFieldId("120204205429290117")
                    )
                    .setAdFormat(AdPreview.EnumAdFormat.VALUE_INSTAGRAM_PROFILE_FEED)
                    .execute().getRawResponse();

            JsonReader reader = Json.createReader(new StringReader(rawResponse));
            return reader.readObject().toString();

        } catch (Exception e) {

            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }

    }

    @NotNull
    public String publishPosts(List<String> postId) {
        try (var ex = new StructuredTaskScope.ShutdownOnFailure()) {
            postId.stream().map(id -> ex.fork(() -> new PagePost(id, apiContext).update()
                    .setIsPublished(true)
                    .execute().getId()
            )).collect(Collectors.toList());

            ex.join();

            return "published";

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

//
//
//
//           new DynamicPostChildAttachment().set
//
//            Page execute = new Page("",apiContext).createFeed()
////                    .setAttachedMedia(photoIds.stream().map(p ->Map.of("media_fbid",p)).collect(Collectors.toList()))
//                    .setObjectAttachment("")
//                    .setPublished(false)
//                    .setct
//                    .setCallToAction(callToAction)
//                    .set
//                    .execute();
//            return execute.getId();
//            UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl("https://graph.facebook.com/v18.0/101086373072062_182740701555918")
//                    .queryParam("access_token", apiContext.getAccessToken())
//                    .queryParam("object_attachment", "182718701558118")
//                    .queryParam("call_to_action", callToAction.toString())
//                    .queryParam("cta_type","CALL_NOW")
//                    .queryParam("cta_link","tel:+251911853171")
//                    .queryParam("story_fbid","182122181617770")
//                    .queryParam("og_action_type_id","1226135157422772")
//                    .queryParam("story","")
//                    .queryParam("message","")
//                    .queryParam("attachments","182733491556639")
//                    .queryParam()
//                    .queryParam("published", false);
//                    .queryParam("attachments","https://www.facebook.com/121697844326871/posts/182122181617770");
//
//            IntStream.range(0, photoIds.size())
//                    .forEach(i -> {
//                        var mediaObject = JSON. """
//                                {
//                                "media_fbid":\{ photoIds.get(i) }
//                                }
//                                """ ;
//                        uriComponentsBuilder.queryParam(STR."attached_media[\{ i }]" , mediaObject);
//                    });
//            URI uri = uriComponentsBuilder.build().toUri();
//
//
//            HttpRequest request = HttpRequest.newBuilder(uri)
//                    .POST(HttpRequest.BodyPublishers.noBody())
//                    .build();
//
//
//            HttpResponse<String> send = HttpClient.newHttpClient()
//                    .send(request, HttpResponse.BodyHandlers.ofString());
//
//
//            String body = send.body();
//            return body;
//        } catch (Exception e) {
//            LOG.error(e.getMessage(), e);
//            throw new RuntimeException(e.getMessage());
//        }
    public void updatePost(String postId) {
        try {

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public List<String> createCanvas() {

        try {
            Page page = new Page("101086373072062", apiContext);

//            Canvas execute = page.createCanvase()
//                    .setBodyElementIds(
//                            List.of("2140200682984001","1527027974796744","7060164790710247")
//                    )
//                    .setName("ora")
//                    .execute();
            return page.getCanvases().execute()
                    .stream()
                    .map(Canvas::get)
                    .map(r -> {
                        try {
                            return r.execute().getRawResponseAsJsonObject();
                        } catch (APIException e) {
                            throw new RuntimeException(e);
                        }
                    }).map(JsonElement::toString)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage());

        }
    }

    private APIRequest.ResponseWrapper getResponseWrapper(APIRequest.DefaultRequestExecutor defaultRequestExecutor, Map<String, Object> requestParams) throws APIException, IOException {
        return defaultRequestExecutor.sendPost(" https://graph.facebook.com/v20.0/feed",
                requestParams,
                apiContext
        );
    }
}
