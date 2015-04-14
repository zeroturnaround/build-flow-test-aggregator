package org.zeroturnaround.jenkins.flowbuildtestaggregator;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import com.tikal.jenkins.plugins.multijob.MultiJobBuild;
import hudson.model.AbstractProject;
import hudson.model.Run;
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

public class FlowTestAggregator extends Recorder {

  @DataBoundConstructor
  public FlowTestAggregator() {}

  public BuildStepMonitor getRequiredMonitorService() {
    return BuildStepMonitor.NONE;
  }

  @Override
  public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
    FlowTestResults testResults = new FlowTestResults(build);
    FlowRun flowRun = (FlowRun) build;
    flowRun.addAction(testResults);
    listener.getLogger().println("Starting to gather test results!");
    try {
      aggregateResultsFromJobInvocation(flowRun.getStartJob(), testResults, listener);
    } catch (ExecutionException e) {
      listener.getLogger().println("ERROR: " + e.getMessage());
    }
    listener.getLogger().println("Gathered results from " + testResults.getChildReports().size() + " jobs" );
    return true;
  }

  private void aggregateResultsFromJobInvocation(JobInvocation jobInvocation, FlowTestResults testResults, BuildListener listener) throws ExecutionException, InterruptedException {
    Run run = jobInvocation.getBuild();
    if (run instanceof FlowRun) {
      listener.getLogger().println("Going to gather results in flow " + run);
      for (JobInvocation jobInv : ((FlowRun) run).getJobsGraph().vertexSet()) {
        if (!jobInv.getClass().getName().contains("Start")) {
          aggregateResultsFromJobInvocation(jobInv, testResults, listener);
        }
      }
    } else if (run instanceof MultiJobBuild) {
      listener.getLogger().println("Going to gather results from multijob " + run);
      // Results can be present at the mutlijob level as well
      // (anyway it should not be duplicated = do not store the same results on the multijob parent and downstream builds)
      addTestResultFromBuild(run, testResults, listener);
      // only results from first level SubBuilds are collected
      for (MultiJobBuild.SubBuild subBuild : ((MultiJobBuild) run).getSubBuilds()) {
        AbstractProject project = (AbstractProject) Jenkins.getInstance().getItem(subBuild.getJobName());
        Run build = project.getBuildByNumber(subBuild.getBuildNumber());
        addTestResultFromBuild(build, testResults, listener);
      }
    } else {
      addTestResultFromBuild(run, testResults, listener);
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
