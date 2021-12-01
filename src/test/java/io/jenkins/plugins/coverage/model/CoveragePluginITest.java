package io.jenkins.plugins.coverage.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.Run;
import jenkins.model.ParameterizedJobMixIn.ParameterizedJob;

import io.jenkins.plugins.coverage.CoveragePublisher;
import io.jenkins.plugins.coverage.adapter.CoberturaReportAdapter;
import io.jenkins.plugins.coverage.adapter.JacocoReportAdapter;
import io.jenkins.plugins.coverage.threshold.Threshold;
import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerSuite;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for the coverage API plugin.
 *
 * @author Ullrich Hafner
 */
public class CoveragePluginITest extends IntegrationTestWithJenkinsPerSuite {

    // TODO: other possibility than duplicating files because of different ressource folder ?
    private static final String JACOCO_ANALYSIS_MODEL_FILE_NAME = "jacoco-analysis-model.xml";
    private static final String JACOCO_CODING_STYLE_FILE_NAME = "jacoco-codingstyle.xml";
    private static final String JACOCO_CODING_STYLE_DECREASED_FILE_NAME = "jacoco-codingstyle-2.xml";
    private static final String COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_FILE_NAME = "coverage-with-lots-of-data.xml";
    private static final String COBERTURA_COVERAGE_FILE_NAME = "cobertura-coverage.xml";

    private static final int JACOCO_ANALYSIS_MODEL_TOTAL_LINES = 6368;
    private static final int JACOCO_ANALYSIS_MODEL_COVERED_LINES = 6083;
    private static final int JACOCO_CODING_STYLE_TOTAL_LINES = 323;
    private static final int JACOCO_CODING_STYLE_COVERED_LINES = 294;
    private static final int COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_COVERED_LINES = 602;
    private static final int COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_TOTAL_LINES = 958;

    private static final int COBERTURA_COVERAGE_COVERED_LINES = 2;
    private static final int COBERTURA_COVERAGE_TOTAL_LINES = 2;

    /** Example integration test for a freestyle build with code coverage. */
    @Test
    public void coveragePluginFreestyleHelloWorld() {
        // automatisch 1. Jenkins starten
        // automatisch 2. Plugin deployen
        // 3a. Job erzeugen
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_ANALYSIS_MODEL_FILE_NAME);
        // 3b. Job konfigurieren// 3a. Job erzeugen
        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_ANALYSIS_MODEL_FILE_NAME);
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        verifySimpleCoverageNode(project,
                JACOCO_ANALYSIS_MODEL_COVERED_LINES,
                JACOCO_ANALYSIS_MODEL_TOTAL_LINES - JACOCO_ANALYSIS_MODEL_COVERED_LINES);
    }

    /** Test with no adapters */
    @Test
    public void freestyleWithEmptyAdapters() {
        FreeStyleProject project = createFreeStyleProject();

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        coveragePublisher.setAdapters(Collections.emptyList());
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(coverageResult).isEqualTo(null);
    }

    /** Test with JacocoAdapter and no files */
    @Test
    public void freestyleJacocoWithEmptyFiles() {
        FreeStyleProject project = createFreeStyleProject();

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter("*.xml");
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(coverageResult).isEqualTo(null);
    }

    /** Test with one Jacoco file */
    @Test
    public void freestyleJacocoWithOneFile() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_ANALYSIS_MODEL_FILE_NAME);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_ANALYSIS_MODEL_FILE_NAME);
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        // 6. Mit Assertions Ergebnisse überprüfen
        assertThat(build.getNumber()).isEqualTo(1);

