package com.tseday.advert.meta.persistance;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class AdPackage {

    @Id
    @Enumerated(EnumType.STRING)
    private PackageType packageType;

    private double amount;

    private Integer numberOfAds;

    private Integer numberOfCampaigns;


    public AdPackage(PackageType packageType, double amount, Integer numberOfAds, Integer numberOfCampaigns) {
        this.packageType = packageType;
        this.amount = amount;
        this.numberOfAds = numberOfAds;
        this.numberOfCampaigns = numberOfCampaigns;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AdPackage adPackage = (AdPackage) o;
        return packageType == adPackage.packageType && numberOfAds.equals(adPackage.numberOfAds) && numberOfCampaigns.equals(adPackage.numberOfCampaigns);
    }

    @Override
    public int hashCode() {
        return Objects.hash(packageType, numberOfAds, numberOfCampaigns);
    }
}
