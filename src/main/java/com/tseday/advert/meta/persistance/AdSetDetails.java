package com.tseday.advert.meta.persistance;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class AdSetDetails{
   @Id
    private String id;
   @Column(unique = true)
   private String name;
   @ElementCollection
    private Set<String> ads;

   @OneToOne
   private AdPackage adPackage;

   @OneToOne
   private ClientEntity clientEntity;

    public AdSetDetails(String id, String name, AdPackage adPackage, ClientEntity clientEntity) {
        this.id = id;
        this.name = name;
        this.adPackage = adPackage;
        this.clientEntity = clientEntity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AdSetDetails that = (AdSetDetails) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
