package org.zeroturnaround.jenkins.flowbuildtestaggregator;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

import com.tikal.jenkins.plugins.multijob.MultiJobBuild;

import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixRun;
import hudson.maven.MavenModuleSetBuild;
import hudson.maven.reporters.SurefireAggregatedReport;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Run;
import hudson.tasks.test.TestResultProjectAction;
import jenkins.model.Jenkins;

import org.kohsuke.stapler.DataBoundConstructor;

import com.cloudbees.plugins.flow.BuildFlow;
import com.cloudbees.plugins.flow.FlowRun;
import com.cloudbees.plugins.flow.JobInvocation;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.tasks.junit.TestResultAction;
import hudson.tasks.test.AggregatedTestResultAction.Child;

public class FlowTestAggregator extends Recorder {

  public final boolean showTestResultTrend;

  @DataBoundConstructor
  public FlowTestAggregator(boolean showTestResultTrend) {
    this.showTestResultTrend = showTestResultTrend;
  }

  public BuildStepMonitor getRequiredMonitorService() {
    return BuildStepMonitor.NONE;
  }

  @Override
  public Collection<? extends Action> getProjectActions(AbstractProject<?, ?> project) {
    if (showTestResultTrend) return Collections.<Action>singleton(new TestResultProjectAction(project));
    return Collections.emptyList();
  }

  @Override
  public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
    FlowRun flowRun = (FlowRun) build;
    FlowTestResults testResults = new FlowTestResults();
    flowRun.addAction(testResults);
    listener.getLogger().println("Starting to gather test results!");
    try {
      aggregateResultsFromBuild(flowRun.getStartJob().getBuild(), testResults, listener);
    } catch (ExecutionException e) {
      listener.getLogger().println("ERROR: " + e.getMessage());
    }
    listener.getLogger().println("Gathered results from " + testResults.getChildReports().size() + " jobs" );
    return true;
  }

  private void aggregateResultsFromBuild(Run build, FlowTestResults testResults, BuildListener listener) throws ExecutionException, InterruptedException {
    if (build == null) return;
    if (build instanceof FlowRun) {
      aggregateResultsFromFlowJob((FlowRun) build, testResults, listener);
    } else if (build instanceof MultiJobBuild) {
      aggregateResultsFromMultiJob((MultiJobBuild) build, testResults, listener);
    } else if (build instanceof MatrixBuild) {
      aggregateResultsFromMatrixJob((MatrixBuild) build, testResults, listener);
    } else if (build instanceof MavenModuleSetBuild) {
      aggregateResultsFromMavenMultiModuleJob(build, testResults, listener);
    } else {
      addTestResultFromBuild(build, testResults, listener);
    }
  }

  private void aggregateResultsFromFlowJob(FlowRun build, FlowTestResults testResults, BuildListener listener) throws ExecutionException, InterruptedException {
    listener.getLogger().println("Going to gather results from flow " + build);
    for (JobInvocation jobInv : build.getJobsGraph().vertexSet()) {
      if (!jobInv.getClass().getName().contains("Start")) {
        aggregateResultsFromBuild(jobInv.getBuild(), testResults, listener);
      }
    }
  }

  private void aggregateResultsFromMatrixJob(MatrixBuild run, FlowTestResults testResults, BuildListener listener) {
    listener.getLogger().println("Going to gather results from matrix job " + run);
    for (MatrixRun matrixRun : run.getRuns()) {
      addTestResultFromBuild(matrixRun, testResults, listener);
    }
  }

  private void aggregateResultsFromMultiJob(MultiJobBuild multijob, FlowTestResults testResults, BuildListener listener) throws ExecutionException, InterruptedException {
    listener.getLogger().println("Going to gather results from MultiJob " + multijob);
    // Results can be present at the MultiJob level as well
    // (anyway it should not be duplicated = do not store the same results on the MultiJob parent and downstream builds)
    addTestResultFromBuild(multijob, testResults, listener);
    for (MultiJobBuild.SubBuild subBuild : multijob.getSubBuilds()) {
      AbstractProject project = (AbstractProject) Jenkins.getInstance().getItem(subBuild.getJobName());
      aggregateResultsFromBuild(project.getBuildByNumber(subBuild.getBuildNumber()), testResults, listener);
    }
  }

  private void aggregateResultsFromMavenMultiModuleJob(Run<?, ?> build, FlowTestResults testResults, BuildListener listener) {
    listener.getLogger().println("Going to gather results from Maven multi module job " + build);
    SurefireAggregatedReport aggregatedTestReport = build.getAction(hudson.maven.reporters.SurefireAggregatedReport.class);
    if (aggregatedTestReport != null) {
      listener.getLogger().println("Adding test result for job " + build);
      for (Child child : aggregatedTestReport.children) {
        TestResultAction testResult = aggregatedTestReport.getChildReport(child);
        testResults.add(testResult);
      }
    }
  }

  private void addTestResultFromBuild(Run build, FlowTestResults testResults, BuildListener listener) {
    TestResultAction testResult = build.getAction(hudson.tasks.junit.TestResultAction.class);
    if (testResult != null) {
      listener.getLogger().println("Adding test result for job " + build);
      testResults.add(testResult);
    }
  }

  @Extension
  public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

  @Override
  public DescriptorImpl getDescriptor() {
    return DESCRIPTOR;
  }

  public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

    /**
     * This human readable name is used in the configuration screen.
     */
    public String getDisplayName() {
      return "Aggregate build flow test results";
    }

    @Override
    public boolean isApplicable(Class jobType) {
      return BuildFlow.class.equals(jobType);
    }
  }
}