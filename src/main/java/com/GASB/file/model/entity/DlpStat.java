package com.GASB.file.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "dlp_stat")
public class DlpStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @OneToOne
    @JoinColumn(name = "upload_id", referencedColumnName = "id")
    private FileUpload fileUpload;

    @Builder.Default
    @Column(name = "dlp_status")
    private int dlpStatus = -1;
}
