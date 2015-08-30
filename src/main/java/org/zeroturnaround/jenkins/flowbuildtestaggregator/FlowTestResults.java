package org.zeroturnaround.jenkins.flowbuildtestaggregator;

import jenkins.model.Jenkins;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.tasks.test.AggregatedTestResultAction;

public class FlowTestResults extends AggregatedTestResultAction {

  public FlowTestResults(AbstractBuild owner) {
    super(owner);
  }

  @Override
  protected String getChildName(AbstractTestResultAction tr) {
    return tr.owner.getProject().getFullName();
  }

  @Override
  public AbstractBuild<?, ?> resolveChild(Child child) {
    AbstractProject<?, ?> project = Jenkins.getInstance().getItemByFullName(child.name, AbstractProject.class);

    if (project != null) {
      return project.getBuildByNumber(child.build);
    } else {
      return null;
    }
  }

  @Override
  protected void add(AbstractTestResultAction child) {
    super.add(child);
  }
}
