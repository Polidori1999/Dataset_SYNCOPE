package it.uniroma2.isw2.utils;

import it.uniroma2.isw2.model.Release;

import java.util.ArrayList;
import java.util.List;

public class ReleaseSelector {

    private ReleaseSelector() {
    }

    public static List<Release> selectInitialReleases(List<Release> releases, double percentageToKeep) {
        List<Release> selected = new ArrayList<>();

        if (releases.isEmpty()) {
            return selected;
        }

        int numberToKeep = (int) Math.ceil(releases.size() * percentageToKeep);

        for (int i = 0; i < numberToKeep && i < releases.size(); i++) {
            selected.add(releases.get(i));
        }

        return selected;
    }
}