package jetbrains.buildServer.sharedResources.server.runtime;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.serverSide.BuildPromotionEx;
import jetbrains.buildServer.serverSide.RunningBuildEx;
import jetbrains.buildServer.serverSide.SRunningBuild;
import jetbrains.buildServer.serverSide.buildDistribution.BuildPromotionInfo;
import jetbrains.buildServer.serverSide.buildDistribution.QueuedBuildInfo;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.LockType;
import jetbrains.buildServer.sharedResources.model.TakenLock;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.model.resources.ResourceFactory;
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

  private Resources myResources;

  private LocksStorage myLocksStorage;

  /**
   * Class under test
   */
  private TakenLocks myTakenLocks;

  private final String myProjectId = "MY_PROJECT_ID";

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    m = new Mockery();
    myLocks = m.mock(Locks.class);
    myResources = m.mock(Resources.class);
    myLocksStorage = m.mock(LocksStorage.class);
    myTakenLocks = new TakenLocksImpl(myLocks, myResources, myLocksStorage);
  }

  @Test
  public void testCollectTakenLocks_EmptyInput() throws Exception {
    final Map<String, TakenLock> result = myTakenLocks.collectTakenLocks(
            myProjectId, Collections.<SRunningBuild>emptyList(), Collections.<QueuedBuildInfo>emptyList());
    assertNotNull(result);
    assertEquals(0, result.size());
  }

  @Test
  public void testCollectRunningBuilds_Stored() throws Exception {
    final Map<String, Lock> takenLocks1 = new HashMap<String, Lock>() {{
      put("lock1", new Lock("lock1", LockType.READ, ""));
      put("lock2", new Lock("lock2", LockType.WRITE, ""));

    }};

    final Map<String, Lock> takenLocks2 = new HashMap<String, Lock>() {{
      put("lock1", new Lock("lock1", LockType.READ, ""));
    }};

    final RunningBuildEx rb1 = m.mock(RunningBuildEx.class, "rb-1");
    final RunningBuildEx rb2 = m.mock(RunningBuildEx.class, "rb-2");

    final BuildPromotionEx bp1 = m.mock(BuildPromotionEx.class, "bp-1");
    final BuildPromotionEx bp2 = m.mock(BuildPromotionEx.class, "bp-2");

    final Collection<SRunningBuild> runningBuilds = new ArrayList<SRunningBuild>() {{
      add(rb1);
      add(rb2);
    }};

    m.checking(new Expectations() {{
      oneOf(rb1).getBuildPromotionInfo();
      will(returnValue(bp1));

      oneOf(myLocksStorage).locksStored(rb1);
      will(returnValue(true));

      oneOf(myLocksStorage).load(rb1);
      will(returnValue(takenLocks1));

      oneOf(rb2).getBuildPromotionInfo();
      will(returnValue(bp2));

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
    final Collection<SRunningBuild> runningBuilds = new ArrayList<SRunningBuild>() {{
      add(rb1);
    }};

    final Collection<QueuedBuildInfo> queuedBuilds = new ArrayList<QueuedBuildInfo>() {{
      add(qb1);
    }};


    m.checking(new Expectations() {{
      oneOf(rb1).getBuildPromotionInfo();
      will(returnValue(bp1));

      oneOf(myLocksStorage).locksStored(rb1);
      will(returnValue(false));

      oneOf(myLocks).fromBuildPromotion(bp1);
      will(returnValue(takenLocks1.keySet()));

      oneOf(qb1).getBuildPromotionInfo();
      will(returnValue(bp2));

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

  @Test
  public void testGetUnavailableLocks_Custom_All() throws Exception {
    final Map<String, Resource> resources = new HashMap<String, Resource>();
    resources.put("custom_resource1", ResourceFactory.newCustomResource("custom_resource1", Arrays.asList("v1", "v2")));

    final Collection<Lock> locksToTake = new ArrayList<Lock>() {{
      add(new Lock("custom_resource1", LockType.WRITE));
    }};

    final Map<String, TakenLock> takenLocks = new HashMap<String, TakenLock>() {{
      TakenLock tl1 = new TakenLock();
      tl1.addLock(m.mock(BuildPromotionInfo.class), new Lock("custom_resource1", LockType.READ));
      put("custom_resource1", tl1);
    }};

    m.checking(new Expectations() {{
      oneOf(myResources).asMap(myProjectId);
      will(returnValue(resources));
    }});

    final Set<String> fairSet = new HashSet<String>();

    final Collection result = myTakenLocks.getUnavailableLocks(locksToTake, takenLocks, myProjectId, fairSet);
    assertNotNull(result);
    assertEquals(1, result.size());
  }

  @Test
  public void testGetUnavailableLocks_Custom_Specific() throws Exception {
    final Map<String, Resource> resources = new HashMap<String, Resource>();
    resources.put("custom_resource1", ResourceFactory.newCustomResource("custom_resource1", Arrays.asList("v1", "v2")));

    final Collection<Lock> locksToTake = new ArrayList<Lock>() {{
      add(new Lock("custom_resource1", LockType.WRITE, "v1"));
    }};

    final Map<String, TakenLock> takenLocks = new HashMap<String, TakenLock>() {{
      TakenLock tl1 = new TakenLock();
      tl1.addLock(m.mock(BuildPromotionInfo.class), new Lock("custom_resource1", LockType.READ, "v1"));
      put("custom_resource1", tl1);
    }};

    m.checking(new Expectations() {{
      oneOf(myResources).asMap(myProjectId);
      will(returnValue(resources));
    }});

    final Set<String> fairSet = new HashSet<String>();

    final Collection result = myTakenLocks.getUnavailableLocks(locksToTake, takenLocks, myProjectId, fairSet);
    assertNotNull(result);
    assertEquals(1, result.size());

  }

  @Test
  public void testGetUnavailableLocks_Custom_Any() throws Exception {
    // case when write lock ALL is taken
    final Map<String, Resource> resources = new HashMap<String, Resource>();
    resources.put("custom_resource1", ResourceFactory.newCustomResource("custom_resource1", Arrays.asList("v1", "v2")));

    final Collection<Lock> locksToTake = new ArrayList<Lock>() {{
      add(new Lock("custom_resource1", LockType.READ));
    }};

    final Map<String, TakenLock> takenLocks = new HashMap<String, TakenLock>() {{
      TakenLock tl1 = new TakenLock();
      tl1.addLock(m.mock(BuildPromotionInfo.class), new Lock("custom_resource1", LockType.WRITE));
      put("custom_resource1", tl1);
    }};

    m.checking(new Expectations() {{
      oneOf(myResources).asMap(myProjectId);
      will(returnValue(resources));
    }});

    final Set<String> fairSet = new HashSet<String>();

    final Collection result = myTakenLocks.getUnavailableLocks(locksToTake, takenLocks, myProjectId, fairSet);
    assertNotNull(result);
    assertEquals(1, result.size());
  }

  @Test
  public void testGetUnavailableLocks_Custom_Any_NoValuesAvailable() throws Exception {
    final Map<String, Resource> resources = new HashMap<String, Resource>();
    resources.put("custom_resource1", ResourceFactory.newCustomResource("custom_resource1", Arrays.asList("v1", "v2")));

    final Collection<Lock> locksToTake = new ArrayList<Lock>() {{
      add(new Lock("custom_resource1", LockType.READ));
    }};

    final Map<String, TakenLock> takenLocks = new HashMap<String, TakenLock>() {{
      TakenLock tl1 = new TakenLock();
      tl1.addLock(m.mock(BuildPromotionInfo.class, "bp1"), new Lock("custom_resource1", LockType.READ, "v1"));
      tl1.addLock(m.mock(BuildPromotionInfo.class, "bp2"), new Lock("custom_resource1", LockType.READ, "v2"));
      put("custom_resource1", tl1);
    }};

    m.checking(new Expectations() {{
      oneOf(myResources).asMap(myProjectId);
      will(returnValue(resources));
    }});


    final Set<String> fairSet = new HashSet<String>();

    final Collection result = myTakenLocks.getUnavailableLocks(locksToTake, takenLocks, myProjectId, fairSet);
    assertNotNull(result);
    assertEquals(1, result.size());
  }

  @Test
  public void testGetUnavailableLocks_ReadRead_Quota() throws Exception {
    final Map<String, Resource> resources = new HashMap<String, Resource>();
    resources.put("quoted_resource1", ResourceFactory.newQuotedResource("quoted_resource1", 2));

    final Collection<Lock> locksToTake = new ArrayList<Lock>() {{
      add(new Lock("quoted_resource1", LockType.READ));
    }};

    final Map<String, TakenLock> takenLocks = new HashMap<String, TakenLock>() {{
      TakenLock tl1 = new TakenLock();
      tl1.addLock(m.mock(BuildPromotionInfo.class, "bp1"), new Lock("quoted_resource1", LockType.READ));
      tl1.addLock(m.mock(BuildPromotionInfo.class, "bp2"), new Lock("quoted_resource1", LockType.READ));
      put("quoted_resource1", tl1);
    }};

    m.checking(new Expectations() {{
      oneOf(myResources).asMap(myProjectId);
      will(returnValue(resources));
    }});
    final Set<String> fairSet = new HashSet<String>();

    final Collection result = myTakenLocks.getUnavailableLocks(locksToTake, takenLocks, myProjectId, fairSet);
    assertNotNull(result);
    assertEquals(1, result.size());

  }

  @Test
  public void testGetUnavailableLocks_ReadWrite() throws Exception {
    final Map<String, Resource> resources = new HashMap<String, Resource>();
    resources.put("quoted_resource1", ResourceFactory.newQuotedResource("quoted_resource1", 2));

    final Collection<Lock> locksToTake = new ArrayList<Lock>() {{
      add(new Lock("quoted_resource1", LockType.READ));
    }};

    final Map<String, TakenLock> takenLocks = new HashMap<String, TakenLock>() {{
      TakenLock tl1 = new TakenLock();
      tl1.addLock(m.mock(BuildPromotionInfo.class, "bp1"), new Lock("quoted_resource1", LockType.WRITE));
      put("quoted_resource1", tl1);
    }};

    m.checking(new Expectations() {{
      oneOf(myResources).asMap(myProjectId);
      will(returnValue(resources));
    }});

    final Set<String> fairSet = new HashSet<String>();

    final Collection result = myTakenLocks.getUnavailableLocks(locksToTake, takenLocks, myProjectId, fairSet);
    assertNotNull(result);
    assertEquals(1, result.size());
  }

  @Test
  public void testGetUnavailableLocks_WriteRead() throws Exception {
    final Map<String, Resource> resources = new HashMap<String, Resource>();
    resources.put("quoted_resource1", ResourceFactory.newQuotedResource("quoted_resource1", 2));
    final Collection<Lock> locksToTake = new ArrayList<Lock>() {{
      add(new Lock("quoted_resource1", LockType.WRITE));
    }};
    final Map<String, TakenLock> takenLocks = new HashMap<String, TakenLock>() {{
      TakenLock tl1 = new TakenLock();
      tl1.addLock(m.mock(BuildPromotionInfo.class, "bp1"), new Lock("quoted_resource1", LockType.READ));
      put("quoted_resource1", tl1);
    }};
    m.checking(new Expectations() {{
      oneOf(myResources).asMap(myProjectId);
      will(returnValue(resources));
    }});

    final Set<String> fairSet = new HashSet<String>();

    final Collection result = myTakenLocks.getUnavailableLocks(locksToTake, takenLocks, myProjectId, fairSet);
    assertNotNull(result);
    assertEquals(1, result.size());
  }


  /**
   *
   * Test setup:
   * - fairSet is empty
   * - 1 infinite resource
   * - 1 build holds read lock
   * - 1 build tries to pass through agents filter (with write lock request)
   *
   * Expected results:
   *  - wait reason is returned
   *  - fairSet should contain write lock name requested
   *
   * @throws Exception if something goes wrong
   */
  @Test
  @TestFor (issues = "TW-28307")
  public void testGetUnavailableLocks_WritePrioritySet() throws Exception {
    final String resourceName = "resource";
    final Map<String, Resource> resources = new HashMap<String, Resource>();
    resources.put(resourceName, ResourceFactory.newInfiniteResource(resourceName));
    final Collection<Lock> locksToTake = new ArrayList<Lock>() {{
      add(new Lock(resourceName, LockType.WRITE));
    }};

    final Map<String, TakenLock> takenLocks = new HashMap<String, TakenLock>() {{
      final TakenLock tl = new TakenLock();
      tl.addLock(m.mock(BuildPromotionInfo.class, "bp1"), new Lock(resourceName, LockType.READ));
      put(resourceName, tl);
    }};

    m.checking(new Expectations() {{
      oneOf(myResources).asMap(myProjectId);
      will(returnValue(resources));
    }});
    final Set<String> fairSet = new HashSet<String>();
    final Collection<Lock> result = myTakenLocks.getUnavailableLocks(locksToTake, takenLocks, myProjectId, fairSet);
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(resourceName, result.iterator().next().getName());
    assertNotEmpty(fairSet);
    assertEquals(1, fairSet.size());
    assertEquals(resourceName, fairSet.iterator().next());
  }

  /**
   * Test setup:
   * - fairSet is empty
   * - 1 infinite resource
   * - 1 build holds read lock
   * - 1 build tries to pass through agents filter (with write lock request)
   * - 1 another build tries to pass through agents filter (with read lock request)
   *
   * Expected results
   * - collection of unavailable locks for both builds is not empty (i.e. both builds are not allowed to start)
   *
   * @throws Exception if something goes wrong
   */
  @Test
  @TestFor (issues = "TW-28307")
  public void testGetUnavailableLocks_PreservePriority() throws Exception {
    final String resourceName = "resource";
    final Map<String, Resource> resources = new HashMap<String, Resource>();
    resources.put(resourceName, ResourceFactory.newInfiniteResource(resourceName));

    final Collection<Lock> writeLockToTake = new ArrayList<Lock>() {{
      add(new Lock(resourceName, LockType.WRITE));
    }};

    final Collection<Lock> readLockToTake = new ArrayList<Lock>() {{
      add(new Lock(resourceName, LockType.READ));
    }};

    final Map<String, TakenLock> takenLocks = new HashMap<String, TakenLock>() {{
      final TakenLock tl = new TakenLock();
      tl.addLock(m.mock(BuildPromotionInfo.class, "bp1"), new Lock(resourceName, LockType.READ));
      put(resourceName, tl);
    }};

    m.checking(new Expectations() {{
      allowing(myResources).asMap(myProjectId);
      will(returnValue(resources));
    }});


    final Set<String> fairSet = new HashSet<String>();

    { // 1) Check that read-read locks are working
      final Collection<Lock> result = myTakenLocks.getUnavailableLocks(readLockToTake, takenLocks, myProjectId, fairSet);
      assertNotNull(result);
      assertEmpty(result);
      assertEmpty(fairSet);
    }

    { // 2) Check that fair set influences read lock processing
      Collection<Lock> result = myTakenLocks.getUnavailableLocks(writeLockToTake, takenLocks, myProjectId, fairSet);
      assertNotNull(result);
      assertEquals(1, result.size());
      assertEquals(resourceName, result.iterator().next().getName());
      assertEquals(1, fairSet.size());
      assertEquals(resourceName, fairSet.iterator().next());

      // now we have lock name in fair set. read lock must not be acquired
      result = myTakenLocks.getUnavailableLocks(readLockToTake, takenLocks, myProjectId, fairSet);
      assertNotNull(result);
      assertEquals(1, result.size());
      assertEquals(resourceName, result.iterator().next().getName());
      assertEquals(1, fairSet.size());
      assertEquals(resourceName, fairSet.iterator().next());
    }
  }


  /**
   * Same as TakenLocksImplTest#testGetUnavailableLocks_PreservePriority but for custom resources
   *
   * @throws Exception if something goes wrong
   */
  @Test
  @TestFor (issues = "TW-28307")
  public void testGetUnavailableLocks_Custom_Fair() throws Exception {
    final String resourceName = "resource";
    final Map<String, Resource> resources = new HashMap<String, Resource>();
    resources.put(resourceName, ResourceFactory.newCustomResource(resourceName, Arrays.asList("val1", "val2", "val3")));

    final Collection<Lock> allLockToTake = new ArrayList<Lock>() {{
      add(new Lock(resourceName, LockType.WRITE));
    }};

    final Collection<Lock> anyLockToTake = new ArrayList<Lock>() {{
      add(new Lock(resourceName, LockType.READ));
    }};

    final Map<String, TakenLock> takenLocksAny = new HashMap<String, TakenLock>() {{
      final TakenLock tl = new TakenLock();
      tl.addLock(m.mock(BuildPromotionInfo.class, "bp1"), new Lock(resourceName, LockType.READ));
      put(resourceName, tl);
    }};

    final Map<String, TakenLock> takenLocksSpecific = new HashMap<String, TakenLock>() {{
      final TakenLock tl = new TakenLock();
      tl.addLock(m.mock(BuildPromotionInfo.class, "bp2"), new Lock(resourceName, LockType.READ, "val1"));
      put(resourceName, tl);
    }};

    m.checking(new Expectations() {{
      allowing(myResources).asMap(myProjectId);
      will(returnValue(resources));
    }});

    final Set<String> fairSet = new HashSet<String>();

    { // Check that any-any locks are working
      final Collection<Lock> result = myTakenLocks.getUnavailableLocks(anyLockToTake, takenLocksAny, myProjectId, fairSet);
      assertNotNull(result);
      assertEmpty(result);
      assertEmpty(fairSet);
    }

    { // Check that any-apecific locks are working
      final Collection<Lock> result = myTakenLocks.getUnavailableLocks(anyLockToTake, takenLocksSpecific, myProjectId, fairSet);
      assertNotNull(result);
      assertEmpty(result);
      assertEmpty(fairSet);
    }

    { // Check that fair set influences read lock processing
      Collection<Lock> result = myTakenLocks.getUnavailableLocks(allLockToTake, takenLocksAny, myProjectId, fairSet);
      assertNotNull(result);
      assertEquals(1, result.size());
      assertEquals(resourceName, result.iterator().next().getName());
      assertEquals(1, fairSet.size());
      assertEquals(resourceName, fairSet.iterator().next());

      // now we have lock name in fair set. any lock must not be acquired
      result = myTakenLocks.getUnavailableLocks(anyLockToTake, takenLocksAny, myProjectId, fairSet);
      assertNotNull(result);
      assertEquals(1, result.size());
      assertEquals(resourceName, result.iterator().next().getName());
      assertEquals(1, fairSet.size());
      assertEquals(resourceName, fairSet.iterator().next());
    }
  }
}
