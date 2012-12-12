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

package jetbrains.buildServer.sharedResources;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.serverSide.SBuildAgent;
import jetbrains.buildServer.serverSide.buildDistribution.*;
import jetbrains.buildServer.sharedResources.model.LockType;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.*;

/**
 * Contains tests for {@code SharedResourcesBuildFeature}
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class FeatureTest extends BaseTestCase {

  private Mockery m;
  //private SharedResourcesAgentsFilter myFilter;
  private AgentsFilterContext myContext;
  private QueuedBuildInfo myQueuedBuild;
  private BuildDistributorInput myDistributorInput;
  private Map<QueuedBuildInfo, SBuildAgent> myDistributedBuildsMap;
  private BuildPromotionInfo myQueuedBuildPromotion;


  @BeforeMethod
  @Override
  @SuppressWarnings("unchecked")
  protected void setUp() throws Exception {
    super.setUp();
    m = new Mockery();
    //myFilter = new SharedResourcesAgentsFilter();
    myContext = m.mock(AgentsFilterContext.class);
    myQueuedBuild = m.mock(QueuedBuildInfo.class);
    myDistributorInput = m.mock(BuildDistributorInput.class);
    myDistributedBuildsMap = m.mock(Map.class);
    myQueuedBuildPromotion = m.mock(BuildPromotionInfo.class);
  }


  /**
   * Test case: 1 build, locks available
   */
  @Test
  public void testSingleBuild() throws Exception {
    final Map<String, String> myBuildParams = new HashMap<String, String>() {{
      put(TestUtils.generateLockAsParam("lock1", LockType.READ), "");
      put(TestUtils.generateLockAsParam("lock2", LockType.READ), "");
    }};

    final List<RunningBuildInfo> runningBuilds = new ArrayList<RunningBuildInfo>();
    final Set<QueuedBuildInfo> queuedBuilds = new HashSet<QueuedBuildInfo>();
    final List<Map<String, String>> runningParams = new ArrayList<Map<String, String>>();
    final List<BuildPromotionInfo> runningPromotions = new ArrayList<BuildPromotionInfo>();

    m.checking(constructExpectations(
            queuedBuilds, runningBuilds, runningPromotions, runningParams, myBuildParams
    ));

//    AgentsFilterResult result = myFilter.filterAgents(myContext);
//    assertNotNull(result);
//    assertNull(result.getWaitReason());
//    m.assertIsSatisfied();
  }


  /**
   * Test case: multiple builds, locks available
   */
  // todo: implement
  public void testMultipleBuildsLocksAvailable() throws Exception {
    final List<Map<String, String>> runningParams = new ArrayList<Map<String, String>>() {{
      add(new HashMap<String, String>() {{
        put(TestUtils.generateLockAsParam("lock1", LockType.READ), "");
        put(TestUtils.generateLockAsParam("lock2", LockType.READ), "");
      }});
      add(new HashMap<String, String>() {{
        put(TestUtils.generateLockAsParam("lock3", LockType.WRITE), "");
        put(TestUtils.generateLockAsParam("lock4", LockType.WRITE), "");
      }});
    }};

    final Map<String, String> myBuildParams = new HashMap<String, String>() {{
      put(TestUtils.generateLockAsParam("lock1", LockType.READ), "");
      put(TestUtils.generateLockAsParam("lock2", LockType.READ), "");
    }};

    final Set<QueuedBuildInfo> queuedBuilds = new HashSet<QueuedBuildInfo>();
    final List<RunningBuildInfo> runningBuilds = new ArrayList<RunningBuildInfo>();
    final List<BuildPromotionInfo> runningPromotions = new ArrayList<BuildPromotionInfo>();
    for (int i = 0; i < runningParams.size(); i++) {
      runningBuilds.add(m.mock(RunningBuildInfo.class, "running-build-" + i));
      runningPromotions.add(m.mock(BuildPromotionInfo.class, "running-promotion-" + i));
    }

    m.checking(constructExpectations(
            queuedBuilds, runningBuilds, runningPromotions, runningParams, myBuildParams
    ));


//    AgentsFilterResult result = myFilter.filterAgents(myContext);
//    assertNotNull(result);
//    assertNull(result.getWaitReason());
    m.assertIsSatisfied();
  }

  /**
   * Test case: multiple builds, locks unavailable
   */
  @Test
  public void testMultipleBuildsLocksUnavailable() {
    final List<Map<String, String>> runningParams = new ArrayList<Map<String, String>>() {{
      add(new HashMap<String, String>() {{
        put(TestUtils.generateLockAsParam("lock1", LockType.READ), "");
        put(TestUtils.generateLockAsParam("lock2", LockType.READ), "");
      }});
      add(new HashMap<String, String>() {{
        put(TestUtils.generateLockAsParam("lock3", LockType.WRITE), "");
        put(TestUtils.generateLockAsParam("lock4", LockType.WRITE), "");
      }});
    }};

    final Map<String, String> myBuildParams = new HashMap<String, String>() {{
      put(TestUtils.generateLockAsParam("lock3", LockType.READ), "");
      put(TestUtils.generateLockAsParam("lock4", LockType.READ), "");
    }};

    final Set<QueuedBuildInfo> queuedBuilds = new HashSet<QueuedBuildInfo>();
    final List<RunningBuildInfo> runningBuilds = new ArrayList<RunningBuildInfo>();
    final List<BuildPromotionInfo> runningPromotions = new ArrayList<BuildPromotionInfo>();
    for (int i = 0; i < runningParams.size(); i++) {
      runningBuilds.add(m.mock(RunningBuildInfo.class, "running-build-" + i));
      runningPromotions.add(m.mock(BuildPromotionInfo.class, "running-promotion-" + i));
    }

    m.checking(constructExpectations(
            queuedBuilds, runningBuilds, runningPromotions, runningParams, myBuildParams
    ));

//    AgentsFilterResult result = myFilter.filterAgents(myContext);
//    assertNotNull(result);
//    assertNotNull(result.getWaitReason());
//    m.assertIsSatisfied();

  }


  private Expectations constructExpectations(final Set<QueuedBuildInfo> queuedBuilds,
                                             final List<RunningBuildInfo> runningBuilds,
                                             final List<BuildPromotionInfo> runningPromotions,
                                             final List<Map<String, String>> runningParams,
                                             final Map<String, String> myBuildParams) {
    return new Expectations() {{
      oneOf(myContext).getStartingBuild();
      will(returnValue(myQueuedBuild));

      oneOf(myContext).getDistributorInput();
      will(returnValue(myDistributorInput));

      oneOf(myDistributorInput).getRunningBuilds();
      will(returnValue(runningBuilds));

      oneOf(myContext).getDistributedBuilds();
      will(returnValue(myDistributedBuildsMap));

      oneOf(myDistributedBuildsMap).keySet();
      will(returnValue(queuedBuilds));

      oneOf(myQueuedBuild).getBuildPromotionInfo();
      will(returnValue(myQueuedBuildPromotion));

      oneOf(myQueuedBuildPromotion).getParameters();
      will(returnValue(myBuildParams));

      for (int i = 0; i < runningBuilds.size(); i++) {
        oneOf(runningBuilds.get(i)).getBuildPromotionInfo();
        will(returnValue(runningPromotions.get(i)));

        oneOf(runningPromotions.get(i)).getParameters();
        will(returnValue(runningParams.get(i)));
      }
    }};
  }


}