//        assertLineCoverageResults(Arrays.asList(TOTAL_LINES_JACOCO_ANALYSIS_MODEL),
//                Arrays.asList(COVERED_LINES_JACOCO_ANALYSIS_MODEL), coverageResult);
    }

    /** Test with two Jacoco files */
    @Test
    public void freestyleJacocoWithTwoFiles() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_ANALYSIS_MODEL_FILE_NAME, JACOCO_CODING_STYLE_FILE_NAME);

        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter("*.xml");
        CoveragePublisher coveragePublisher = new CoveragePublisher();
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));

        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getNumber()).isEqualTo(1);

        assertLineCoverageResults(Arrays.asList(JACOCO_ANALYSIS_MODEL_TOTAL_LINES, JACOCO_CODING_STYLE_TOTAL_LINES),
                Arrays.asList(JACOCO_ANALYSIS_MODEL_COVERED_LINES, JACOCO_CODING_STYLE_COVERED_LINES), coverageResult);
    }

    /** Test with two Jacoco files and two adapters */
    @Test
    public void freestyleJacocoWithTwoFilesAndTwoAdapters() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_ANALYSIS_MODEL_FILE_NAME, JACOCO_CODING_STYLE_FILE_NAME);

        JacocoReportAdapter jacocoReportAdapterOne = new JacocoReportAdapter(JACOCO_ANALYSIS_MODEL_FILE_NAME);
        JacocoReportAdapter jacocoReportAdapterTwo = new JacocoReportAdapter(JACOCO_CODING_STYLE_FILE_NAME);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        coveragePublisher.setAdapters(Arrays.asList(jacocoReportAdapterOne, jacocoReportAdapterTwo));

        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getNumber()).isEqualTo(1);

        assertLineCoverageResults(Arrays.asList(JACOCO_ANALYSIS_MODEL_TOTAL_LINES, JACOCO_CODING_STYLE_TOTAL_LINES),
                Arrays.asList(JACOCO_ANALYSIS_MODEL_COVERED_LINES, JACOCO_CODING_STYLE_COVERED_LINES), coverageResult);
    }

    private void assertLineCoverageResults(List<Integer> totalLines, List<Integer> coveredLines,
            CoverageBuildAction coverageResult) {
        int totalCoveredLines = coveredLines.stream().mapToInt(x -> x).sum();
        int totalMissedLines =
                totalLines.stream().mapToInt(x -> x).sum() - coveredLines.stream().mapToInt(x -> x).sum();
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage(
                        totalCoveredLines,
                        totalMissedLines
                ));
    }

    /** Test with Cobertura Adapter and no files */
    @Test
    public void freestyleCoberturaWithEmptyFiles() {
        FreeStyleProject project = createFreeStyleProject();

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        CoberturaReportAdapter coberturaReportAdapter = new CoberturaReportAdapter("*.xml");
        coveragePublisher.setAdapters(Collections.singletonList(coberturaReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getNumber()).isEqualTo(1);
        assertThat(coverageResult).isEqualTo(null);
    }

    @Test
    public void freestyleCoberturaWithOneFile() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_FILE_NAME);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        CoberturaReportAdapter coberturaReportAdapter = new CoberturaReportAdapter("*.xml");

        coveragePublisher.setAdapters(Collections.singletonList(coberturaReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getNumber()).isEqualTo(1);

        assertLineCoverageResults(
                Arrays.asList(COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_TOTAL_LINES),
                Arrays.asList(COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_COVERED_LINES),
                coverageResult);
    }

    @Test
    public void freestyleCoberturaWithTwoFiles() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_FILE_NAME, COBERTURA_COVERAGE_FILE_NAME);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        CoberturaReportAdapter coberturaReportAdapter = new CoberturaReportAdapter("*.xml");

        coveragePublisher.setAdapters(Collections.singletonList(coberturaReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getNumber()).isEqualTo(1);

        assertLineCoverageResults(
                Arrays.asList(COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_TOTAL_LINES, COBERTURA_COVERAGE_TOTAL_LINES),
                Arrays.asList(COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_COVERED_LINES, COBERTURA_COVERAGE_COVERED_LINES),
                coverageResult);
    }

    @Test
    public void freestyleWithJacocoAdapterAndCoberturaFile() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_FILE_NAME);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(
                COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_FILE_NAME);

        coveragePublisher.setAdapters(Arrays.asList(jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        assertThat(build.getNumber()).isEqualTo(1);

        assertLineCoverageResults(
                Collections.emptyList(),
                Collections.emptyList(),
                coverageResult);
    }

    @Test
    public void freestyleWithCoberturaAndJacocoFile() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_CODING_STYLE_FILE_NAME, COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_FILE_NAME);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_CODING_STYLE_FILE_NAME);
        CoberturaReportAdapter coberturaReportAdapter = new CoberturaReportAdapter(
                COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_FILE_NAME);

        coveragePublisher.setAdapters(Arrays.asList(coberturaReportAdapter, jacocoReportAdapter));
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        // 6. Mit Assertions Ergebnisse überprüfen
        assertThat(build.getNumber()).isEqualTo(1);

        assertLineCoverageResults(
                Arrays.asList(COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_TOTAL_LINES, JACOCO_CODING_STYLE_TOTAL_LINES),
                Arrays.asList(COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_COVERED_LINES, JACOCO_CODING_STYLE_COVERED_LINES),
                coverageResult);
    }

    @Test
    public void zeroReportsFail() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_FILE_NAME);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        CoberturaReportAdapter coberturaReportAdapter = new CoberturaReportAdapter("*.xml");

//        coveragePublisher.setAdapters(Collections.singletonList(coberturaReportAdapter));
        coveragePublisher.setFailNoReports(true);
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        // 6. Mit Assertions Ergebnisse überprüfen
        assertThat(build.getNumber()).isEqualTo(1);

        // TODO: Niko: complete tests
//        assertThat(coverageResult)
//        assertThatThrownBy(() -> coverageResult.getHealthReport());
    }

    @Test
    public void zeroReportsOkay() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, COBERTURA_COVERAGE_WITH_LOTS_OF_DATA_FILE_NAME);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        CoberturaReportAdapter coberturaReportAdapter = new CoberturaReportAdapter("*.xml");

