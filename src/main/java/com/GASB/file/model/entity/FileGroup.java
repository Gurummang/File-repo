package com.GASB.file.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class FileGroup {

    @Id
    private Long id;

    @OneToOne
    @JoinColumn(name = "id", referencedColumnName = "id")
    private Activities activities;

    @Column(columnDefinition = "TEXT")
    private String groupName;
}
