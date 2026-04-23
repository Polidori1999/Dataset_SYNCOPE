package it.uniroma2.isw2.model;

public class ReleaseSnapshot {
    private final String releaseId;
    private final String releaseName;
    private final int releaseIndex;
    private final String releaseDate;
    private final String snapshotCommitHash;

    public ReleaseSnapshot(String releaseId,
                           String releaseName,
                           int releaseIndex,
                           String releaseDate,
                           String snapshotCommitHash) {
        this.releaseId = releaseId;
        this.releaseName = releaseName;
        this.releaseIndex = releaseIndex;
        this.releaseDate = releaseDate;
        this.snapshotCommitHash = snapshotCommitHash;
    }

    public String getReleaseId() {
        return releaseId;
    }

    public String getReleaseName() {
        return releaseName;
    }

    public int getReleaseIndex() {
        return releaseIndex;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public String getSnapshotCommitHash() {
        return snapshotCommitHash;
    }
}