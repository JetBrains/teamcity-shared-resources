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

import java.util.List;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.sharedResources.tests.SharedResourcesIntegrationTest;
import jetbrains.buildServer.util.WaitForAssert;
import org.testng.Assert;
import org.testng.annotations.Test;

import static jetbrains.buildServer.sharedResources.tests.SharedResourcesIntegrationTestsSupport.*;

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

    new WaitForAssert() {
      @Override
      protected boolean condition() {
        return myFixture.getBuildsManager().getRunningBuilds().size() == 0;
      }
    };

    final SBuild associatedBuild = qb.getBuildPromotion().getAssociatedBuild();
    Assert.assertNotNull(associatedBuild);
    assertTrue(associatedBuild.isFinished());
    assertEquals(1, getSharedResourceParameters(associatedBuild).size());
  }
}
