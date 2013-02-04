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
import jetbrains.buildServer.BuildAgent;
import jetbrains.buildServer.parameters.ParametersProvider;
import jetbrains.buildServer.serverSide.BuildPromotionEx;
import jetbrains.buildServer.serverSide.BuildTypeEx;
import jetbrains.buildServer.serverSide.buildDistribution.*;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.LockType;
import jetbrains.buildServer.sharedResources.server.feature.Locks;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeatures;
import jetbrains.buildServer.sharedResources.server.feature.TakenLocks;
import jetbrains.buildServer.util.TestFor;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.*;

/**
 * Class {@code SharedResourcesWaitPreconditionTest}
 *
 * Contains tests for {@code SharedResourcesWaitPrecondition}
 *
 * @see SharedResourcesWaitPrecondition
 * *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
@SuppressWarnings("UnusedShould")
@TestFor(testForClass = SharedResourcesWaitPrecondition.class)
public class SharedResourcesWaitPreconditionTest extends BaseTestCase {

  private Mockery m;

  private Locks myLocks;

  private SharedResourcesFeatures myFeatures;

  private QueuedBuildInfo myQueuedBuild;

  private BuildPromotionEx myBuildPromotion;

  private BuildDistributorInput myBuildDistributorInput;

  private BuildTypeEx myBuildType;

  private final String myProjectId = "PROJECT_ID";

  private ParametersProvider myParametersProvider;

  private TakenLocks myTakenLocks;

  /** Class under test*/
  private SharedResourcesWaitPrecondition myWaitPrecondition;


  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    m = new Mockery();
    myLocks = m.mock(Locks.class);
    myFeatures = m.mock(SharedResourcesFeatures.class);
    myBuildType = m.mock(BuildTypeEx.class);
    myQueuedBuild = m.mock(QueuedBuildInfo.class);
    myBuildPromotion = m.mock(BuildPromotionEx.class);
    myParametersProvider = m.mock(ParametersProvider.class);
    myTakenLocks = m.mock(TakenLocks.class);
    myBuildDistributorInput = m.mock(BuildDistributorInput.class);
    myWaitPrecondition = new SharedResourcesWaitPrecondition(myFeatures, myLocks, myTakenLocks);
  }

  @Test
  public void testNullBuildType() throws Exception {
    m.checking(new Expectations() {{
      oneOf(myQueuedBuild).getBuildPromotionInfo();
      will(returnValue(myBuildPromotion));

      oneOf(myBuildPromotion).getBuildType();
      will(returnValue(null));

      oneOf(myBuildPromotion).getProjectId();
      will(returnValue(myProjectId));
    }});
    final WaitReason result = myWaitPrecondition.canStart(myQueuedBuild, Collections.<QueuedBuildInfo, BuildAgent>emptyMap(), myBuildDistributorInput, false);
    assertNull(result);
  }

  @Test
  public void testNullProjectId() throws Exception {
    m.checking(new Expectations() {{
      oneOf(myQueuedBuild).getBuildPromotionInfo();
      will(returnValue(myBuildPromotion));

      oneOf(myBuildPromotion).getBuildType();
      will(returnValue(myBuildType));

      oneOf(myBuildPromotion).getProjectId();
      will(returnValue(null));

    }});
    final WaitReason result = myWaitPrecondition.canStart(myQueuedBuild, Collections.<QueuedBuildInfo, BuildAgent>emptyMap(), myBuildDistributorInput, false);
    assertNull(result);
  }

  @Test
  public void testNoFeaturesPresent() throws Exception {
    m.checking(new Expectations() {{
      oneOf(myQueuedBuild).getBuildPromotionInfo();
      will(returnValue(myBuildPromotion));

      oneOf(myBuildPromotion).getBuildType();
      will(returnValue(myBuildType));

      oneOf(myBuildPromotion).getProjectId();
      will(returnValue(myProjectId));

      oneOf(myFeatures).featuresPresent(myBuildType);
      will(returnValue(false));

    }});
    final WaitReason result = myWaitPrecondition.canStart(myQueuedBuild, Collections.<QueuedBuildInfo, BuildAgent>emptyMap(), myBuildDistributorInput, false);
    assertNull(result);
  }

  @Test
  public void testNoLocksInFeatures() throws Exception {
    final Collection<Lock> emptyLocks = Collections.emptyList();
    final Map<String, String> emptyParams = Collections.emptyMap();

    m.checking(new Expectations() {{
      oneOf(myQueuedBuild).getBuildPromotionInfo();
      will(returnValue(myBuildPromotion));

      oneOf(myBuildPromotion).getBuildType();
      will(returnValue(myBuildType));

      oneOf(myBuildPromotion).getProjectId();
      will(returnValue(myProjectId));

      oneOf(myFeatures).featuresPresent(myBuildType);
      will(returnValue(true));

      oneOf(myBuildPromotion).getParametersProvider();
      will(returnValue(myParametersProvider));

      oneOf(myParametersProvider).getAll();
      will(returnValue(emptyParams));

      oneOf(myLocks).fromBuildParameters(emptyParams);
      will(returnValue(emptyLocks));
    }});
    final WaitReason result = myWaitPrecondition.canStart(myQueuedBuild, Collections.<QueuedBuildInfo, BuildAgent>emptyMap(), myBuildDistributorInput, false);
    assertNull(result);
  }

  @Test
  public void testLocksPresentSingleBuild() throws Exception {
    final Map<String, String> params = new HashMap<String, String>() {{
      put("teamcity.locks.readLock.lock1", "");
    }};
    final Collection<Lock> locks = new ArrayList<Lock>() {{
      add(new Lock("lock1", LockType.READ));
    }};

    m.checking(new Expectations() {{
      oneOf(myQueuedBuild).getBuildPromotionInfo();
      will(returnValue(myBuildPromotion));

      oneOf(myBuildPromotion).getBuildType();
      will(returnValue(myBuildType));

      oneOf(myBuildPromotion).getProjectId();
      will(returnValue(myProjectId));

      oneOf(myFeatures).featuresPresent(myBuildType);
      will(returnValue(true));

      oneOf(myBuildPromotion).getParametersProvider();
      will(returnValue(myParametersProvider));

      oneOf(myParametersProvider).getAll();
      will(returnValue(params));

      oneOf(myLocks).fromBuildParameters(params);
      will(returnValue(locks));

      oneOf(myBuildDistributorInput).getRunningBuilds();
      will(returnValue(Collections.emptyList()));

    }});

    final WaitReason result = myWaitPrecondition.canStart(myQueuedBuild, Collections.<QueuedBuildInfo, BuildAgent>emptyMap(), myBuildDistributorInput, false);
    assertNull(result);
  }

  @Test
  public void testNoLocksTaken() throws Exception {
    final Map<String, String> params = new HashMap<String, String>() {{
      put("teamcity.locks.readLock.lock1", "");
    }};
    final Collection<Lock> locks = new ArrayList<Lock>() {{
      add(new Lock("lock1", LockType.READ));
    }};

    final QueuedBuildInfo info = m.mock(QueuedBuildInfo.class, "queued-build-info");
    final BuildPromotionEx queuedPromotion = m.mock(BuildPromotionEx.class, "queued-promo");

    final Map<QueuedBuildInfo, BuildAgent> queuedBuilds = new HashMap<QueuedBuildInfo, BuildAgent>() {{
      put(info, m.mock(BuildAgent.class));
    }};

    final Collection<BuildPromotionInfo> buildPromotions = new ArrayList<BuildPromotionInfo>() {{
      add(queuedPromotion);
    }};

    m.checking(new Expectations() {{
      oneOf(myQueuedBuild).getBuildPromotionInfo();
      will(returnValue(myBuildPromotion));

      oneOf(myBuildPromotion).getBuildType();
      will(returnValue(myBuildType));

      oneOf(myBuildPromotion).getProjectId();
      will(returnValue(myProjectId));

      oneOf(myFeatures).featuresPresent(myBuildType);
      will(returnValue(true));

      oneOf(myBuildPromotion).getParametersProvider();
      will(returnValue(myParametersProvider));

      oneOf(myParametersProvider).getAll();
      will(returnValue(params));

      oneOf(myLocks).fromBuildParameters(params);
      will(returnValue(locks));

      oneOf(myBuildDistributorInput).getRunningBuilds();
      will(returnValue(Collections.<RunningBuildInfo>emptyList()));

      oneOf(info).getBuildPromotionInfo();
      will(returnValue(queuedPromotion));

      oneOf(queuedPromotion).getProjectId();
      will(returnValue(myProjectId));

      oneOf(info).getBuildPromotionInfo();
      will(returnValue(queuedPromotion));

      oneOf(myTakenLocks).collectTakenLocks(buildPromotions);
      will(returnValue(Collections.emptyMap()));

    }});

    final WaitReason result = myWaitPrecondition.canStart(myQueuedBuild, queuedBuilds, myBuildDistributorInput, false);
    assertNull(result);

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
