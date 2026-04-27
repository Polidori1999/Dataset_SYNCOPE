package it.uniroma2.isw2.smell;

import java.util.List;

/**
 * Contiene il risultato del calcolo degli smell:
 * - righe classe-release con NSmells
 * - file non analizzabili da PMD
 */
public class SmellComputationResult {

    private final List<ClassReleaseSmell> smells;
    private final List<PmdAnalysisError> errors;

    public SmellComputationResult(List<ClassReleaseSmell> smells,
                                  List<PmdAnalysisError> errors) {
        this.smells = smells;
        this.errors = errors;
    }

    public List<ClassReleaseSmell> getSmells() {
        return smells;
    }

    public List<PmdAnalysisError> getErrors() {
        return errors;
    }
}