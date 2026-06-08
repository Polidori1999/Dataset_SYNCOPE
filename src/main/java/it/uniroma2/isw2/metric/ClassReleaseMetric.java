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

    private ClassReleaseMetric(Builder builder) {
        this.project = builder.project;
        this.releaseId = builder.releaseId;
        this.classPath = builder.classPath;
        this.sizeLoc = builder.sizeLoc;
        this.nom = builder.nom;
        this.avgMethodSize = builder.avgMethodSize;
        this.cycloComplexity = builder.cycloComplexity;
        this.fanOut = builder.fanOut;
        this.nr = builder.nr;
        this.nFix = builder.nFix;
        this.nAuth = builder.nAuth;
        this.locAdded = builder.locAdded;
        this.maxLocAdded = builder.maxLocAdded;
        this.churn = builder.churn;
        this.maxChurn = builder.maxChurn;
        this.maxChangeSetSize = builder.maxChangeSetSize;
        this.avgModifiedDirs = builder.avgModifiedDirs;
        this.ageSinceLastChange = builder.ageSinceLastChange;
        this.ownershipRatio = builder.ownershipRatio;
        this.nSmells = builder.nSmells;
        this.smellDensity = builder.smellDensity;
        this.sameDirectoryChangeRatio = builder.sameDirectoryChangeRatio;
        this.age = builder.age;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String project;
        private String releaseId;
        private String classPath;

        private int sizeLoc;
        private int nom;
        private double avgMethodSize;
        private int cycloComplexity;
        private int fanOut;

        private int nr;
        private int nFix;
        private int nAuth;

        private int locAdded;
        private int maxLocAdded;

        private int churn;
        private int maxChurn;

        private int maxChangeSetSize;
        private double avgModifiedDirs;

        private long ageSinceLastChange;
        private double ownershipRatio;

        private int nSmells;
        private double smellDensity;

        private double sameDirectoryChangeRatio;

        private long age;

        private Builder() {
        }

        public Builder identity(String project, String releaseId, String classPath) {
            this.project = project;
            this.releaseId = releaseId;
            this.classPath = classPath;
            return this;
        }

        public Builder sourceMetrics(int sizeLoc,
                                     int nom,
                                     double avgMethodSize,
                                     int cycloComplexity,
                                     int fanOut) {
            this.sizeLoc = sizeLoc;
            this.nom = nom;
            this.avgMethodSize = avgMethodSize;
            this.cycloComplexity = cycloComplexity;
            this.fanOut = fanOut;
            return this;
        }

        public Builder historicalCounts(int nr, int nFix, int nAuth) {
            this.nr = nr;
            this.nFix = nFix;
            this.nAuth = nAuth;
            return this;
        }

        public Builder historicalChanges(int locAdded,
                                         int maxLocAdded,
                                         int churn,
                                         int maxChurn,
                                         int maxChangeSetSize,
                                         double avgModifiedDirs) {
            this.locAdded = locAdded;
            this.maxLocAdded = maxLocAdded;
            this.churn = churn;
            this.maxChurn = maxChurn;
            this.maxChangeSetSize = maxChangeSetSize;
            this.avgModifiedDirs = avgModifiedDirs;
            return this;
        }

        public Builder historicalRatios(long ageSinceLastChange,
                                        double ownershipRatio,
                                        double sameDirectoryChangeRatio,
                                        long age) {
            this.ageSinceLastChange = ageSinceLastChange;
            this.ownershipRatio = ownershipRatio;
            this.sameDirectoryChangeRatio = sameDirectoryChangeRatio;
            this.age = age;
            return this;
        }

        public Builder smellMetrics(int nSmells, double smellDensity) {
            this.nSmells = nSmells;
            this.smellDensity = smellDensity;
            return this;
        }

        public ClassReleaseMetric build() {
            return new ClassReleaseMetric(this);
        }
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
}