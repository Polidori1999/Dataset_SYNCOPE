package it.uniroma2.isw2;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.nio.charset.StandardCharsets;

class RetrieveTicketsID {
    private static final Logger LOGGER =
            Logger.getLogger(RetrieveTicketsID.class.getName());
    private static final String FIELD_CREATED = "created";
    private static final String FIELD_VERSIONS = "versions";
    private static final String FIELD_RESOLUTIONDATE = "resolutiondate";
    private static final String FIELD_NAME = "name";


    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    public static JSONArray readJsonArrayFromUrl(String url) throws IOException, JSONException {
        try (InputStream is = new URL(url).openStream()) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String jsonText = readAll(rd);
            return new JSONArray(jsonText);
        }
    }

    public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
        try (InputStream is = new URL(url).openStream()) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String jsonText = readAll(rd);
            return new JSONObject(jsonText);
        }
    }

    public static void main(String[] args) throws IOException, JSONException {
        String projName = "SYNCOPE";
        String outName = projName + "Tickets.csv";

        try (FileWriter fileWriter = new FileWriter(outName)) {
            writeTicketsCsv(fileWriter, projName);
            LOGGER.info("CSV creato con successo.");
        } catch (IOException | JSONException e) {
            LOGGER.log(Level.SEVERE, "Errore nella scrittura del CSV.", e);
        }
    }
    private static void writeTicketsCsv(FileWriter fileWriter, String projName)
            throws IOException, JSONException {
        int startAt = 0;
        int total;
        int pageSize = 1000;

        fileWriter.append("TicketID,CreationDate,ResolutionDate,AffectedVersions\n");

        do {
            JSONObject json = readJsonFromUrl(buildSearchUrl(projName, startAt, pageSize));
            JSONArray issues = json.getJSONArray("issues");
            total = json.getInt("total");

            writeIssues(fileWriter, issues);

            startAt += issues.length();

        } while (startAt < total);
    }

    private static String buildSearchUrl(String projName, int startAt, int pageSize) {
        return "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22"
                + projName
                + "%22AND%22issueType%22=%22Bug%22AND(%22status%22=%22closed%22OR%22status%22=%22resolved%22)"
                + "AND%22resolution%22=%22fixed%22&fields=key,resolutiondate,versions,created&startAt="
                + startAt
                + "&maxResults="
                + pageSize;
    }

    private static void writeIssues(FileWriter fileWriter, JSONArray issues)
            throws IOException, JSONException {
        for (int k = 0; k < issues.length(); k++) {
            writeIssue(fileWriter, issues.getJSONObject(k));
        }
    }

    private static void writeIssue(FileWriter fileWriter, JSONObject issue)
            throws IOException, JSONException {
        JSONObject fields = issue.getJSONObject("fields");

        String key = issue.getString("key");
        String created = getOptionalString(fields, FIELD_CREATED);
        String resolutionDate = getOptionalString(fields, FIELD_RESOLUTIONDATE);
        String affectedVersions = buildAffectedVersions(fields);

        fileWriter.append(escapeCsv(key)).append(",");
        fileWriter.append(escapeCsv(created)).append(",");
        fileWriter.append(escapeCsv(resolutionDate)).append(",");
        fileWriter.append(escapeCsv(affectedVersions)).append("\n");
    }

    private static String getOptionalString(JSONObject object, String fieldName)
            throws JSONException {
        return object.has(fieldName) && !object.isNull(fieldName)
                ? object.getString(fieldName)
                : "";
    }

    private static String buildAffectedVersions(JSONObject fields) throws JSONException {
        StringBuilder affectedVersions = new StringBuilder();

        if (fields.has(FIELD_VERSIONS) && !fields.isNull(FIELD_VERSIONS)) {
            appendAffectedVersions(affectedVersions, fields.getJSONArray(FIELD_VERSIONS));
        }

        return affectedVersions.toString();
    }

    private static void appendAffectedVersions(StringBuilder affectedVersions, JSONArray versions)
            throws JSONException {
        for (int v = 0; v < versions.length(); v++) {
            appendAffectedVersion(affectedVersions, versions.getJSONObject(v));
        }
    }

    private static void appendAffectedVersion(StringBuilder affectedVersions, JSONObject versionObj)
            throws JSONException {
        if (versionObj.has(FIELD_NAME)) {
            if (!affectedVersions.isEmpty()) {
                affectedVersions.append(";");
            }
            affectedVersions.append(versionObj.getString(FIELD_NAME));
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