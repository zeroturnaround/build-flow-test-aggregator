package org.zeroturnaround.jenkins.flowbuildtestaggregator;

import com.cloudbees.plugins.flow.BuildFlow;
import com.cloudbees.plugins.flow.FlowRun;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.RunMap;
import hudson.model.listeners.ItemListener;
import hudson.tasks.test.AggregatedTestResultAction;
import hudson.tasks.test.AggregatedTestResultAction.Child;
import hudson.util.Function1;
import jenkins.model.Jenkins;

import java.util.ArrayList;
import java.util.List;

@Extension
public class ProjectRenameOrDeleteListener extends ItemListener {

  @Override
  public void onRenamed(Item item, final String oldName, final String newName) {
    processEvent(new Function1<Child, Child>() {
      public Child call(Child child) {
        if (child.name.split("/")[0].equals(oldName)) {
          String convertedName = child.name.replaceFirst(oldName, newName);
          return new Child(convertedName, child.build);
        }
        return child;
      }
    });
  }

  @Override
  public void onDeleted(final Item item) {
    processEvent(new Function1<Child, Child>() {
      public Child call(Child child) {
        if (child.name.split("/")[0].equals(item.getName())) return null;
        return child;
      }
    });
  }

  private void processEvent(Function1<Child, Child> childProcessor) {
    List<BuildFlow> buildFlowProjects = Jenkins.getInstance().getAllItems(BuildFlow.class);
    for (BuildFlow buildFlow : buildFlowProjects) {
      processEventForBuildFlow(buildFlow, childProcessor);
    }
  }

  private void processEventForBuildFlow(AbstractProject<BuildFlow, FlowRun> buildFlow, Function1<Child, Child> childProcessor) {
    RunMap<FlowRun> builds = buildFlow._getRuns();
    for (FlowRun build : builds) {
      AggregatedTestResultAction testResult = build.getAction(FlowTestResults.class);
      if (testResult != null) {
        processChildReports(childProcessor, testResult);
      }
    }
  }

  private void processChildReports(Function1<Child, Child> childProcessor, AggregatedTestResultAction testResult) {
    List<Child> processedChildren = new ArrayList<Child>();
    for (Child child : testResult.children) {
      Child processedChild = childProcessor.call(child);
      if (processedChild != null) processedChildren.add(processedChild);
    }
    testResult.children.clear();
    testResult.children.addAll(processedChildren);
  }
}