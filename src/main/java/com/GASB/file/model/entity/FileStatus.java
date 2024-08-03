package com.GASB.file.model.entity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "file_status")
public class FileStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @OneToOne
    @JoinColumn(name = "file_id", referencedColumnName = "id")
    private StoredFile storedFile;

    @Builder.Default
    @Column(name = "gscan_status")
    private int GscanStatus = -1;
    @Builder.Default
    @Column(name = "dlp_status")
    private int DlpStatus = -1;
    @Builder.Default
    @Column(name = "vt_status")
    private int VtStatus = -1;

}
