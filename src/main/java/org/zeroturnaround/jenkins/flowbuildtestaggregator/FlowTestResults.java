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
    return tr.owner.getProject().getName();
  }

  @Override
  public AbstractBuild<?, ?> resolveChild(Child child) {
    return Jenkins.getInstance().getItemByFullName(child.name, AbstractProject.class).getBuildByNumber(child.build);
  }

  @Override
  protected void add(AbstractTestResultAction child) {
    super.add(child);
  }
}
