package com.tseday.advert.meta.dto;

import java.util.List;

public record AdSetLocationTargetRequest(List<String> region, List<CityLocation> city) {
}
