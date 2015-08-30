package org.zeroturnaround.jenkins.flowbuildtestaggregator;

import hudson.Extension;
import hudson.model.Item;
import hudson.model.RunMap;
import hudson.model.AbstractProject;
import hudson.model.listeners.ItemListener;
import hudson.tasks.test.AggregatedTestResultAction;
import hudson.tasks.test.AggregatedTestResultAction.Child;

import java.util.ArrayList;
import java.util.List;

import jenkins.model.Jenkins;

import com.cloudbees.plugins.flow.BuildFlow;
import com.cloudbees.plugins.flow.FlowRun;

@Extension
public class ProjectRenameListener extends ItemListener {

  @Override
  public void onRenamed(Item item, String oldName, String newName) {
    List<BuildFlow> buildFlowProjects = Jenkins.getInstance().getAllItems(BuildFlow.class);
    for (BuildFlow buildFlow : buildFlowProjects) {
      doRenameForFlowProjects(buildFlow, oldName, newName);
    }
  }

  private void doRenameForFlowProjects(AbstractProject<BuildFlow, FlowRun> buildFlow, String oldName, String newName) {
    RunMap<FlowRun> builds = buildFlow._getRuns();

    for (FlowRun build : builds) {
      AggregatedTestResultAction testResult = build.getAction(FlowTestResults.class);
      if (testResult != null) {
        List<Child> renamedChildren = getRenamedChildBuilds(testResult, oldName, newName);
        testResult.children.clear();
        testResult.children.addAll(renamedChildren);
      }
    }
  }

  private List<Child> getRenamedChildBuilds(AggregatedTestResultAction testResult, String oldName, String newName) {
    List<Child> renamedChildren = new ArrayList<Child>();
    for (Child child : testResult.children) {
      Child renamedChild = getRenamedChild(child, oldName, newName);
      renamedChildren.add(renamedChild);
    }
    return renamedChildren;
  }

  private Child getRenamedChild(Child child, String oldName, String newName) {
    Child renamedChild = child;
    if (child.name.split("/")[0].equals(oldName)) {
      String convertedName = child.name.replaceFirst(oldName, newName);
      renamedChild = new Child(convertedName, child.build);
    }
    return renamedChild;
  }
}