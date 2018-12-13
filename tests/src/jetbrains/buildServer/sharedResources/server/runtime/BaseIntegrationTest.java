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
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.tests.SharedResourcesIntegrationTest;
import jetbrains.buildServer.util.TestFor;
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
}
