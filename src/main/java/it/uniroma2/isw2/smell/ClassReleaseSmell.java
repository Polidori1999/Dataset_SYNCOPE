package it.uniroma2.isw2.smell;

/**
 * Rappresenta il numero di smell PMD rilevati per una classe
 * in una specifica release.
 */
public class ClassReleaseSmell {

    private final String project;
    private final String releaseId;
    private final String classPath;
    private final int nSmells;

    public ClassReleaseSmell(String project, String releaseId, String classPath, int nSmells) {
        this.project = project;
        this.releaseId = releaseId;
        this.classPath = classPath;
        this.nSmells = nSmells;
    }

    public String getProject() {
        return project;
    }

    public String getReleaseId() {
        return releaseId;
    }

    public String getClassPath() {
        return classPath;
    }

    public int getNSmells() {
        return nSmells;
    }
}