package com.tseday.advert.meta.service;

import com.facebook.ads.sdk.Canvas;
import com.facebook.ads.sdk.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.tseday.advert.meta.dto.AdMediaUpload;
import com.tseday.advert.meta.dto.CreateAdRequest;
import com.tseday.advert.util.ImageProcessor;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.StructuredTaskScope;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tseday.advert.util.ImageProcessor.writeJPG;


@Service
public class AdCreativeService {

    @Value("${meta.accountId}")
    String accountId;

    @Value("${meta.userAccessToken}")
    String userAccessToken;

    private final APIContext apiContext;

    private final MetaAdService metaAdService;

    private final ObjectMapper objectMapper;

    private final StringTemplate.Processor<JsonObject, RuntimeException> JSON;


    private static final Logger LOG = LoggerFactory.getLogger(AdCreativeService.class);


    public AdCreativeService(@Qualifier("metaApiContext") APIContext apiContext,
                             MetaAdService metaAdService,
                             ObjectMapper objectMapper,
                             @Qualifier("jsonProcessor") StringTemplate.Processor<JsonObject, RuntimeException> json) {
        this.apiContext = apiContext;
        this.metaAdService = metaAdService;
        this.objectMapper = objectMapper;
        JSON = json;
    }


    public String createAdFromPost(String pagePostId) {

        var callToAction = JSON."""
                    {
                    "type": "CALL_NOW",
                    "value": {
                   "link":"tel:+251933586527"
                }}
                """;



        try {
            AdCreative adCreative = new AdAccount(accountId, apiContext).createAdCreative()
                    .setObjectStoryId("101086373072062_182718701558118")
//                    .setLinkUrl("https://www.facebook.com/121697844326871/posts/181115178385137")
//                    .setCallToAction(callToAction.toString())
//                    .setCallToAction(callToAction)
//                    .setObjectId("101086373072062")
//                    .setCallToAction()
                    .execute();

            String id = adCreative.getId();

            return metaAdService.createAd(
                    new CreateAdRequest("lensa-construction", "lensa-adSet", id, "test_callNow")
            );

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public String createCarouselAdCreative() {

        var callToAction = JSON."""
                    {
                    "type": "CONTACT_US",
                    "value": {
                    "link":"https://www.facebook.com/profile.php?id=100094599682184"
                }
                }
                """;
        try {


//            List<CompletableFuture<String>> futureList = Stream.of(
//                            "/home/michael/Documents/o1_fit.jpg",
//                            "/home/michael/Documents/o2_fit.jpg",
//                            "/home/michael/Documents/o3_fit.jpg",
//                            "/home/michael/Documents/o4_fit.jpg"
//
//                    )
//                    .map(p -> CompletableFuture.supplyAsync(() -> metaAdService.createAdImage(p)))
//                    .collect(Collectors.toList());
//
//            List<AdAssetFeedSpecImage> photUrlList = futureList.stream()
//                    .map(CompletableFuture::join)
//                    .map(url -> new AdAssetFeedSpecImage().setFieldUrl(url))
//                    .collect(Collectors.toList());

            UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl("https://graph.facebook.com/v18.0/101086373072062_182122181617770")
                    .queryParam("access_token", apiContext.getAccessToken());

            String link = uriComponentsBuilder.build().toUri().toString();


            AdCreative adCreative = new AdAccount(accountId, apiContext).createAdCreative()
                    .setName("gridCarosel")
                    .setObjectStorySpec(
                            new AdCreativeObjectStorySpec()
                                    .setFieldPageId("101086373072062")

                                    .setFieldLinkData(
                                            new AdCreativeLinkData()
                                                    .setFieldCallToAction(
                                                            new AdCreativeLinkDataCallToAction()
                                                                    .setFieldType(AdCreativeLinkDataCallToAction.EnumType.VALUE_CALL_NOW)
                                                                    .setFieldValue(
                                                                            JSON."""
                                                                                    {"link":"tel:+251911853171"}

                                                                                    """.toString()
                                                                    )
                                                    ).setFieldLink("https://www.facebook.com/permalink.php?story_fbid=182208078275847&id=100094599682184&ref=embed_post")




                                    )

                    )
//                    .setObjectType(AdCreative.EnumObjectType.VALUE_SHARE.toString())
//                    .setCallToAction(callToAction.toString())
//                    .setAssetFeedSpec(
//                            new AdAssetFeedSpec()
//                                    .setFieldImages(
//                                            photUrlList
//                                    )
//                                    .setFieldCallToActions(List.of(callToAction.toString()))
//                                    .setFieldCallToActionTypes(List.of(AdAssetFeedSpec.EnumCallToActionTypes.VALUE_CALL_NOW,
//                                            AdAssetFeedSpec.EnumCallToActionTypes.VALUE_CONTACT_US))
//                                    .setFieldBodies(List.of(new AdAssetFeedSpecBody().setFieldText("ORA Cosmetics")))
//                                    .setFieldLinkUrls(List.of(new AdAssetFeedSpecLinkURL().setFieldWebsiteUrl("https://www.facebook.com/profile.php?id=100094599682184")))
//                                    .setFieldAdFormats(List.of( "SINGLE_IMAGE"))
//
//
//                    )


//                    .setActorId("101086373072062")
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


            return metaAdService.createAd(
                    new CreateAdRequest("lensa-construction", "lensa-adSet", id, "test_callNow")
            );

        } catch (Exception e) {
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

    public String createVideoCollectionAds() {
        try {

            var callToAction = JSON."""
                    {
                    "type": "CALL_NOW",
                    "value": {
                   "link":"tel:+251933586527"
                }}
                """;

            Page page = new Page("101086373072062", apiContext);

            //create vide with timeout asynchronously
//
            String videoId = "7260557894006629";

            String canvasVideo = page.createCanvasElement().setCanvasVideo(Map.of("video_id",videoId))
                    .execute().getRawResponseAsJsonObject().get("id").getAsString();

            String buttonId = page.createCanvasElement().setCanvasButton(
                    Map.of("rich_text", Map.of("plain_text", "Chat on WhatsApp"),

                            "open_url_action", Map.of(
                                    "url", "https://wa.me/251988999699"
                )
                    )
            ).execute().getRawResponseAsJsonObject().get("id").getAsString();

            String footerId = page.createCanvasElement().setCanvasFooter(
                    Map.of("child_elements", List.of(buttonId))
            ).execute().getRawResponseAsJsonObject().get("id").getAsString();

            String canvasProductSet = createCollectionProductSet();

            String canvasId =
                    page.createCanvase()
                    .setBodyElementIds(
                            List.of(
                                    canvasVideo,
                                    canvasProductSet,
                                    footerId
                            )
                    ).setName("ora_cosmetics").setIsPublished(true).execute().getRawResponseAsJsonObject().get("id").getAsString();



            AdCreative adCreative = new AdAccount(accountId, apiContext).createAdCreative()
                    .setName("Dynamic Video Collection Ad")
                    .setObjectStorySpec(
                            new AdCreativeObjectStorySpec()
                                    .setFieldVideoData(
                                            new AdCreativeVideoData().setFieldCallToAction(
                                                    new AdCreativeLinkDataCallToAction().setFieldType(AdCreativeLinkDataCallToAction.EnumType.VALUE_LEARN_MORE)
                                                            .setFieldValue(JSON."""
                                                    {"link":"https://fb.com/canvas_doc/\{canvasId}"}
                                                    """.toString()
                                                    )
                                                    )
                                                    .setFieldTitle("Ora Cosmetics Browse Our Porducts")
                                                    .setFieldMessage("Ora Cosmetics")



                                    ).setFieldPageId("101086373072062")

                    ).setObjectType("VIDEO")
//                                        .setDegreesOfFreedomSpec(
//                            new Gson().toJson(
//                                    Map.of("creative_features_spec", Map.of(
//                                            "standard_enhancements", Map.of(
//                                                    "enroll_status", "OPT_OUT"
//                                            )
//                                    ))
//                            ))
                    .execute();

            return metaAdService.createAd(
                    new CreateAdRequest("ora-cosmetics", "ora-adSet", adCreative.getId(), "oraAD"));
//            );

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }


    public String createCollectionProductSet() {
        try {

            Page page = new Page("101086373072062", apiContext);

            CanvasBodyElement canvasBodyElement = page.createCanvasElement().setCanvasProductSet(

                            Map.of("product_set_id", "1739735126465474",
                                    "item_headline", "{{product.name}}",
                                    "item_description", "{{product.current_price}}",
                                    "image_overlay_spec", Map.of(
                                            "overlay_template", "pill_with_text",
                                            "text_type", "price",
                                            "text_font", "dynads_hybrid_bold",
                                            "position", "top_left",
                                            "theme_color", "background_e50900_text_ffffff",
                                            "float_with_margin", true),

                                    "storefront_setting", Map.of(
                                            "enable_sections", true,
                                            "customized_section_titles", List.of(
                                                    Map.of(
                                                            "title_id", "popular",
                                                            "customized_title", "My Populars"
                                                    ),
                                                    Map.of(
                                                            "title_id", "favorites",
                                                            "customized_title", "My Favorites"
                                                    )
                                            ),
                                            "product_set_layout", Map.of("layout_type", "GRID_2COL")

                                    ),
                                    "show_in_feed", true
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
                    .setProductSetId("1992695311102821")
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

    public String createMultiPhotoPost() {

        String scheduledTime = String.valueOf(LocalDateTime.now().plusHours(1).atZone(ZoneId.systemDefault()).toEpochSecond());

        var callToAction = JSON."""
                    {
                    "type": "CALL_NOW",
                    "value": {
                    "link":"tel:+251911853171"
                }
                }
                """;


        try {

            Page page = new Page("101086373072062", apiContext);

            Stream<String> photoPath = Stream.of("/home/michael/Documents/o1_fit.jpg",
                    "/home/michael/Documents/o2_fit.jpg",
                    "/home/michael/Documents/o3_fit.jpg",
                    "/home/michael/Documents/o4_fit.jpg"

            );

            try (var executor = new StructuredTaskScope<String>()) {

                List<StructuredTaskScope.Subtask<String>> subtaskList = photoPath.map(p -> executor
                                .fork(() -> metaAdService.createAdMedia(new AdMediaUpload(p, AdTypeEnum.IMAGE))))
                        .toList();

                executor.join();

                List<StructuredTaskScope.Subtask<String>> subtaskList1 = subtaskList.stream()
                        .map(StructuredTaskScope.Subtask::get)
                        .map(url -> executor.fork(() -> page.createPhoto().setUrl(url).setPublished(false).execute().getId()
                        )).toList();

                executor.join();


                List<Map<String, String>> mediaFbid = subtaskList1.stream()
                        .map(StructuredTaskScope.Subtask::get)
                        .map(p -> Map.of("media_fbid", p))
                        .collect(Collectors.toList());


                JsonArray arrayBuilder = Json.createArrayBuilder(mediaFbid).build();

                return page.createFeed()
                        .setAttachedMedia(arrayBuilder.toString())
                        .setPublished(false)
                        .setCallToAction(callToAction.toString())
                        .execute().getId();

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


        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage());
        }
    }


    public List<String> resizeImage() {

        final int IMG_WIDTH = 554;

        final int IMG_HEIGHT = 395 - (395 / 4);
        int paddingWidth = 100;

//        lensa2ResizedPadding
        Map<Path, Path> pathPathMap = Map.of(
//                Paths.get("/home/michael/Documents/lensa2.jpg"), Paths.get("/home/michael/Documents/lensa2ResizedPadding.jpg"),
                Paths.get("/home/michael/Documents/lensa3.png"), Paths.get("/home/michael/Documents/lensa3t.png")
//                Paths.get("/home/michael/Documents/lensa4.jpg"), Paths.get("/home/michael/Documents/lensa4ResizedPadding.jpg")
        );
        return pathPathMap.entrySet()
                .stream()
                .map(e -> {

                    Path source = e.getKey();
                    Path target = e.getValue();
                    // read an image to BufferedImage for processing
                    try {
                        BufferedImage originalImage = ImageIO.read(source.toFile());

                        BufferedImage scaled = ImageProcessor.getScaledInstance(
                                originalImage, IMG_WIDTH, IMG_HEIGHT, RenderingHints.VALUE_INTERPOLATION_BILINEAR, true);
                        writeJPG(scaled, new FileOutputStream(target.toString()), 0.85f);
//
//                        BufferedImage scaledImage = ImageProcessor.progressiveScaling(
//                                originalImage,
//                               IMG_WIDTH > IMG_HEIGHT ? IMG_WIDTH : IMG_HEIGHT);

//                        BufferedImage newResizedImage
//                                = new BufferedImage(
//                                        originalImage.getWidth() , originalImage.getHeight() + 2 * paddingWidth,
////                                IMG_WIDTH,
////                                IMG_HEIGHT,
//                                BufferedImage.TYPE_INT_ARGB);
//                        Graphics2D g = newResizedImage.createGraphics();
//
//                        g.setBackground(Color.WHITE);
//                        g.setPaint(Color.WHITE);
//
//                        // background transparent
//                        g.setComposite(AlphaComposite.Src);
//                        g.fillRect(0, 0,newResizedImage.getWidth() ,  2 * paddingWidth
//                        );
//
//
//
//
//                        // puts the original image into the newResizedImage
////                        g.drawImage(newResizedImage, 0, 0, IMG_WIDTH, IMG_HEIGHT, null);
//                        g.drawImage(originalImage, 0,  0, null);
//                        g.dispose();
//
//                        // get file extension
//                        String s = target.getFileName().toString();
//                        String fileExtension = s.substring(s.lastIndexOf(".") + 1);
//
//                        // we want image in png format
//                        boolean write = ImageIO.write(newResizedImage, fileExtension, target.toFile());
                        return target.toString();
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }

                }).collect(Collectors.toList());
    }

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
                    .map( r -> {
                        try {
                            return r.execute().getRawResponseAsJsonObject();
                        } catch (APIException e) {
                            throw new RuntimeException(e);
                        }
                    }).map(JsonElement::toString)
                  .collect(Collectors.toList());


//            URI uri = UriComponentsBuilder.fromHttpUrl("https://graph.facebook.com/v18.0/101086373072062/canvases")
//                    .queryParam("access_token", apiContext.getAccessToken())
//                    .queryParam("body_element_ids", List.of("2140200682984001","1527027974796744","7060164790710247"))
//                    .queryParam("name", "ora")
//                    .queryParam("is_published", "true")
//                    .queryParam("is_hidden", "true").build().toUri();




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

        } catch (Exception e) {
            LOG.error(e.getMessage(),e);
            throw new RuntimeException(e.getMessage());

        }
    }

    private APIRequest.ResponseWrapper getResponseWrapper(APIRequest.DefaultRequestExecutor defaultRequestExecutor, Map<String, Object> requestParams) throws APIException, IOException {
        return defaultRequestExecutor.sendPost(" https://graph.facebook.com/v17.0/feed",
                requestParams,
                apiContext
        );
    }
}
