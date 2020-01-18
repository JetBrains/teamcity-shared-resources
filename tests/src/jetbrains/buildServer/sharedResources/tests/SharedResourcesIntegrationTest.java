/*
 * Copyright 2000-2020 JetBrains s.r.o.
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;
import jetbrains.buildServer.serverSide.BuildEstimates;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SQueuedBuild;
import jetbrains.buildServer.serverSide.artifacts.BuildArtifactHolder;
import jetbrains.buildServer.serverSide.artifacts.BuildArtifactsViewMode;
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

  @NotNull
  protected List<String> readArtifact(SBuild build) {
    final BuildArtifactHolder holder = build.getArtifacts(BuildArtifactsViewMode.VIEW_HIDDEN_ONLY)
                                            .findArtifact(".teamcity/JetBrains.SharedResources/taken_locks.txt");
    if (!holder.isAvailable()) {
      fail("Shared resources artifact is not available for the build: " + build);
    }
    try {
      return new BufferedReader(new InputStreamReader(holder.getArtifact().getInputStream())).lines().collect(Collectors.toList());
    } catch (IOException e) {
      e.printStackTrace();
      fail("Failed to read shared resources artifact contents from build [" + build + "], Exception: " + e.getMessage());
    }
    return null;
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

