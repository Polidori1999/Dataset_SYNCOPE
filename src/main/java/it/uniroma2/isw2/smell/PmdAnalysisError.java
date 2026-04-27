package it.uniroma2.isw2.smell;

/**
 * Rappresenta un file Java che PMD non è riuscito ad analizzare.
 * La classe resta comunque nel dataset finale, ma l'errore viene tracciato.
 */
public class PmdAnalysisError {

    private final String project;
    private final String releaseId;
    private final String classPath;
    private final String errorMessage;

    public PmdAnalysisError(String project, String releaseId, String classPath, String errorMessage) {
        this.project = project;
        this.releaseId = releaseId;
        this.classPath = classPath;
        this.errorMessage = errorMessage;
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

    public String getErrorMessage() {
        return errorMessage;
    }
}