package com.tseday.advert.meta.dto;

import com.tseday.advert.meta.service.AdTypeEnum;

public record AdMediaUpload(String url, AdTypeEnum type) {
}
