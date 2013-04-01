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
import jetbrains.buildServer.serverSide.BuildPromotionEx;
import jetbrains.buildServer.serverSide.BuildTypeEx;
import jetbrains.buildServer.serverSide.buildDistribution.*;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.LockType;
import jetbrains.buildServer.sharedResources.model.TakenLock;
import jetbrains.buildServer.sharedResources.server.feature.Locks;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeatures;
import jetbrains.buildServer.sharedResources.server.runtime.TakenLocks;
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

    m.checking(new Expectations() {{
      oneOf(myQueuedBuild).getBuildPromotionInfo();
      will(returnValue(myBuildPromotion));

      oneOf(myBuildPromotion).getBuildType();
      will(returnValue(myBuildType));

      oneOf(myBuildPromotion).getProjectId();
      will(returnValue(myProjectId));

      oneOf(myFeatures).featuresPresent(myBuildType);
      will(returnValue(true));

      oneOf(myLocks).fromBuildPromotion(myBuildPromotion);
      will(returnValue(emptyLocks));
    }});
    final WaitReason result = myWaitPrecondition.canStart(myQueuedBuild, Collections.<QueuedBuildInfo, BuildAgent>emptyMap(), myBuildDistributorInput, false);
    assertNull(result);
  }

  @Test
  public void testLocksPresentSingleBuild() throws Exception {
    final Collection<Lock> locks = new ArrayList<Lock>() {{
      add(new Lock("lock1", LockType.READ));
    }};
    final Map<QueuedBuildInfo, BuildAgent> canBeStarted = Collections.emptyMap();
    final Collection<RunningBuildInfo> runningBuilds = Collections.emptyList();

    m.checking(new Expectations() {{
      oneOf(myQueuedBuild).getBuildPromotionInfo();
      will(returnValue(myBuildPromotion));

      oneOf(myBuildPromotion).getBuildType();
      will(returnValue(myBuildType));

      oneOf(myBuildPromotion).getProjectId();
      will(returnValue(myProjectId));

      oneOf(myFeatures).featuresPresent(myBuildType);
      will(returnValue(true));

      oneOf(myFeatures).searchForFeatures(myBuildType);
      will(returnValue(Collections.emptyList()));

      oneOf(myLocks).fromBuildPromotion(myBuildPromotion);
      will(returnValue(locks));

      oneOf(myBuildDistributorInput).getRunningBuilds();
      will(returnValue(runningBuilds));

      oneOf(myTakenLocks).collectTakenLocks(myProjectId, runningBuilds, canBeStarted.keySet());
      will(returnValue(Collections.emptyMap()));

    }});

    final WaitReason result = myWaitPrecondition.canStart(myQueuedBuild, canBeStarted, myBuildDistributorInput, false);
    assertNull(result);
  }


  @Test
  public void testMultipleBuildsLocksNotCrossing() throws Exception {
    final Collection<Lock> locks = new ArrayList<Lock>() {{
      add(new Lock("lock1", LockType.READ));
    }};
    final Map<QueuedBuildInfo, BuildAgent> canBeStarted = Collections.emptyMap();
    final Collection<RunningBuildInfo> runningBuilds = Collections.emptyList();

    final Map<String, TakenLock> takenLocks = new HashMap<String, TakenLock>() {{
      final TakenLock tl = new TakenLock();
      tl.addLock(m.mock(BuildPromotionInfo.class), new Lock("lock2", LockType.READ));
      put("lock2", tl);

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

      oneOf(myFeatures).searchForFeatures(myBuildType);
      will(returnValue(Collections.emptyList()));

      oneOf(myLocks).fromBuildPromotion(myBuildPromotion);
      will(returnValue(locks));

      oneOf(myBuildDistributorInput).getRunningBuilds();
      will(returnValue(runningBuilds));

      oneOf(myTakenLocks).collectTakenLocks(myProjectId, runningBuilds, canBeStarted.keySet());
      will(returnValue(takenLocks));

      oneOf(myTakenLocks).getUnavailableLocks(locks, takenLocks, myProjectId);
      will(returnValue(Collections.emptyList()));

    }});

    final WaitReason result = myWaitPrecondition.canStart(myQueuedBuild, canBeStarted, myBuildDistributorInput, false);
    assertNull(result);

  }

  @Test
  public void testMultipleBuildsLocksCrossing() throws Exception {
    final Collection<Lock> locks = new ArrayList<Lock>() {{
      add(new Lock("lock1", LockType.READ));
    }};
    final Map<QueuedBuildInfo, BuildAgent> canBeStarted = Collections.emptyMap();
    final Collection<RunningBuildInfo> runningBuilds = Collections.emptyList();

    final BuildPromotionEx bpex = m.mock(BuildPromotionEx.class, "bpex");
    final Map<String, TakenLock> takenLocks = new HashMap<String, TakenLock>() {{
      final TakenLock tl = new TakenLock();
      tl.addLock(bpex, new Lock("lock1", LockType.WRITE));
      put("lock1", tl);
    }};

    final BuildTypeEx btex = m.mock(BuildTypeEx.class, "bpex-btex");
    final String name = "UNAVAILABLE";

    m.checking(new Expectations() {{
      oneOf(myQueuedBuild).getBuildPromotionInfo();
      will(returnValue(myBuildPromotion));

      oneOf(myBuildPromotion).getBuildType();
      will(returnValue(myBuildType));

      oneOf(myBuildPromotion).getProjectId();
      will(returnValue(myProjectId));

      oneOf(myFeatures).featuresPresent(myBuildType);
      will(returnValue(true));

      oneOf(myFeatures).searchForFeatures(myBuildType);
      will(returnValue(Collections.emptyList()));

      oneOf(myLocks).fromBuildPromotion(myBuildPromotion);
      will(returnValue(locks));

      oneOf(myBuildDistributorInput).getRunningBuilds();
      will(returnValue(runningBuilds));

      oneOf(myTakenLocks).collectTakenLocks(myProjectId, runningBuilds, canBeStarted.keySet());
      will(returnValue(takenLocks));

      oneOf(myTakenLocks).getUnavailableLocks(locks, takenLocks, myProjectId);
      will(returnValue(locks));

      oneOf(bpex).getBuildType();
      will(returnValue(btex));

      oneOf(btex).getName();
      will(returnValue(name));
    }});

    final WaitReason result = myWaitPrecondition.canStart(myQueuedBuild, canBeStarted, myBuildDistributorInput, false);
    assertNotNull(result);
    assertNotNull(result.getDescription());
  }
}