//        coveragePublisher.setAdapters(Collections.singletonList(coberturaReportAdapter));
        coveragePublisher.setFailNoReports(true);
        project.getPublishersList().add(coveragePublisher);

        Run<?, ?> build = buildSuccessfully(project);
        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);

        // 6. Mit Assertions Ergebnisse überprüfen
        assertThat(build.getNumber()).isEqualTo(1);

        // TODO: Niko: complete tests
//        assertThat(coverageResult)
//        assertThatThrownBy(() -> coverageResult.getHealthReport());
    }

    @Test
    public void freestyleQualityGatesSuccessful() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_ANALYSIS_MODEL_FILE_NAME);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter("*.xml");
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));

        Threshold lineThreshold = new Threshold("Line");
        lineThreshold.setUnhealthyThreshold(95f);
        lineThreshold.setFailUnhealthy(true);

        coveragePublisher.setGlobalThresholds(Collections.singletonList(lineThreshold));
        project.getPublishersList().add(coveragePublisher);
        Run<?, ?> build = buildSuccessfully(project);

        assertThat(build.getResult()).isEqualTo(Result.SUCCESS);
    }

    @Test
    public void freestyleQualityGatesFail() {
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_ANALYSIS_MODEL_FILE_NAME);

        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter("*.xml");
        coveragePublisher.setAdapters(Collections.singletonList(jacocoReportAdapter));

        Threshold lineThreshold = new Threshold("Line");
        lineThreshold.setUnhealthyThreshold(99f);
        lineThreshold.setFailUnhealthy(true);

        coveragePublisher.setGlobalThresholds(Collections.singletonList(lineThreshold));
        project.getPublishersList().add(coveragePublisher);
        Run<?, ?> build = buildWithResult(project, Result.FAILURE);

        assertThat(build.getResult()).isEqualTo(Result.FAILURE);
    }

    @Test
    public void healthReports() {
        // TODO: Niko
    }

    // TODO: Michi: Build is successful. Wrong checks ?
    @Test
    public void failWhenCoverageDecreases() {
        // build 1
        FreeStyleProject project = createFreeStyleProject();
        copyFilesToWorkspace(project, JACOCO_CODING_STYLE_FILE_NAME, JACOCO_CODING_STYLE_DECREASED_FILE_NAME);
        CoveragePublisher coveragePublisher = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapter = new JacocoReportAdapter(JACOCO_CODING_STYLE_FILE_NAME);
        coveragePublisher.setAdapters(Arrays.asList(jacocoReportAdapter));
        coveragePublisher.setFailBuildIfCoverageDecreasedInChangeRequest(true);
        project.getPublishersList().add(coveragePublisher);
        Run<?, ?> build = buildSuccessfully(project);

        // build 2
        CoveragePublisher coveragePublisherTwo = new CoveragePublisher();
        JacocoReportAdapter jacocoReportAdapterTwo = new JacocoReportAdapter(JACOCO_CODING_STYLE_DECREASED_FILE_NAME);
        coveragePublisherTwo.setAdapters(Arrays.asList(jacocoReportAdapterTwo));
        coveragePublisherTwo.setFailBuildIfCoverageDecreasedInChangeRequest(true);
        project.getPublishersList().add(coveragePublisherTwo);
        Run<?, ?> build_two = buildWithResult(project, Result.FAILURE);
        assertThat(build_two.getResult()).isEqualTo(Result.FAILURE);
    }

    @Test
    public void skipChecksWhenPublishing() {
        // TODO: Niko
    }

    // TODO: @All: Check Google DOC for more assigned tests !

    @Test
    public void pipelineCoberturaWithOneFile() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_ANALYSIS_MODEL_FILE_NAME);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [jacocoAdapter('**/*.xml')]"
                + "}", true));

        verifySimpleCoverageNode(job, 6083, 6368 - 6083);

    }

    /** Example integration test for a pipeline with code coverage. */
    @Test
    public void coveragePluginPipelineHelloWorld() {
        WorkflowJob job = createPipelineWithWorkspaceFiles(JACOCO_ANALYSIS_MODEL_FILE_NAME);
        job.setDefinition(new CpsFlowDefinition("node {"
                + "   publishCoverage adapters: [jacocoAdapter('**/*.xml')]"
                + "}", true));

        verifySimpleCoverageNode(job, 6083, 6368 - 6083);

    }

    private void verifySimpleCoverageNode(final ParameterizedJob<?, ?> project, int assertCoveredLines,
            int assertMissedLines) {
        // 4. Jacoco XML File in den Workspace legen (Stub für einen Build)
        // 5. Jenkins Build starten
        Run<?, ?> build = buildSuccessfully(project);

        // 6. Mit Assertions Ergebnisse überprüfen
        assertThat(build.getNumber()).isEqualTo(1);

        CoverageBuildAction coverageResult = build.getAction(CoverageBuildAction.class);
        assertThat(coverageResult.getLineCoverage())
                .isEqualTo(new Coverage(assertCoveredLines, assertMissedLines));
    }

}
