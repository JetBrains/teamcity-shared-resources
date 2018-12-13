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

package jetbrains.buildServer.sharedResources.tests;

import jetbrains.buildServer.serverSide.BuildEstimates;
import jetbrains.buildServer.serverSide.SQueuedBuild;
import jetbrains.buildServer.serverSide.buildDistribution.WaitReason;
import jetbrains.buildServer.serverSide.impl.BaseServerTestCase;
import jetbrains.buildServer.serverSide.impl.timeEstimation.CachingBuildEstimator;
import jetbrains.buildServer.util.WaitForAssert;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.BeforeMethod;

/**
 * Base class for execution of integration tests with shared resources in builds
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public abstract class SharedResourcesIntegrationTest extends BaseServerTestCase {

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    SharedResourcesIntegrationTestsSupport.apply(myFixture);
  }

  protected void waitForAllBuildsToFinish() {
    new WaitForAssert() {
      @Override
      protected boolean condition() {
        return myFixture.getBuildsManager().getRunningBuilds().size() == 0;
      }
    };
  }

  protected void waitForReason(@NotNull final SQueuedBuild queuedBuild, @NotNull final String expectedReason) {
    final CachingBuildEstimator estimator = myFixture.getSingletonService(CachingBuildEstimator.class);

    new WaitForAssert() {

      private String myReportedReason = "<default>";

      @Override
      protected boolean condition() {
        estimator.invalidate(false);
        final BuildEstimates buildEstimates = queuedBuild.getBuildEstimates();
        if (buildEstimates != null) {
          final WaitReason waitReason = buildEstimates.getWaitReason();
          if (waitReason != null) {
            myReportedReason = waitReason.getDescription();
          }
          System.out.println(myReportedReason);
          return myReportedReason != null && myReportedReason.equals(expectedReason);
        }
        return false;
      }

      @Override
      protected String getAssertMessage() {
        return "Expected wait reason [" + expectedReason + "], last reported: [" + myReportedReason + "]";
      }
    };
  }
}

