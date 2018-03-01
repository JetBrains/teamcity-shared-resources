/*
 * Copyright 2000-2018 JetBrains s.r.o.
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

import java.util.Optional;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.artifacts.BuildArtifactHolder;
import jetbrains.buildServer.serverSide.artifacts.BuildArtifactsViewMode;
import jetbrains.buildServer.serverSide.buildDistribution.WaitReason;
import jetbrains.buildServer.serverSide.impl.ProjectEx;
import jetbrains.buildServer.serverSide.impl.timeEstimation.CachingBuildEstimator;
import jetbrains.buildServer.sharedResources.server.SharedResourcesBuildFeature;
import jetbrains.buildServer.sharedResources.tests.SharedResourcesIntegrationTest;
import jetbrains.buildServer.util.WaitFor;
import jetbrains.buildServer.util.WaitForAssert;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class CompositeBuildsIntegrationTest extends SharedResourcesIntegrationTest {


  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    enableRunningBuildsUpdate();
  }

  /**
   * Test setup:
   *
   * dep ---> (C)[resource, write_lock]
   *
   */
  @Test
  public void testSimpleChain() {
    // create composite build
    BuildTypeEx btComposite = createCompositeBuildType(myProject, "composite", null);
    // create dep build
    SBuildType btDep = myProject.createBuildType("btDep", "btDep");
    // add dependency
    addDependency(btComposite, btDep);
    // add resource
    addResource(myProject, createInfiniteResource("resource"));
    // add lock on resource to composite build
    addReadLock(btComposite, "resource");
    // start composite build
    QueuedBuildEx qbComposite = (QueuedBuildEx)btComposite.addToQueue("");
    assertNotNull(qbComposite);

    final SQueuedBuild first = myFixture.getBuildQueue().getFirst();
    Assert.assertNotNull(first);
    BuildPromotion qbDep = first.getBuildPromotion();

    myFixture.flushQueueAndWaitN(2);

    assertEquals(2, myFixture.getBuildsManager().getRunningBuilds().size());

    final SBuild depBuild = qbDep.getAssociatedBuild();
    assertTrue(depBuild instanceof RunningBuildEx);
    finishBuild((SRunningBuild)depBuild, false);

    waitForAllBuildsToFinish();

    assertEquals(0, myFixture.getBuildsManager().getRunningBuilds().size());
    assertTrue(myFixture.getBuildQueue().isQueueEmpty());
    final SBuild associatedBuild = qbComposite.getBuildPromotion().getAssociatedBuild();
    Assert.assertNotNull(associatedBuild);
    assertTrue(associatedBuild.isFinished());

    // check for parameters

    // check for stored locks
    final BuildArtifactHolder artifact = associatedBuild.getArtifacts(BuildArtifactsViewMode.VIEW_HIDDEN_ONLY).findArtifact(".teamcity/JetBrains.SharedResources/taken_locks.txt");
    assertTrue("Artifact with taken locks is not present in composite build", artifact.isAvailable());
  }

  /**
   * Test setup:
   *
   * 2 agents
   *
   * dep1 ---> (C1)['resource', write lock]
   *
   * dep2 ---> (C2)['resource', write lock] 
   *
   * Two simple chains of one composite and one dependent build
   * Composite builds each hold write lock on same resource
   *
   * First chain is run and blocks execution of the second chain
   *
   */
  @Test
  public void testSimpleChain_WriteLock_OtherChain() {
    // create one simple chain
    BuildTypeEx btCompositeFirst = createCompositeBuildType(myProject, "composite1", null);
    SBuildType btDepFirst = myProject.createBuildType("btDep1", "btDep1");
    addDependency(btCompositeFirst, btDepFirst);
    // create resource
    addResource(myProject, createInfiniteResource("resource"));
    // create lock in composite build
    addWriteLock(btCompositeFirst, "resource");
    // create second simple chain
    BuildTypeEx btCompositeSecond = createCompositeBuildType(myProject, "composite2", null);
    SBuildType btDepSecond = myProject.createBuildType("btDep2", "btDep2");
    addDependency(btCompositeSecond, btDepSecond);
    // create lock in second composite build
    btCompositeSecond.addBuildFeature(SharedResourcesBuildFeature.FEATURE_TYPE, createWriteLock("resource"));

    // register second agent
    myFixture.createEnabledAgent("Ant");
    // run first chain
    QueuedBuildEx qbCompositeFirst = (QueuedBuildEx)btCompositeFirst.addToQueue("");
    assertNotNull(qbCompositeFirst);
    final BuildPromotionEx firstCompositePromo = qbCompositeFirst.getBuildPromotion();

    final SQueuedBuild depFirst = myFixture.getBuildQueue().getFirst();
    Assert.assertNotNull(depFirst);
    BuildPromotion qbDepFirst = depFirst.getBuildPromotion();

    myFixture.flushQueueAndWaitN(2);
    // await chain is running
    assertEquals(2, myFixture.getBuildsManager().getRunningBuilds().size());
    // assert we have dep build running
    final SBuild depBuildFirst = qbDepFirst.getAssociatedBuild();
    assertTrue(depBuildFirst instanceof RunningBuildEx);
    // assert we have empty queue
    assertEquals(0, myFixture.getBuildQueue().getNumberOfItems());
    // run second chain
    final QueuedBuildEx qbCompositeSecond = (QueuedBuildEx)btCompositeSecond.addToQueue("");
    assertNotNull(qbCompositeSecond);

    final SQueuedBuild depSecond = myFixture.getBuildQueue().getFirst();
    Assert.assertNotNull(depSecond);
    BuildPromotion qbDepSecond = depSecond.getBuildPromotion();

    final String expectedWaitReason = "Build is waiting for the following resource to become available: resource (locked by My Default Test Project :: composite1)";
    waitForReason(depSecond, expectedWaitReason);

    // finish current builds
    finishBuild((SRunningBuild)depBuildFirst, false);

    new WaitFor() {
      @Override
      protected boolean condition() {
        final SBuild associatedBuild = firstCompositePromo.getAssociatedBuild();
        return associatedBuild instanceof SFinishedBuild;
      }
    };

    myFixture.flushQueueAndWaitN(2);

    final SBuild depBuildSecond = qbDepSecond.getAssociatedBuild();
    assertTrue(depBuildSecond instanceof RunningBuildEx);
  }

  /**
   * Test setup:
   *
   * 2 agents
   *
   * dep ---> (C2)[resource, write lock] ---> (C1)
   *
   * other_build[resource, write lock]
   *
   */
  @Test
  public void testCompositeInsideComposite() {
    // register second agent
    myFixture.createEnabledAgent("Ant");
    BuildTypeEx btComposite1 = createCompositeBuildType(myProject, "composite1", null);
    // create complex composite chain
    BuildTypeEx btComposite2 = createCompositeBuildType(myProject, "composite2", null);
    SBuildType btDep = myProject.createBuildType("btDep", "btDep");
    addDependency(btComposite2, btDep);
    addDependency(btComposite1, btComposite2);
    // create other build
    SBuildType btOther = myProject.createBuildType("btOther", "Other Build Type");
    // add resources and locks
    addResource(myProject, createInfiniteResource("resource"));
    addWriteLock(btComposite2, "resource");
    addWriteLock(btOther, "resource");
    // run other build
    QueuedBuildEx qbOther = (QueuedBuildEx)btOther.addToQueue("");
    assertNotNull(qbOther);
    BuildPromotion bpOther = qbOther.getBuildPromotion();
    // dep should not start until chain finished
    myFixture.flushQueueAndWait();
    // we are running exactly one build
    assertEquals(1, myFixture.getBuildsManager().getRunningBuilds().size());
    final SBuild otherBuild = bpOther.getAssociatedBuild();
    assertTrue(otherBuild instanceof RunningBuildEx);
    // put head of the chain in queue
    QueuedBuildEx qbComposite1 = (QueuedBuildEx)btComposite1.addToQueue("");
    assertNotNull(qbComposite1);
    // all chain is in the queue
    assertEquals(3, myFixture.getBuildQueue().getNumberOfItems());
    // get top build from queue. Should be dep
    final SQueuedBuild depBuild = myFixture.getBuildQueue().getFirst();
    assertNotNull(depBuild);
    final BuildPromotionEx depPromo = (BuildPromotionEx)depBuild.getBuildPromotion();
    String expectedWaitReason = "Build is waiting for the following resource to become available: resource (locked by My Default Test Project :: Other Build Type)";
    waitForReason(depBuild, expectedWaitReason);
    // finish build
    finishBuild((SRunningBuild)otherBuild, false);
    new WaitFor() {
      @Override
      protected boolean condition() {
        final SBuild associatedBuild = bpOther.getAssociatedBuild();
        return associatedBuild instanceof SFinishedBuild;
      }
    };
    // check that chain started
    myFixture.flushQueueAndWaitN(3);

    final SBuild depRunningBuild = depPromo.getAssociatedBuild();
    assertTrue(depRunningBuild instanceof RunningBuildEx);
  }

  /**
   * Test setup:
   *
   * dep [resource, write lock] --> (C) ['resource', write lock]
   *
   * Same lock in composite head and dep should not prevent dep from starting
   * Dep and composite should successfully acquire locks and have them in artifacts
   */
  @Test
  public void testSameLockInComposite() {
    // create composite build
    BuildTypeEx btComposite = createCompositeBuildType(myProject, "composite", null);
    // create dep build
    SBuildType btDep = myProject.createBuildType("btDep", "btDep");
    // add dependency
    addDependency(btComposite, btDep);
    // add resource
    addResource(myProject, createInfiniteResource("resource"));
    // add lock on resource to composite and dep builds
    addWriteLock(btComposite, "resource");
    addWriteLock(btDep, "resource");
    // start composite build
    QueuedBuildEx qbComposite = (QueuedBuildEx)btComposite.addToQueue("");
    assertNotNull(qbComposite);
    // await 2 running builds despite write locks inside one chain
    final SQueuedBuild first = myFixture.getBuildQueue().getFirst();
    Assert.assertNotNull(first);
    BuildPromotion qbDep = first.getBuildPromotion();
    myFixture.flushQueueAndWaitN(2);
    assertEquals(2, myFixture.getBuildsManager().getRunningBuilds().size());
    // dep build is running
    final SBuild depBuild = qbDep.getAssociatedBuild();
    assertTrue(depBuild instanceof RunningBuildEx);
  }

  /**
   * Test setup:
   *
   * 2 agents
   *
   * dep1  ---- --- --- --- --- --- --->
   *                                      ---> (C) ['resource', write lock]
   * dep2 ['resource', write lock]  --->
   *
   * dep1 should start before dep2
   * C, consequently, starts, acquires lock and becomes visible in RunningBuildsManager
   * should not prevent dep2 from starting on the second agent, as write lock, taken by the chain should not count inside it
   *
   */
  @Test
  public void testSameLockInRunningComposite() {
    // register second agent
    myFixture.createEnabledAgent("Ant");
    // create composite build
    BuildTypeEx btComposite = createCompositeBuildType(myProject, "composite", null);
    // create dep builds
    SBuildType btDep1 = myProject.createBuildType("btDep1", "btDep1");
    SBuildType btDep2 = myProject.createBuildType("btDep2", "btDep2");
    // add dependency
    addDependency(btComposite, btDep1);
    addDependency(btComposite, btDep2);
    // add resource
    addResource(myProject, createInfiniteResource("resource"));
    // add write locks to dep2 and composite head
    addWriteLock(btComposite, "resource");
    addWriteLock(btDep2, "resource");
    // put chain to queue
    // start composite build
    QueuedBuildEx qbComposite = (QueuedBuildEx)btComposite.addToQueue("");
    assertNotNull(qbComposite);
    // get queued build of
    Optional<SQueuedBuild> opQueued = myFixture.getBuildQueue().getItems().stream().filter(qb -> qb.getBuildType().equals(btDep1)).findFirst();
    if (!opQueued.isPresent()) {
      fail("Could not find needed dependency (dep1) in queue");
    }
    final SQueuedBuild dep1Queued = opQueued.get();
    myFixture.getBuildQueue().moveTop(dep1Queued.getItemId());
    // start and await 3 builds
    myFixture.flushQueueAndWaitN(3);
    assertEquals(3, myFixture.getBuildsManager().getRunningBuilds().size());
  }

  private void waitForAllBuildsToFinish() {
    new WaitForAssert() {
      @Override
      protected boolean condition() {
        return myFixture.getBuildsManager().getRunningBuilds().size() == 0;
      }
    };
  }

  private void waitForReason(@NotNull final SQueuedBuild queuedBuild, @NotNull final String expectedReason) {
    CachingBuildEstimator estimator = myFixture.getSingletonService(CachingBuildEstimator.class);

    new WaitForAssert() {
      @Override
      protected boolean condition() {
        estimator.invalidate(false);
        final BuildEstimates buildEstimates = queuedBuild.getBuildEstimates();
        if (buildEstimates != null) {
          final WaitReason waitReason = buildEstimates.getWaitReason();
          return waitReason != null && waitReason.getDescription().equals(expectedReason);
        }
        return false;
      }
    };
  }

  @SuppressWarnings("SameParameterValue")
  @NotNull
  private static BuildTypeEx createCompositeBuildType(@NotNull final ProjectEx project,
                                                      @NotNull final String extId,
                                                      @Nullable final String name) {
    final BuildTypeEx btComposite = project.createBuildType(extId, name != null ? name : extId);
    btComposite.setOption(BuildTypeOptions.BT_BUILD_CONFIGURATION_TYPE, BuildTypeOptions.BuildConfigurationType.COMPOSITE.name());
    return btComposite;
  }
}
