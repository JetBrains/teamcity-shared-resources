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

package jetbrains.buildServer.sharedResources.server.report;

import java.io.File;
import java.util.List;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.tests.SharedResourcesIntegrationTest;
import jetbrains.buildServer.util.FileUtil;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static jetbrains.buildServer.sharedResources.tests.SharedResourcesIntegrationTestsSupport.*;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class BuildUsedResourcesReportTest extends SharedResourcesIntegrationTest {

  private BuildUsedResourcesReport myBuildUsedResourcesReport;

  @Override
  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    myBuildUsedResourcesReport = myFixture.getSingletonService(BuildUsedResourcesReport.class);
  }

  @Test
  public void testReportExists_Yes() {
    addResource(myFixture, myProject, createInfiniteResource("resource_infinite"));
    addResource(myFixture, myProject, createQuotedResource("resource_quoted", 10));
    addResource(myFixture, myProject, createCustomResource("resource_custom", "a", "b", "c", "d"));

    final BuildTypeEx bt = myProject.createBuildType("BuildType");

    addReadLock(bt, "resource_infinite");
    addWriteLock(bt, "resource_quoted");
    addSpecificLock(bt, "resource_custom", "c");

    QueuedBuildEx qb = (QueuedBuildEx)bt.addToQueue("");
    assertNotNull(qb);

    myFixture.flushQueueAndWait();
    List<SRunningBuild> builds = myFixture.getBuildsManager().getRunningBuilds();
    assertEquals(1, builds.size());

    final BuildPromotionEx buildPromotion = qb.getBuildPromotion();
    assertNotNull(buildPromotion);
    final SBuild build = buildPromotion.getAssociatedBuild();
    assertTrue(build instanceof RunningBuildEx);

    final SFinishedBuild finishedBuild = finishBuild((SRunningBuild)build, false);
    assertNotNull(finishedBuild);

    assertTrue(myBuildUsedResourcesReport.exists(finishedBuild));
    List<UsedResource> usedResources = myBuildUsedResourcesReport.load(finishedBuild);
    assertEquals(3, usedResources.size());
  }


  @Test
  public void testReportExists_No() {
    final BuildTypeEx bt = myProject.createBuildType("BuildType");
    QueuedBuildEx qb = (QueuedBuildEx)bt.addToQueue("");
    assertNotNull(qb);

    myFixture.flushQueueAndWait();
    List<SRunningBuild> builds = myFixture.getBuildsManager().getRunningBuilds();
    assertEquals(1, builds.size());

    final BuildPromotionEx buildPromotion = qb.getBuildPromotion();
    assertNotNull(buildPromotion);
    final SBuild build = buildPromotion.getAssociatedBuild();
    assertTrue(build instanceof RunningBuildEx);

    final SFinishedBuild finishedBuild = finishBuild((SRunningBuild)build, false);
    assertNotNull(finishedBuild);

    assertFalse(myBuildUsedResourcesReport.exists(finishedBuild));
  }

  @Test
  public void testCorruptReport() throws Exception {
    final BuildTypeEx bt = myProject.createBuildType("BuildType");
    QueuedBuildEx qb = (QueuedBuildEx)bt.addToQueue("");
    assertNotNull(qb);

    myFixture.flushQueueAndWait();
    List<SRunningBuild> builds = myFixture.getBuildsManager().getRunningBuilds();
    assertEquals(1, builds.size());

    final BuildPromotionEx buildPromotion = qb.getBuildPromotion();
    assertNotNull(buildPromotion);

    final File artifact = new File(buildPromotion.getArtifactsDirectory(), SharedResourcesPluginConstants.BASE_ARTIFACT_PATH + "/used_resources.json");
    if (FileUtil.createParentDirs(artifact)) {
      FileUtil.writeFile(artifact, "Non-Json contents", "UTF-8");
    }

    final SBuild build = buildPromotion.getAssociatedBuild();
    assertTrue(build instanceof RunningBuildEx);

    final SFinishedBuild finishedBuild = finishBuild((SRunningBuild)build, false);
    assertNotNull(finishedBuild);

    assertTrue(myBuildUsedResourcesReport.exists(finishedBuild));
    assertTrue(myBuildUsedResourcesReport.load(finishedBuild).isEmpty());
  }


}
