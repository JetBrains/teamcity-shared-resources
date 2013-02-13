package jetbrains.buildServer.sharedResources.server.runtime;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.serverSide.BuildPromotionEx;
import jetbrains.buildServer.serverSide.RunningBuildEx;
import jetbrains.buildServer.serverSide.buildDistribution.QueuedBuildInfo;
import jetbrains.buildServer.serverSide.buildDistribution.RunningBuildInfo;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.LockType;
import jetbrains.buildServer.sharedResources.model.TakenLock;
import jetbrains.buildServer.sharedResources.server.feature.Locks;
import jetbrains.buildServer.sharedResources.server.feature.Resources;
import jetbrains.buildServer.util.TestFor;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
@TestFor(testForClass = {TakenLocks.class, TakenLocksImpl.class})
@SuppressWarnings("UnusedShould")
public class TakenLocksImplTest extends BaseTestCase {

  private Mockery m;

  private Locks myLocks;

  @SuppressWarnings("FieldCanBeLocal") // for getUnavailableLocks
  private Resources myResources;

  private LocksStorage myLocksStorage;

  /** Class under test*/
  private TakenLocks myTakenLocks;

  private final String myProjectId = "MY_PROJECT_ID";

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    m = new Mockery();
    myLocks = m.mock(Locks.class);
    myResources = m.mock(Resources.class);
    myLocksStorage = m.mock(LocksStorage.class);
    myTakenLocks = new TakenLocksImpl(myLocks, myResources, myLocksStorage);
  }

  @Test
  public void testCollectTakenLocks_EmptyInput() throws Exception {
    final Map<String, TakenLock> result = myTakenLocks.collectTakenLocks(
            myProjectId, Collections.<RunningBuildInfo>emptyList(), Collections.<QueuedBuildInfo>emptyList());
    assertNotNull(result);
    assertEquals(0, result.size());
  }

  @Test
  public void testCollectRunningBuilds_Stored() throws Exception {
    final Map<Lock, String> takenLocks1 = new HashMap<Lock, String>() {{
      put(new Lock("lock1", LockType.READ), "");
      put(new Lock("lock2", LockType.WRITE), "");
    }};
    final Map<Lock, String> takenLocks2 = new HashMap<Lock, String>() {{
      put(new Lock("lock1", LockType.READ), "");
    }};

    final RunningBuildEx rb1 = m.mock(RunningBuildEx.class, "rb-1");
    final RunningBuildEx rb2 = m.mock(RunningBuildEx.class, "rb-2");

    final BuildPromotionEx bp1 = m.mock(BuildPromotionEx.class, "bp-1");
    final BuildPromotionEx bp2 = m.mock(BuildPromotionEx.class, "bp-2");

    final Collection<RunningBuildInfo> runningBuilds = new ArrayList<RunningBuildInfo>() {{
      add(rb1);
      add(rb2);
    }};

    m.checking(new Expectations() {{
      oneOf(rb1).getBuildPromotionInfo();
      will(returnValue(bp1));

      oneOf(bp1).getProjectId();
      will(returnValue(myProjectId));

      oneOf(myLocksStorage).locksStored(rb1);
      will(returnValue(true));

      oneOf(myLocksStorage).load(rb1);
      will(returnValue(takenLocks1));

      oneOf(rb2).getBuildPromotionInfo();
      will(returnValue(bp2));

      oneOf(bp2).getProjectId();
      will(returnValue(myProjectId));

      oneOf(myLocksStorage).locksStored(rb2);
      will(returnValue(true));

      oneOf(myLocksStorage).load(rb2);
      will(returnValue(takenLocks2));

    }});

    final Map<String, TakenLock> result = myTakenLocks.collectTakenLocks(
            myProjectId, runningBuilds, Collections.<QueuedBuildInfo>emptyList());
    assertNotNull(result);
    assertEquals(2, result.size());
    final TakenLock tl1 = result.get("lock1");
    assertNotNull(tl1);
    assertEquals(2, tl1.getReadLocks().size());

    final TakenLock tl2 = result.get("lock2");
    assertNotNull(tl2);
    assertTrue(tl2.hasWriteLocks());
    m.assertIsSatisfied();
  }

  @Test
  public void testCollectRunningQueued_Promotions() throws Exception {
    final Map<Lock, String> takenLocks1 = new HashMap<Lock, String>() {{
      put(new Lock("lock1", LockType.READ), "");
      put(new Lock("lock2", LockType.WRITE), "");
    }};
    final Map<Lock, String> takenLocks2 = new HashMap<Lock, String>() {{
      put(new Lock("lock1", LockType.READ), "");
    }};

    final RunningBuildEx rb1 = m.mock(RunningBuildEx.class, "rb-1");
    final BuildPromotionEx bp1 = m.mock(BuildPromotionEx.class, "bp-1");

    final QueuedBuildInfo qb1 = m.mock(QueuedBuildInfo.class, "qb-1");
    final BuildPromotionEx bp2 = m.mock(BuildPromotionEx.class, "bp-2");
    final Collection<RunningBuildInfo> runningBuilds = new ArrayList<RunningBuildInfo>() {{
      add(rb1);
    }};

    final Collection<QueuedBuildInfo> queuedBuilds = new ArrayList<QueuedBuildInfo>() {{
      add(qb1);
    }};


    m.checking(new Expectations() {{
      oneOf(rb1).getBuildPromotionInfo();
      will(returnValue(bp1));

      oneOf(bp1).getProjectId();
      will(returnValue(myProjectId));

      oneOf(myLocksStorage).locksStored(rb1);
      will(returnValue(false));

      oneOf(myLocks).fromBuildPromotion(bp1);
      will(returnValue(takenLocks1.keySet()));

      oneOf(qb1).getBuildPromotionInfo();
      will(returnValue(bp2));

      oneOf(bp2).getProjectId();
      will(returnValue(myProjectId));

      oneOf(myLocks).fromBuildPromotion(bp2);
      will(returnValue(takenLocks2.keySet()));
    }});

    final Map<String, TakenLock> result = myTakenLocks.collectTakenLocks(
            myProjectId, runningBuilds, queuedBuilds);
    assertNotNull(result);
    assertEquals(2, result.size());
    final TakenLock tl1 = result.get("lock1");
    assertNotNull(tl1);
    assertEquals(2, tl1.getReadLocks().size());

    final TakenLock tl2 = result.get("lock2");
    assertNotNull(tl2);
    assertTrue(tl2.hasWriteLocks());
    m.assertIsSatisfied();
  }
}
