package it.uniroma2.isw2.smell;

import it.uniroma2.isw2.labeling.GitCommandRunner;
import it.uniroma2.isw2.model.Release;
import it.uniroma2.isw2.model.ReleaseJavaClass;
import it.uniroma2.isw2.utils.DateUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Calcola NSmells per ogni coppia classe-release usando PMD.
 * Per ogni release selezionata effettua checkout allo snapshot della release,
 * esegue PMD e associa le violation alle classi presenti nell'inventory.
 */
public class SmellService {

    private static final DateTimeFormatter GIT_BEFORE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String projectName;
    private final Path repositoryPath;
    private final PmdFileListWriter fileListWriter;
    private final PmdRunner pmdRunner;

    public SmellService(String projectName,
                        Path repositoryPath,
                        PmdFileListWriter fileListWriter,
                        PmdRunner pmdRunner) {
        this.projectName = projectName;
        this.repositoryPath = repositoryPath;
        this.fileListWriter = fileListWriter;
        this.pmdRunner = pmdRunner;
    }

    public SmellComputationResult computeSmells(List<Release> selectedReleases,
                                                List<ReleaseJavaClass> releaseJavaClasses)
            throws IOException {
        List<ClassReleaseSmell> smellRows = new ArrayList<>();
        List<PmdAnalysisError> errorRows = new ArrayList<>();

        String originalCommitHash = getCurrentCommitHash();

        try {
            for (Release release : selectedReleases) {
                String releaseId = release.getVersionId();

                String snapshotCommitHash = findLastCommitOfReleaseDay(
                        release.getDate(),
                        originalCommitHash
                );

                if (snapshotCommitHash.isBlank()) {
                    continue;
                }

                checkout(snapshotCommitHash);

                Path fileListPath = fileListWriter.writeFileList(
                        releaseId,
                        repositoryPath,
                        releaseJavaClasses
                );

                PmdRunResult runResult = pmdRunner.run(releaseId, fileListPath);

                Map<String, Integer> violationsByClassPath =
                        PmdReportParser.countViolationsByClassPath(
                                runResult.getCsvReportPath(),
                                repositoryPath
                        );

                errorRows.addAll(PmdReportParser.readAnalysisErrors(
                        projectName,
                        releaseId,
                        runResult.getXmlReportPath(),
                        repositoryPath
                ));

                for (ReleaseJavaClass javaClass : releaseJavaClasses) {
                    if (!releaseId.equals(javaClass.getReleaseId())) {
                        continue;
                    }

                    String classPath = normalizePath(javaClass.getClassPath());
                    int nSmells = violationsByClassPath.getOrDefault(classPath, 0);

                    smellRows.add(new ClassReleaseSmell(
                            projectName,
                            releaseId,
                            classPath,
                            nSmells
                    ));
                }

                System.out.println("Smell calcolati per release "
                        + release.getVersionName()
                        + " (" + releaseId + ").");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interruzione durante il calcolo degli smell.", e);
        } finally {
            try {
                checkout(originalCommitHash);
            } catch (Exception e) {
                System.out.println("Attenzione: impossibile ripristinare il commit originale della repository.");
                e.printStackTrace();
            }
        }

        return new SmellComputationResult(smellRows, errorRows);
    }

    private String findLastCommitOfReleaseDay(String releaseDate,
                                              String referenceCommitHash)
            throws IOException, InterruptedException {
        LocalDateTime endExclusive = DateUtils.parseReleaseDate(releaseDate).plusDays(1);
        String formattedDate = endExclusive.format(GIT_BEFORE_FORMAT);

        List<String> lines = GitCommandRunner.runCommand(
                repositoryPath.toString(),
                "git",
                "rev-list",
                "-n",
                "1",
                "--before=" + formattedDate,
                "--first-parent",
                referenceCommitHash
        );

        if (lines.isEmpty()) {
            return "";
        }

        return lines.get(0).trim();
    }

    private String getCurrentCommitHash() throws IOException {
        try {
            List<String> lines = GitCommandRunner.runCommand(
                    repositoryPath.toString(),
                    "git",
                    "rev-parse",
                    "HEAD"
            );

            if (lines.isEmpty()) {
                return "";
            }

            return lines.get(0).trim();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interruzione durante la lettura del commit corrente.", e);
        }
    }

    private void checkout(String commitHash) throws IOException, InterruptedException {
        if (commitHash == null || commitHash.isBlank()) {
            return;
        }

        GitCommandRunner.runCommand(
                repositoryPath.toString(),
                "git",
                "checkout",
                "-f",
                commitHash
        );
    }

    private String normalizePath(String path) {
        return path.replace("\\", "/").trim();
    }
}