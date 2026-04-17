package it.uniroma2.isw2.io;

import it.uniroma2.isw2.model.Release;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ReleaseCsvReader {

    private ReleaseCsvReader() {
    }

    public static List<Release> loadReleases(String filePath) throws IOException {
        List<Release> releases = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line = br.readLine();

            if (line == null) {
                return releases;
            }

            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",", -1);

                if (parts.length < 4) {
                    continue;
                }

                int index = Integer.parseInt(parts[0].trim());
                String versionId = parts[1].trim();
                String versionName = parts[2].trim();
                String date = parts[3].trim();

                releases.add(new Release(index, versionId, versionName, date));
            }
        }

        releases.sort(Comparator.comparingInt(Release::getIndex));
        return releases;
    }
}