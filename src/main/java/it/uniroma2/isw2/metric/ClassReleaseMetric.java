package it.uniroma2.isw2.metric;

/**
 * Rappresenta le metriche calcolate per una coppia classe-release.
 *
 * Ogni istanza corrisponde a una riga del dataset finale prima del merge
 * con la label buggy yes/no.
 */
public class ClassReleaseMetric {

    private final String project;
    private final String releaseId;
    private final String classPath;

    private final int sizeLoc;
    private final int nom;
    private final double avgMethodSize;
    private final int cycloComplexity;
    private final int fanOut;

    private final int nr;
    private final int nFix;
    private final int nAuth;

    private final int locAdded;
    private final int maxLocAdded;

    private final int churn;
    private final int maxChurn;

    private final int maxChangeSetSize;
    private final double avgModifiedDirs;

    private final long ageSinceLastChange;
    private final double ownershipRatio;

    private final int nSmells;
    private final double smellDensity;

    private final double sameDirectoryChangeRatio;

    private final long age;

    public ClassReleaseMetric(String project,
                              String releaseId,
                              String classPath,
                              int sizeLoc,
                              int nom,
                              double avgMethodSize,
                              int cycloComplexity,
                              int fanOut,
                              int nr,
                              int nFix,
                              int nAuth,
                              int locAdded,
                              int maxLocAdded,
                              int churn,
                              int maxChurn,
                              int maxChangeSetSize,
                              double avgModifiedDirs,
                              long ageSinceLastChange,
                              double ownershipRatio,
                              int nSmells,
                              double smellDensity,
                              double sameDirectoryChangeRatio,
                              long age) {
        this.project = project;
        this.releaseId = releaseId;
        this.classPath = classPath;
        this.sizeLoc = sizeLoc;
        this.nom = nom;
        this.avgMethodSize = avgMethodSize;
        this.cycloComplexity = cycloComplexity;
        this.fanOut = fanOut;
        this.nr = nr;
        this.nFix = nFix;
        this.nAuth = nAuth;
        this.locAdded = locAdded;
        this.maxLocAdded = maxLocAdded;
        this.churn = churn;
        this.maxChurn = maxChurn;
        this.maxChangeSetSize = maxChangeSetSize;
        this.avgModifiedDirs = avgModifiedDirs;
        this.ageSinceLastChange = ageSinceLastChange;
        this.ownershipRatio = ownershipRatio;
        this.nSmells = nSmells;
        this.smellDensity = smellDensity;
        this.sameDirectoryChangeRatio = sameDirectoryChangeRatio;
        this.age = age;
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

    public int getSizeLoc() {
        return sizeLoc;
    }

    public int getNom() {
        return nom;
    }

    public double getAvgMethodSize() {
        return avgMethodSize;
    }

    public int getCycloComplexity() {
        return cycloComplexity;
    }

    public int getFanOut() {
        return fanOut;
    }

    public int getNr() {
        return nr;
    }

    public int getNFix() {
        return nFix;
    }

    public int getNAuth() {
        return nAuth;
    }

    public int getLocAdded() {
        return locAdded;
    }

    public int getMaxLocAdded() {
        return maxLocAdded;
    }

    public int getChurn() {
        return churn;
    }

    public int getMaxChurn() {
        return maxChurn;
    }

    public int getMaxChangeSetSize() {
        return maxChangeSetSize;
    }

    public double getAvgModifiedDirs() {
        return avgModifiedDirs;
    }

    public long getAgeSinceLastChange() {
        return ageSinceLastChange;
    }

    public double getOwnershipRatio() {
        return ownershipRatio;
    }

    public int getNSmells() {
        return nSmells;
    }

    public double getSmellDensity() {
        return smellDensity;
    }

    public double getSameDirectoryChangeRatio() {
        return sameDirectoryChangeRatio;
    }

    public long getAge() {
        return age;
    }

    /*
     * Getter di compatibilità, utili se qualche writer vecchio usa ancora
     * nomi precedenti.
     */
    public int getLoc() {
        return sizeLoc;
    }

    public int getDependencies() {
        return fanOut;
    }

    public double getOwnership() {
        return ownershipRatio;
    }
}