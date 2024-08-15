package com.tseday.advert.meta.persistance;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class GeoLocationEntity {
    @Id
    private String key;
    private String name;
    private String type;

    private String countryCode;

    public GeoLocationEntity(String key, String name, String type, String countryCode) {
        this.key = key;
        this.name = name;
        this.type = type;
        this.countryCode = countryCode;
    }
}
