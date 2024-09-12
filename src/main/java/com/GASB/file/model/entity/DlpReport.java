package com.GASB.file.model.entity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "dlp_report")
public class DlpReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name="policy_id")
    private int policyId;

    @OneToOne
    @JoinColumn(name = "file_id", nullable = false, referencedColumnName = "id")
    private StoredFile storedFile;

    @Column(name="pii_id")
    private int piiId;

    @Column(name = "info_cnt")
    private int infoCnt;
}
