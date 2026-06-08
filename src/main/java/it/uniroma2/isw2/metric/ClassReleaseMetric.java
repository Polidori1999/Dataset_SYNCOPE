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

        public Builder project(String value) {
            this.project = value;
            return this;
        }

        public Builder releaseId(String value) {
            this.releaseId = value;
            return this;
        }

        public Builder classPath(String value) {
            this.classPath = value;
            return this;
        }

        public Builder sizeLoc(int value) {
            this.sizeLoc = value;
            return this;
        }

        public Builder nom(int value) {
            this.nom = value;
            return this;
        }

        public Builder avgMethodSize(double value) {
            this.avgMethodSize = value;
            return this;
        }

        public Builder cycloComplexity(int value) {
            this.cycloComplexity = value;
            return this;
        }

        public Builder fanOut(int value) {
            this.fanOut = value;
            return this;
        }

        public Builder nr(int value) {
            this.nr = value;
            return this;
        }

        public Builder nFix(int value) {
            this.nFix = value;
            return this;
        }

        public Builder nAuth(int value) {
            this.nAuth = value;
            return this;
        }

        public Builder locAdded(int value) {
            this.locAdded = value;
            return this;
        }

        public Builder maxLocAdded(int value) {
            this.maxLocAdded = value;
            return this;
        }

        public Builder churn(int value) {
            this.churn = value;
            return this;
        }

        public Builder maxChurn(int value) {
            this.maxChurn = value;
            return this;
        }

        public Builder maxChangeSetSize(int value) {
            this.maxChangeSetSize = value;
            return this;
        }

        public Builder avgModifiedDirs(double value) {
            this.avgModifiedDirs = value;
            return this;
        }

        public Builder ageSinceLastChange(long value) {
            this.ageSinceLastChange = value;
            return this;
        }

        public Builder ownershipRatio(double value) {
            this.ownershipRatio = value;
            return this;
        }

        public Builder nSmells(int value) {
            this.nSmells = value;
            return this;
        }

        public Builder smellDensity(double value) {
            this.smellDensity = value;
            return this;
        }

        public Builder sameDirectoryChangeRatio(double value) {
            this.sameDirectoryChangeRatio = value;
            return this;
        }

        public Builder age(long value) {
            this.age = value;
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