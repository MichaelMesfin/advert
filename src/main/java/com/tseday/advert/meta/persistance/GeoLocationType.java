package com.tseday.advert.meta.persistance;

import com.google.gson.Gson;

import java.util.Map;
import java.util.function.BiFunction;

public enum GeoLocationType {

    COUNTRY((q, a) -> Map.of("location_types", "[\"country\",\"city\"]",
            "type", "adgeolocation",
            "limit","1000",
            "q",q,
            "access_token", a)),
    REGION((l, a) -> Map.of("location_types", "[\"region\"]",
            "type", "adgeolocation",
            "limit","1000",
            "country_code","ET",
            "q",l,
            "access_token", a)),

    CITY((l, a) -> Map.of("location_types", "[\"city\"]",
            "type", "adgeolocation",
            "limit", "1000",
            "country_code", "ET",
//            "region_id",1034,
            "q", l,
            "access_token", a));

    public Map<String, Object> getRequestParameters(String location, String accessToken) {
        return requestParameters.apply(location, accessToken);
    }

    private BiFunction<String, String, Map<String, Object>> requestParameters;

    GeoLocationType(BiFunction<String, String, Map<String, Object>> requestParameters) {

        this.requestParameters = requestParameters;
    }


}
