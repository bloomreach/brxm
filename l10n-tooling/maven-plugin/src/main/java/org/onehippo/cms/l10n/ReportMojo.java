package org.onehippo.cms.l10n;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import static org.apache.maven.plugins.annotations.ResolutionScope.RUNTIME;

@Mojo(name = "report", defaultPhase = LifecyclePhase.VALIDATE, requiresDependencyResolution = RUNTIME)
public class ReportMojo extends AbstractRegistrarMojo {

    @Parameter(defaultValue = "${reactorProjects}", required = true, readonly = true)
    private List<MavenProject> reactorProjects;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Report currentReport;
        try {
            currentReport = getRegistrar().report();
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
        if (isLastProject()) {
            if (project.isExecutionRoot()) {
                writeReport(currentReport);
                if (currentReport.errorCount > 0) {
                    throw new MojoExecutionException("Errors in l10n report");
                }
                if (currentReport.failureCount > 0) {
                    throw new MojoFailureException("Failures in l10n report");
                }
            } else {
                boolean errors = false, failures = false;
                for (Report report : getReports()) {
                    errors |= report.errorCount > 0;
                    failures |= report.failureCount > 0;
                    writeReport(report);
                }
                if (errors) {
                    throw new MojoExecutionException("Errors in l10n reports");
                }
                if (failures) {
                    throw new MojoFailureException("Failures in l10n reports");
                }
            }
        }
    }

    private void writeReport(final Report report) {
        if (report.failureCount > 0 || report.errorCount > 0) {
            getLog().info("Module " + report.name + ":");
            getLog().info("Errors: " + report.errorCount + "; Failures: " + report.failureCount);
            for (Report.TestCase testCase : report.testCases) {
                for (Report.Error error : testCase.errors) {
                    getLog().error(testCase.name + ": " + error.message);
                }
                for (Report.Failure failure : testCase.failures) {
                    getLog().error(testCase.name + ": " + failure.message);
                }
            }
        }
    }
    
    private Collection<Report> getReports() throws MojoExecutionException {
        final Collection<Report> reports = new ArrayList<>();
        for (MavenProject project : reactorProjects) {
            final File reportFile = new File(project.getBasedir(), "target/TEST-update.xml");
            if (reportFile.exists()) {
                try {
                    JAXBContext context = JAXBContext.newInstance(Report.class);
                    reports.add((Report) context.createUnmarshaller().unmarshal(reportFile));
                } catch (JAXBException e) {
                    throw new MojoExecutionException(e.getMessage(), e);
                }
            }
        }
        return reports;
    }
    
    private boolean isLastProject() {
        final MavenProject lastProject = reactorProjects.get(reactorProjects.size() - 1);
        return lastProject.equals(project);
    }

}
