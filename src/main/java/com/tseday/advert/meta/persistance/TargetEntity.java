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
public class TargetEntity {
    @Id
    private Long id;

    private String name;

    private String topic;

    public TargetEntity(Long id, String name, String topic) {
        this.id = id;
        this.name = name;
        this.topic = topic;
    }
}
