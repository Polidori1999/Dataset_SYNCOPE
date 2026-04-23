package it.uniroma2.isw2.model;

public class BuggyClassReleaseLabel {
    private final String projectName;
    private final String ticketId;
    private final String fixCommitHash;
    private final String classPath;
    private final String releaseId;
    private final String releaseName;
    private final int releaseIndex;
    private final String bugginess;

    public BuggyClassReleaseLabel(String projectName,
                                  String ticketId,
                                  String fixCommitHash,
                                  String classPath,
                                  String releaseId,
                                  String releaseName,
                                  int releaseIndex,
                                  String bugginess) {
        this.projectName = projectName;
        this.ticketId = ticketId;
        this.fixCommitHash = fixCommitHash;
        this.classPath = classPath;
        this.releaseId = releaseId;
        this.releaseName = releaseName;
        this.releaseIndex = releaseIndex;
        this.bugginess = bugginess;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getTicketId() {
        return ticketId;
    }

    public String getFixCommitHash() {
        return fixCommitHash;
    }

    public String getClassPath() {
        return classPath;
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

    public String getBugginess() {
        return bugginess;
    }
}