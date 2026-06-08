package it.uniroma2.isw2.smell;


import it.uniroma2.isw2.labeling.GitReleaseSnapshotService;
import it.uniroma2.isw2.model.Release;
import it.uniroma2.isw2.model.ReleaseJavaClass;


import java.io.IOException;
import java.nio.file.Path;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Calcola NSmells per ogni coppia classe-release usando PMD.
 * Per ogni release selezionata effettua checkout allo snapshot della release,
 * esegue PMD e associa le violation alle classi presenti nell'inventory.
 */
public class SmellService {
    private static final Logger LOGGER =
            Logger.getLogger(SmellService.class.getName());



    private final GitReleaseSnapshotService snapshotService;
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
        this.snapshotService = new GitReleaseSnapshotService(repositoryPath);
    }

    public SmellComputationResult computeSmells(List<Release> selectedReleases,
                                                List<ReleaseJavaClass> releaseJavaClasses)
            throws IOException {
        List<ClassReleaseSmell> smellRows = new ArrayList<>();
        List<PmdAnalysisError> errorRows = new ArrayList<>();

        String originalCommitHash = snapshotService.getCurrentCommitHash();

        try {
            for (Release release : selectedReleases) {
                String releaseId = release.getVersionId();

                String snapshotCommitHash = snapshotService.findLastCommitOfReleaseDay(
                        release.getDate(),
                        originalCommitHash
                );

                if (snapshotCommitHash.isBlank()) {
                    continue;
                }

                snapshotService.checkout(snapshotCommitHash);

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

                LOGGER.log(Level.INFO,
                        "Smell calcolati per release {0} ({1}).",
                        new Object[]{release.getVersionName(), releaseId});
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interruzione durante il calcolo degli smell.", e);
        } finally {
            try {
                snapshotService.checkout(originalCommitHash);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.log(Level.SEVERE,
                        "Attenzione: impossibile ripristinare il commit originale della repository.",
                        e);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE,
                        "Attenzione: impossibile ripristinare il commit originale della repository.",
                        e);
            }
        }

        return new SmellComputationResult(smellRows, errorRows);
    }


    private String normalizePath(String path) {
        return path.replace("\\", "/").trim();
    }
}