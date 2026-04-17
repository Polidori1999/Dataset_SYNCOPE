package it.uniroma2.isw2;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;

class RetrieveTicketsID {

    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    public static JSONArray readJsonArrayFromUrl(String url) throws IOException, JSONException {
        InputStream is = new URL(url).openStream();
        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String jsonText = readAll(rd);
            return new JSONArray(jsonText);
        } finally {
            is.close();
        }
    }

    public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
        InputStream is = new URL(url).openStream();
        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String jsonText = readAll(rd);
            return new JSONObject(jsonText);
        } finally {
            is.close();
        }
    }

    public static void main(String[] args) throws IOException, JSONException {
        String projName = "SYNCOPE";
        int i = 0;
        int total;
        int pageSize = 1000;

        FileWriter fileWriter = null;

        try {
            String outName = projName + "Tickets.csv";
            fileWriter = new FileWriter(outName);

            fileWriter.append("TicketID,CreationDate,ResolutionDate,AffectedVersions\n");

            do {
                String url = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22"
                        + projName
                        + "%22AND%22issueType%22=%22Bug%22AND(%22status%22=%22closed%22OR%22status%22=%22resolved%22)"
                        + "AND%22resolution%22=%22fixed%22&fields=key,resolutiondate,versions,created&startAt="
                        + i
                        + "&maxResults="
                        + pageSize;

                JSONObject json = readJsonFromUrl(url);
                JSONArray issues = json.getJSONArray("issues");
                total = json.getInt("total");

                for (int k = 0; k < issues.length(); k++) {
                    JSONObject issue = issues.getJSONObject(k);
                    JSONObject fields = issue.getJSONObject("fields");

                    String key = issue.getString("key");
                    String created = fields.has("created") && !fields.isNull("created")
                            ? fields.getString("created")
                            : "";
                    String resolutionDate = fields.has("resolutiondate") && !fields.isNull("resolutiondate")
                            ? fields.getString("resolutiondate")
                            : "";

                    StringBuilder affectedVersions = new StringBuilder();
                    if (fields.has("versions") && !fields.isNull("versions")) {
                        JSONArray versions = fields.getJSONArray("versions");
                        for (int v = 0; v < versions.length(); v++) {
                            JSONObject versionObj = versions.getJSONObject(v);
                            if (versionObj.has("name")) {
                                if (affectedVersions.length() > 0) {
                                    affectedVersions.append(";");
                                }
                                affectedVersions.append(versionObj.getString("name"));
                            }
                        }
                    }

                    fileWriter.append(escapeCsv(key)).append(",");
                    fileWriter.append(escapeCsv(created)).append(",");
                    fileWriter.append(escapeCsv(resolutionDate)).append(",");
                    fileWriter.append(escapeCsv(affectedVersions.toString())).append("\n");
                }

                i += issues.length();

            } while (i < total);

            System.out.println("CSV creato con successo.");

        } catch (Exception e) {
            System.out.println("Errore nella scrittura del CSV.");
            e.printStackTrace();
        } finally {
            if (fileWriter != null) {
                try {
                    fileWriter.flush();
                    fileWriter.close();
                } catch (IOException e) {
                    System.out.println("Errore nella chiusura del file.");
                    e.printStackTrace();
                }
            }
        }
    }

    private static String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}