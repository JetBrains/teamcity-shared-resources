/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package jetbrains.buildServer.sharedResources.server;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.serverSide.BuildPromotionEx;
import jetbrains.buildServer.serverSide.buildDistribution.BuildPromotionInfo;
import jetbrains.buildServer.serverSide.settings.ProjectSettingsManager;
import jetbrains.buildServer.sharedResources.TestUtils;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourceFeatures;
import jetbrains.buildServer.util.TestFor;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Class {@code SharedResourcesWaitPreconditionTest}
 *
 * Contains tests for {@code SharedResourcesWaitPrecondition}
 *
 * @see SharedResourcesWaitPrecondition
 * @see SharedResourcesUtils
 * *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
@TestFor(testForClass = SharedResourcesWaitPrecondition.class)
public class SharedResourcesWaitPreconditionTest extends BaseTestCase {

  private Mockery m;


  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    m = new Mockery();
  }


  /**
   * @see SharedResourcesWaitPrecondition#filterPromotions(String, java.util.Collection
   * @throws Exception if something goes wrong
   */
  @Test
  public void testFilterBuildPromotions() throws Exception {
    final String myProjectId = TestUtils.generateRandomName();
    final Collection<BuildPromotionInfo> myPromotions = new ArrayList<BuildPromotionInfo>();
    final Collection<BuildPromotionInfo> otherPromotions = new ArrayList<BuildPromotionInfo>();
    int myPromotionsSize = TestUtils.generateBoundedRandomInt();
    for (int i = 0; i < myPromotionsSize; i++) {
      myPromotions.add(m.mock(BuildPromotionEx.class, TestUtils.generateRandomName()));
    }
    int otherPromotionsSize = TestUtils.generateBoundedRandomInt();
    for (int i = 0; i < otherPromotionsSize; i++) {
      otherPromotions.add(m.mock(BuildPromotionEx.class, TestUtils.generateRandomName()));
    }
    final Collection<BuildPromotionInfo> allPromotions = new ArrayList<BuildPromotionInfo>();
    allPromotions.addAll(myPromotions);
    allPromotions.addAll(otherPromotions);

    m.checking(new Expectations() {{
      for (BuildPromotionInfo info: myPromotions) {
        oneOf((BuildPromotionEx)info).getProjectId();
        will(returnValue(myProjectId));
      }

      for (BuildPromotionInfo info: otherPromotions) {
        oneOf((BuildPromotionEx)info).getProjectId();
        will(returnValue(TestUtils.generateRandomName()));
      }
    }});

    // todo: proper test of filtering
    final SharedResourcesWaitPrecondition p =
            new SharedResourcesWaitPrecondition(m.mock(ProjectSettingsManager.class), m.mock(SharedResourceFeatures.class));
    p.filterPromotions(myProjectId, allPromotions);

    assertNotEmpty(allPromotions);
    assertEquals(myPromotionsSize, allPromotions.size());
    for (BuildPromotionInfo info: myPromotions) {
      assertContains(allPromotions, info);
    }
    for (BuildPromotionInfo info: otherPromotions) {
      assertNotContains(allPromotions, info);
    }
    m.assertIsSatisfied();
  }

  @Test
  public void testInEmulationMode() throws Exception {

  }

  @Test
  public void testNoLocksPresent() throws Exception {

  }

  @Test
  public void testLocksPresentSingleBuild() throws Exception {

  }

  @Test
  public void testMultipleBuildsLocksCrossing() throws Exception {

  }

  @Test
  public void testMultipleBuildsLocksNotCrossing() throws Exception {

  }

  @Test
  public void testBuildsFromOtherProjects() throws Exception {

  }
}
