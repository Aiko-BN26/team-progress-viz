package io.github.aikobn26.teamprogressviz.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "commit_file")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString
public class CommitFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "commit_id")
    @ToString.Exclude
    private GitCommit commit;

    @Column(name = "path", nullable = false)
    private String path;

    @Column(name = "filename", insertable = false, updatable = false)
    private String filename;

    @Column(name = "extension")
    private String extension;

    @Column(name = "status")
    private String status;

    @Column(name = "additions")
    private Integer additions;

    @Column(name = "deletions")
    private Integer deletions;

    @Column(name = "changes")
    private Integer changes;

    @Column(name = "raw_blob_url")
    private String rawBlobUrl;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
