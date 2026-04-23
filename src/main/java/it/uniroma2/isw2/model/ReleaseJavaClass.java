package it.uniroma2.isw2.model;

public class ReleaseJavaClass {
    private final String projectName;
    private final String classPath;
    private final String releaseId;
    private final String releaseName;
    private final int releaseIndex;

    public ReleaseJavaClass(String projectName,
                            String classPath,
                            String releaseId,
                            String releaseName,
                            int releaseIndex) {
        this.projectName = projectName;
        this.classPath = classPath;
        this.releaseId = releaseId;
        this.releaseName = releaseName;
        this.releaseIndex = releaseIndex;
    }

    public String getProjectName() {
        return projectName;
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
}