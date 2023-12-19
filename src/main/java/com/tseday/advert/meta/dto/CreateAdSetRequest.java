package com.tseday.advert.meta.dto;

import java.util.List;

public record CreateAdSetRequest(String campaignName,
                                 String adSetName,
                                 AdSetLocationTargetRequest locationTargetRequest,

                                 String interest,

                                 String pageId

                                 ) {
}
