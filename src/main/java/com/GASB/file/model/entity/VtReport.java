package com.GASB.file.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name="vt_report")
public class VtReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @OneToOne
    @JoinColumn(name = "file_id", referencedColumnName = "id")
    private StoredFile storedFile;

    @Column(name = "type")
    private String type;

    @Column(name = "v3")
    private String V3;

    @Column(name = "alyac")
    private String ALYac;

    @Column(name = "kaspersky")
    private String Kaspersky;

    @Column(name = "falcon")
    private String Falcon;

    @Column(name = "avast")
    private String Avast;

    @Column(name = "sentinelone")
    private String Sentinelone;

    @Column(name = "detect_engine")
    private int detectEngine;
    @Column(name = "complete_engine")
    private int completeEngine;

    @Column(name = "score")
    private int score;
    @Column(name = "threat_label")
    private String threatLabel;

    @Column(columnDefinition = "TEXT", nullable = false, name = "report_url")
    private String reportUrl;
}
