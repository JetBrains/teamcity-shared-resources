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

import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.buildDistribution.WaitReason;
import jetbrains.buildServer.serverSide.impl.ProjectEx;
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

  @Test
  public void testSimpleChain_NoOtherBuilds() {
    // create composite build
    BuildTypeEx btComposite = createCompositeBuildType(myProject, "composite", null);
    // create dep build
    SBuildType btDep = myProject.createBuildType("btDep", "btDep");
    // add dependency
    addDependency(btComposite, btDep);
    // add resource
    myProjectFeatures.addFeature(myProject, createInfiniteResource("resource"));
    // add lock on resource to composite build
    btComposite.addBuildFeature(SharedResourcesBuildFeature.FEATURE_TYPE, createReadLock("resource"));
    // start composite build
    QueuedBuildEx qbComposite = (QueuedBuildEx)btComposite.addToQueue("");
    assertNotNull(qbComposite);

    final SQueuedBuild first = myFixture.getBuildQueue().getFirst();
    Assert.assertNotNull(first);
    BuildPromotion qbDep = first.getBuildPromotion();

    myFixture.flushQueueAndWaitN(2);

    assertEquals(2, myFixture.getBuildsManager().getRunningBuilds().size());
    // read logs.. for now
    final SBuild depBuild = qbDep.getAssociatedBuild();
    assertTrue(depBuild instanceof RunningBuildEx);
    finishBuild((SRunningBuild)depBuild, false);

    waitForAllBuildsToFinish();

    assertEquals(0, myFixture.getBuildsManager().getRunningBuilds().size());
    assertTrue(myFixture.getBuildQueue().isQueueEmpty());
    final SBuild associatedBuild = qbComposite.getBuildPromotion().getAssociatedBuild();
    Assert.assertNotNull(associatedBuild);
    assertTrue(associatedBuild.isFinished());
  }

  private void waitForAllBuildsToFinish() {
    new WaitForAssert() {
      @Override
      protected boolean condition() {
        return myFixture.getBuildsManager().getRunningBuilds().size() == 0;
      }
    };
  }


  @Test
  public void testSimpleChain_NonIntersecting() {
    // create one simple chain
    BuildTypeEx btCompositeFirst = createCompositeBuildType(myProject, "composite1", null);
    SBuildType btDepFirst = myProject.createBuildType("btDep1", "btDep1");
    addDependency(btCompositeFirst, btDepFirst);
    // create resource
    myProjectFeatures.addFeature(myProject, createInfiniteResource("resource"));
    // create lock in composite build
    btCompositeFirst.addBuildFeature(SharedResourcesBuildFeature.FEATURE_TYPE, createWriteLock("resource"));
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

    new WaitForAssert() {
      @Override
      protected boolean condition() {
        final BuildEstimates buildEstimates = depSecond.getBuildEstimates();
        if (buildEstimates != null) {
          final WaitReason waitReason = buildEstimates.getWaitReason();
          if (waitReason != null && waitReason.getDescription().equals(expectedWaitReason)) {
            return true;
          }
        }
        myServer.flushQueue();
        return false;
      }
    };

    // finish current builds
    finishBuild((SRunningBuild)depBuildFirst, false);

    new WaitFor() {
      @Override
      protected boolean condition() {
        final SBuild associatedBuild = firstCompositePromo.getAssociatedBuild();
        return associatedBuild != null && associatedBuild instanceof SFinishedBuild;
      }
    };

    myFixture.flushQueueAndWaitN(2);

    final SBuild depBuildSecond = qbDepSecond.getAssociatedBuild();
    assertTrue(depBuildSecond instanceof RunningBuildEx);
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
