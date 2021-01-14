/*
 * Copyright 2000-2021 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.sharedResources.server.runtime;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import jetbrains.buildServer.agent.Constants;
import jetbrains.buildServer.agentServer.AgentBuild;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.buildDistribution.AgentsFilterResult;
import jetbrains.buildServer.serverSide.buildDistribution.SimpleWaitReason;
import jetbrains.buildServer.serverSide.buildDistribution.StartingBuildAgentsFilter;
import jetbrains.buildServer.serverSide.impl.MockBuildAgent;
import jetbrains.buildServer.serverSide.impl.MockVcsSupport;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.tests.SharedResourcesIntegrationTest;
import jetbrains.buildServer.util.TestFor;
import jetbrains.buildServer.vcs.RepositoryStateImpl;
import jetbrains.buildServer.vcs.VcsRootInstanceEx;
import jetbrains.buildServer.vcs.impl.RepositoryStateManager;
import org.testng.Assert;
import org.testng.annotations.Test;

import static jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants.getReservedResourceAttributeKey;
import static jetbrains.buildServer.sharedResources.tests.SharedResourcesIntegrationTestsSupport.*;
import static org.testng.Assert.assertNotEquals;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class BaseIntegrationTest extends SharedResourcesIntegrationTest {

  @Test
  public void simpleIntegrationTest() {
    addResource(myFixture, myProject, createInfiniteResource("resource"));
    final BuildTypeEx bt = myProject.createBuildType("check");
    addReadLock(bt, "resource");

    QueuedBuildEx qb = (QueuedBuildEx)bt.addToQueue("");
    assertNotNull(qb);

    myFixture.flushQueueAndWait();
    List<SRunningBuild> builds = myFixture.getBuildsManager().getRunningBuilds();
    assertEquals(1, builds.size());

    final BuildPromotionEx buildPromotion = qb.getBuildPromotion();
    assertNotNull(buildPromotion);

    final SBuild build = buildPromotion.getAssociatedBuild();
    assertTrue(build instanceof RunningBuildEx);

    finishBuild((SRunningBuild)build, false);

    waitForAllBuildsToFinish();

    final SBuild associatedBuild = qb.getBuildPromotion().getAssociatedBuild();
    Assert.assertNotNull(associatedBuild);
    assertTrue(associatedBuild.isFinished());
    assertEquals(1, getSharedResourceParameters(associatedBuild).size());
  }

  /**
   * 2 agents
   *
   * ProjectA (resource) -> btA(resource, write lock)
   * ProjectB (resource) -> btB(resource, write lock)
   *
   * btA and btB should run simultaneously
   *
   */
  @Test
  @TestFor(issues = "TW-57326")
  public void testSameResourceDifferentTrees() {
    final SProject projectA = myFixture.createProject("ProjectA");
    final Resource resourceA = addResource(myFixture, projectA, createInfiniteResource("resource"));
    final SBuildType btA = projectA.createBuildType("btA");
    addWriteLock(btA, resourceA);

    final SProject projectB = myFixture.createProject("ProjectB");
    final Resource resourceB = addResource(myFixture, projectB, createInfiniteResource("resource"));
    final SBuildType btB = projectB.createBuildType("btB");
    addWriteLock(btB, resourceB);

    myFixture.createEnabledAgent("Ant");

    btA.addToQueue("");
    btB.addToQueue("");

    myFixture.flushQueueAndWaitN(2);
  }

  /**
   *
   * Root
   * |____ProjectA
   *        |____SubProjectA ('CodeVersion' resource defined)
   *                 |____BuildTypeA1 (read lock on CodeVersion resource)
   *                 |____BuildTypeA2 (write lock on CodeVersion resource)
   *
   * |____ProjectB
   *        |____SubProjectB  ('CodeVersion' resource defined)
   *                 |____BuildTypeB (read lock on CodeVersion resource)
   *
   *  - Run BuildTypeA1
   *  - Add BuildTypeA2 to queue, Check wait reason
   *  - Add BuildTypeB to queue. Check started. Should not affect fair distribution set (which should contain ID of 'CodeVersion' resource defined in SubProjectA)
   */
  @Test
  @TestFor(issues = "TW-58285")
  public void testSameNameDifferentProjects() {
    myFixture.createEnabledAgent("Ant");
    myFixture.createEnabledAgent("Ant");


    final SProject projectA = myFixture.createProject("ProjectA");
    final SProject subProjectA = projectA.createProject("SubProjectA", "SubProjectA");
    final Resource codeVersion_A = addResource(myFixture, subProjectA, createInfiniteResource("CodeVersion"));

    final SBuildType buildTypeA1 = subProjectA.createBuildType("btA1");
    addReadLock(buildTypeA1, codeVersion_A);

    final SBuildType buildTypeA2 = subProjectA.createBuildType("btA2");
    addWriteLock(buildTypeA2, codeVersion_A);

    final SProject projectB = myFixture.createProject("ProjectB");
    final SProject subProjectB = projectB.createProject("SubProjectB", "SubProjectB");
    final Resource codeVersion_B = addResource(myFixture, subProjectB, createInfiniteResource("CodeVersion"));

    final SBuildType buildTypeB = subProjectB.createBuildType("btB");
    addReadLock(buildTypeB, codeVersion_B);

    buildTypeA1.addToQueue("");
    myFixture.flushQueueAndWait();
    assertEquals(1, myFixture.getBuildsManager().getRunningBuilds().size());

    final QueuedBuildEx qbV = (QueuedBuildEx)buildTypeA2.addToQueue("");
    assertNotNull(qbV);
    waitForReason(qbV, "Build is waiting for the following resource to become available: CodeVersion (locked by ProjectA / SubProjectA / btA1)");


    buildTypeB.addToQueue("");

    flushQueueAndWait();
    assertEquals(2, myFixture.getBuildsManager().getRunningBuilds().size());
  }


  /**
   * 2 agents
   *
   * Top (resource_top, ['val1', 'val2'])
   *
   * Child1 -> bt1 (resource_top, specific(resource_top, 'val1'))
   * Child2 -> bt2 (resource_top, any(resource_top))
   *
   */
  @Test
  @TestFor(issues = "TW-57515")
  public void testCustomResource_SpecificAny() {
    final SProject top = myFixture.createProject("top");
    final Resource resourceTop = addResource(myFixture, top, createCustomResource("resource_top", "val2", "val1"));
    final LocksStorage storage = myFixture.getSingletonService(LocksStorage.class);

    SProject child1 = top.createProject("child1", "child1");
    SBuildType btSpecific = child1.createBuildType("btSpecific", "btSpecific");
    final String specificRequestedValue = "val1";
    addSpecificLock(btSpecific, "resource_top", specificRequestedValue);

    SProject child2 = top.createProject("child2", "child2");
    SBuildType btAny = child2.createBuildType("btAny", "btAny");
    addReadLock(btAny, resourceTop);

    myFixture.createEnabledAgent("Ant");

    final SQueuedBuild qbAny = btAny.addToQueue("");
    final SQueuedBuild qbSpecific = btSpecific.addToQueue("");
    assertNotNull(qbSpecific);
    assertNotNull(qbAny);

    final RunningBuildEx runningBuild = myFixture.flushQueueAndWait();
    // we can start either one build (any with "specific" value)
    // ot two builds (any and specific with different values)

    // 2 builds can start in any order
    RunningBuildEx specificRunningBuild;

    final BuildPromotionEx buildPromotion = runningBuild.getBuildPromotion();
    final BuildTypeEx bt = buildPromotion.getBuildType();
    assertNotNull(bt);
    if (bt.getExternalId().equals(btAny.getExternalId())) {
      // if we started ANY build, we should check what lock was taken
      final Map<String, Lock> payload = storage.load(buildPromotion);
      String value = payload.get(resourceTop.getName()).getValue();
      // if it is the value that is requested by SPECIFIC one, -> wait for status in specific build, finish any build, start specific, check lock
      if (specificRequestedValue.equals(value)) {
        waitForReason(qbSpecific, "Build is waiting for the following resource to become available: resource_top (locked by top / child2 / btAny)");
        finishBuild(runningBuild, false);
        waitForAllBuildsToFinish();
        specificRunningBuild = myFixture.flushQueueAndWait();
        assert (qbSpecific.getBuildPromotion().getAssociatedBuild() instanceof RunningBuildEx);
        finishBuild(specificRunningBuild, false);
        assertContains(readArtifact(Objects.requireNonNull(qbAny.getBuildPromotion().getAssociatedBuild())), "resource_top\treadLock\tval1");
      } else {
        assertEquals(2, myFixture.getSingletonService(RunningBuildsManager.class).getRunningBuilds().size());
        assert (qbSpecific.getBuildPromotion().getAssociatedBuild() instanceof RunningBuildEx);
        specificRunningBuild = (RunningBuildEx)qbSpecific.getBuildPromotion().getAssociatedBuild();
        finishBuild(runningBuild, false);
        finishBuild(specificRunningBuild, false);
        assertContains(readArtifact(Objects.requireNonNull(qbAny.getBuildPromotion().getAssociatedBuild())), "resource_top\treadLock\tval2");
      }
    }
    assertNotNull(buildPromotion.getAttribute("teamcity.sharedResources." + resourceTop.getId()));
    finishAllBuilds();
    waitForAllBuildsToFinish();
    assertContains(readArtifact(Objects.requireNonNull(qbSpecific.getBuildPromotion().getAssociatedBuild())), "resource_top\treadLock\tval1");
  }

  @Test
  public void testCustomResource_SpecificSingle() {
    final Resource resourceTop = addResource(myFixture, myProject, createCustomResource("resource_top", "value"));
    SBuildType btSpecific = myProject.createBuildType("btSpecific", "btSpecific");

    addWriteLock(btSpecific, "resource_top");

    final SQueuedBuild qbSpecific = btSpecific.addToQueue("");
    assertNotNull(qbSpecific);
    myFixture.flushQueueAndWait();

    finishAllBuilds();
    waitForAllBuildsToFinish();
    final String reservedValue = (String)((BuildPromotionEx)qbSpecific.getBuildPromotion()).getAttribute("teamcity.sharedResources." + resourceTop.getId());
    assertNull("Should not reserve value for write lock", reservedValue);
    assertContains(readArtifact(Objects.requireNonNull(qbSpecific.getBuildPromotion().getAssociatedBuild())), "resource_top\twriteLock\t ");
  }

  @Test
  public void testCustomResource_AnyAny() {
    final SProject top = myFixture.createProject("top");
    final Resource resourceTop = addResource(myFixture, top, createCustomResource("resource_top", "val2", "val1"));

    SProject child1 = top.createProject("child1", "child1");
    SBuildType btAny1 = child1.createBuildType("btAny1", "btAny1");
    addReadLock(btAny1, resourceTop);

    SProject child2 = top.createProject("child2", "child2");
    SBuildType btAny2 = child2.createBuildType("btAny2", "btAny2");
    addReadLock(btAny2, resourceTop);

    myFixture.createEnabledAgent("Ant");

    final SQueuedBuild qbAny1 = btAny1.addToQueue("");
    final SQueuedBuild qbAny2 = btAny2.addToQueue("");

    assertNotNull(qbAny1);
    assertNotNull(qbAny2);

    myFixture.flushQueueAndWaitN(2);

    finishAllBuilds();

    final String val1 = (String)((BuildPromotionEx)qbAny1.getBuildPromotion()).getAttribute("teamcity.sharedResources." + resourceTop.getId());
    assertNotNull(val1);
    final String val2 = (String)((BuildPromotionEx)qbAny2.getBuildPromotion()).getAttribute("teamcity.sharedResources." + resourceTop.getId());
    assertNotNull(val2);
    assertNotEquals(val1, val2, "Provided values for ANY locks are equal! " + val1 + ":" + val2);
  }

  /**
   * Custom shared resource with single value
   *
   * Two builds, both require custom shared resource value
   * one is stopped by custom agent filter, the other is not
   *
   * Build that is not stopped should get value correctly
   */
  @Test
  public void testActualizeResourceAffinity() {
    final String expectedReason = "You shall not pass!";
    final StartingBuildAgentsFilter customFilter = agentsFilterContext -> {
      AgentsFilterResult result = new AgentsFilterResult();
      final BuildPromotionEx bp = (BuildPromotionEx)agentsFilterContext.getStartingBuild().getBuildPromotionInfo();
      if ("stopMe".equals(bp.getBuildTypeExternalId())) {
        result.setWaitReason(new SimpleWaitReason(expectedReason));
      }
      return result;
    };
    myFixture.addService(customFilter);

    final SProject project = myFixture.createProject("project");
    final Resource resource = addResource(myFixture, project, createCustomResource("resource", "value"));
    final SBuildType btStopMe = project.createBuildType("stopMe", "stopMe");
    final SBuildType btOk = project.createBuildType("btOk", "btOk");

    addReadLock(btStopMe, resource);
    addReadLock(btOk, resource);

    final SQueuedBuild stopMeBuild = btStopMe.addToQueue("");
    assertNotNull(stopMeBuild);
    waitForReason(stopMeBuild, expectedReason);
    final SQueuedBuild okQueuedBuild = btOk.addToQueue("");
    assertNotNull(okQueuedBuild);
    final RunningBuildEx rb = myFixture.flushQueueAndWait();
    assertEquals(btOk, rb.getBuildType());
    final String val = (String)rb.getBuildPromotion().getAttribute("teamcity.sharedResources." + resource.getId());
    assertNotNull(val);
    assertEquals("Incorrect value of " + "teamcity.sharedResources." + resource.getId() + " parameter", "value", val);
  }

  @Test
  @TestFor(issues = "TW-57515")
  public void testWithSingleTemplate() {
    final SProject top = myFixture.createProject("top");
    final Resource resource = addResource(myFixture, top, createCustomResource("resource", "val2", "val1"));
    final BuildTypeTemplate template = top.createBuildTypeTemplate("my_template", "My Template");
    addReadLock(template, resource);
    final String paramName = Constants.ENV_PREFIX + "TEST_VALUE";
    template.addBuildParameter(new SimpleParameter(paramName, "%teamcity.locks.readLock." + resource.getName() + "%"));

    final SBuildType bt1 = top.createBuildType("bt1");
    bt1.addTemplate(template, false);
    final SBuildType bt2 = top.createBuildType("bt2");
    bt2.addTemplate(template, false);

    myFixture.createEnabledAgent("Ant");

    final SQueuedBuild qb1 = bt1.addToQueue("");
    final SQueuedBuild qb2 = bt2.addToQueue("");

    assertNotNull(qb1);
    assertNotNull(qb2);

    myFixture.flushQueueAndWaitN(2);
    finishAllBuilds();

    final String val1 = (String)((BuildPromotionEx)qb1.getBuildPromotion()).getAttribute(getReservedResourceAttributeKey(resource.getId()));
    assertNotNull(val1);
    final String val2 = (String)((BuildPromotionEx)qb2.getBuildPromotion()).getAttribute(getReservedResourceAttributeKey(resource.getId()));
    assertNotNull(val2);
    assertNotEquals(val1, val2, "Provided values for ANY locks are equal! " + val1 + ":" + val2);
  }

  @Test
  @TestFor(issues = "TW-57515")
  public void testWithBuildBranches() {
    // register vcs support
    final MockVcsSupport vcsSupport = myFixture.registerVcsSupport("git");
    // create project
    final SProject top = myFixture.createProject("top");
    // create template
    BuildTypeTemplateEx template = (BuildTypeTemplateEx)top.createBuildTypeTemplate("tpl", "tpl");
    // create vcs root
    // attach vcs root to template
    template.addVcsRoot(top.createVcsRoot(vcsSupport.getName(), null, Collections.emptyMap()));
    // setup branches
    final VcsRootInstanceEx instance = (VcsRootInstanceEx)template.getVcsRootInstanceEntries().get(0).getVcsRoot();
    setBranchSpec(instance, "bugfix/(*)");
    RepositoryStateImpl state = new RepositoryStateImpl("myBranch");
    state.setBranchRevision("bugfix/branch1", "rev1");
    state.setBranchRevision("bugfix/branch2", "rev2");
    myFixture.getSingletonService(RepositoryStateManager.class).setRepositoryState(instance, state);
    // create custom shared resource
    final Resource resource = addResource(myFixture, top, createCustomResource("resource", "val2", "val1"));
    // create lock
    addReadLock(template, resource);
    // create build type from template
    final BuildTypeEx buildType = (BuildTypeEx)top.createBuildType("bt");
    buildType.addTemplate(template, false);

    final MockBuildAgent agent2 = myFixture.createEnabledAgent("Ant");

    // add build on branch1 to queue
    // add build on branch2 to queue
    BuildPromotionEx branch1Promo = buildType.createBuildPromotion("branch1");
    branch1Promo.addToQueue("");
    BuildPromotionEx branch2Promo = buildType.createBuildPromotion("branch2");
    branch2Promo.addToQueue("");
    myFixture.flushQueueAndWaitN(2);
    finishAllBuilds();

    final AgentBuild agentBuild1 = myBuildAgent.getAgentBuild();
    String value1 = agentBuild1.getServerParameters().get("teamcity.locks.readLock." + resource.getName());
    final AgentBuild agentBuild2 = agent2.getAgentBuild();
    String value2 = agentBuild2.getServerParameters().get("teamcity.locks.readLock." + resource.getName());
    assertNotEquals(value1, value2);
    finishAllBuilds();

    final String val1 = (String)branch1Promo.getAttribute(getReservedResourceAttributeKey(resource.getId()));
    assertNotNull(val1);
    final String val2 = (String)branch2Promo.getAttribute(getReservedResourceAttributeKey(resource.getId()));
    assertNotNull(val2);
    assertNotEquals(val1, val2, "Provided values for ANY locks are equal! " + val1 + ":" + val2);
  }

  /**
   * Project features, created from Kotlin DSL can have same IDs on different branches of project hierarchy
   * This test creates resources with same ids of underlying project feature and checks,
   * whether builds in different subtrees are not affected by same ids of the resources
   */
  @Test
  public void testSameIdDifferentLevels() {
    final SProject top = myFixture.createProject("top");
    final SProject child1 = top.createProject("child1", "Child 1");
    final SProject child2 = top.createProject("child2", "Child 2");
    final Resource rc1 = addResource(myFixture, child1, "PROJECT_EXT_1", createQuotedResource("rc_child1", 1));
    final Resource rc2 = addResource(myFixture, child2, "PROJECT_EXT_1", createQuotedResource("rc_child2", 1));

    final SBuildType bt1 = child1.createBuildType("bt1");
    addReadLock(bt1, rc1);
    final SBuildType bt2 = child2.createBuildType("bt2");
    addReadLock(bt2, rc2);

    myFixture.createEnabledAgent("Ant");

    bt1.addToQueue("");
    bt2.addToQueue("");
    myFixture.flushQueueAndWaitN(2);
  }
}
