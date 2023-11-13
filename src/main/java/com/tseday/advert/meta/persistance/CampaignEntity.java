package com.tseday.advert.meta.persistance;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class CampaignEntity {
    @Id
    private String id;
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(unique = true)
    private CampaignObjectiveEnums objective;

    public CampaignEntity(String id, String name, CampaignObjectiveEnums objective) {
        this.id = id;
        this.name = name;
        this.objective = objective;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CampaignEntity that = (CampaignEntity) o;
        return id.equals(that.id) && objective == that.objective;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, objective);
    }
}
