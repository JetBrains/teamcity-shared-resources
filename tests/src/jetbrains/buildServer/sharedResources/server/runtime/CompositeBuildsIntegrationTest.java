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
import jetbrains.buildServer.serverSide.impl.ProjectEx;
import jetbrains.buildServer.sharedResources.server.SharedResourcesBuildFeature;
import jetbrains.buildServer.sharedResources.tests.SharedResourcesIntegrationTest;
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
    
    // wait for all builds to finish
    new WaitForAssert() {
      @Override
      protected boolean condition() {
        return myFixture.getBuildsManager().getRunningBuilds().size() == 0;
      }
    };

    assertEquals(0, myFixture.getBuildsManager().getRunningBuilds().size());
    assertTrue(myFixture.getBuildQueue().isQueueEmpty());
    final SBuild associatedBuild = qbComposite.getBuildPromotion().getAssociatedBuild();
    Assert.assertNotNull(associatedBuild);
    assertTrue(associatedBuild.isFinished());
  }

  @Test
  public void testSimpleChain_NonIntersecting() {

  }

  @Test
  public void testSimpleChain_Quota() {

  }

  @Test
  public void testSimpleChain_ReadWrite() {

  }

  @Test
  public void testSimpleChain_Custom() {
    
  }


  @SuppressWarnings("SameParameterValue")
  @NotNull
  static BuildTypeEx createCompositeBuildType(@NotNull final ProjectEx project,
                                              @NotNull final String extId,
                                              @Nullable final String name) {
    final BuildTypeEx btComposite = project.createBuildType(extId, name != null ? name : extId);
    btComposite.setOption(BuildTypeOptions.BT_BUILD_CONFIGURATION_TYPE, BuildTypeOptions.BuildConfigurationType.COMPOSITE.name());
    return btComposite;
  }


}
