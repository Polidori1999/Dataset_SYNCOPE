package it.uniroma2.isw2.model;

public class Release {
    private final int index;
    private final String versionId;
    private final String versionName;
    private final String date;

    public Release(int index, String versionId, String versionName, String date) {
        this.index = index;
        this.versionId = versionId;
        this.versionName = versionName;
        this.date = date;
    }

    public int getIndex() {
        return index;
    }

    public String getVersionId() {
        return versionId;
    }

    public String getVersionName() {
        return versionName;
    }

    public String getDate() {
        return date;
    }
}