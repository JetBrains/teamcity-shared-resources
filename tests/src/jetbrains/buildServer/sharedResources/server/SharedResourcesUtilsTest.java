package jetbrains.buildServer.sharedResources.server;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.parameters.ParametersProvider;
import jetbrains.buildServer.serverSide.BuildPromotionEx;
import jetbrains.buildServer.serverSide.buildDistribution.BuildPromotionInfo;
import jetbrains.buildServer.serverSide.buildDistribution.QueuedBuildInfo;
import jetbrains.buildServer.serverSide.buildDistribution.RunningBuildInfo;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.TestUtils;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.LockType;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * Date: 02.10.12
 * Time: 15:53
 *
 * @author Oleg Rybak
 */
public class SharedResourcesUtilsTest extends BaseTestCase {

  public static final int RANDOM_UPPER_BOUNDARY = 20;
  private static final int MAX_BUILDS = RANDOM_UPPER_BOUNDARY;
  private static final Random R = new Random();
  private Mockery m;
  private BuildPromotionEx myBuildPromotion;
  private ParametersProvider myParametersProvider;
  private Collection<QueuedBuildInfo> myQueuedBuilds;
  private Collection<RunningBuildInfo> myRunningBuilds;


  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    m = new Mockery();
    myBuildPromotion = m.mock(BuildPromotionEx.class);
    myQueuedBuilds = new ArrayList<QueuedBuildInfo>();
    myRunningBuilds = new ArrayList<RunningBuildInfo>();
    myParametersProvider = m.mock(ParametersProvider.class);

    {
      int numBuilds = 1 + R.nextInt(MAX_BUILDS);
      for (int i = 0; i < numBuilds; i++) {
        myQueuedBuilds.add(m.mock(QueuedBuildInfo.class, "queuedBuild" + i));
      }
    }

    {
      int numBuilds = R.nextInt(MAX_BUILDS);
      for (int i = 0; i < numBuilds; i++) {
        myRunningBuilds.add(m.mock(RunningBuildInfo.class, "runningBuild" + i));
      }
    }
  }


  /**
   * @see SharedResourcesUtils#featureParamToBuildParams(String)
   * @throws Exception if something goes wrong
   */
  @Test
  public void testFeatureParamToBuildParams_NullAndEmpty() throws Exception {
    { // null input
      final Map<String, String> result = SharedResourcesUtils.featureParamToBuildParams(null);
      assertNotNull("Expected empty map on null input, received null", result);
      assertTrue("Expected empty map on null input, got [" + result.toString() + "]", result.isEmpty());
    }

    { // empty input
      final Map<String, String> result = SharedResourcesUtils.featureParamToBuildParams("");
      assertNotNull("Expected empty map on empty input, received null", result);
      assertTrue("Expected empty map on empty input, got [" + result.toString() + "]", result.isEmpty());
    }
  }

  /**
   * @see SharedResourcesUtils#featureParamToBuildParams(String)
   * @throws Exception if something goes wrong
   */
  @Test
  public void testFeatureParamToBuildParams_Valid() throws Exception {
    {
      int num = 1 + R.nextInt(RANDOM_UPPER_BOUNDARY);
      final List<String> serializedParams = new ArrayList<String>(num);
      for (int i = 0; i < num; i++) {
        serializedParams.add(TestUtils.generateSerializedLock());
      }
      final StringBuilder sb = new StringBuilder();
      for (String str: serializedParams) {
        sb.append(str).append("\n");
      }
      final String paramsAsString = sb.substring(0, sb.length() - 2);
      final Map<String, String> buildParams = SharedResourcesUtils.featureParamToBuildParams(paramsAsString);
      assertNotNull(buildParams);
      assertFalse("Expected not empty map for input of size [" + num + "]", buildParams.isEmpty());
      assertEquals("Expected that all params [" + num + "] are parsed correctly. Resulting map size is [" + buildParams.size() + "]", num, buildParams.size());
    }
  }


  /**
   * @see SharedResourcesUtils#getLockFromBuildParam(String)
   * @throws Exception if something goes wrong
   */
  @Test
  public void testGetLockFromBuildParam_Valid() throws Exception {
    final String lockName = "LOCK";
    {
      final String paramName = TestUtils.generateLockAsParam(LockType.READ, lockName);
      final Lock lock = SharedResourcesUtils.getLockFromBuildParam(paramName);
      assertNotNull(lock);
      assertEquals(LockType.READ, lock.getType());
    }

    {
      final String paramName = TestUtils.generateLockAsParam(LockType.WRITE, lockName);
      final Lock lock = SharedResourcesUtils.getLockFromBuildParam(paramName);
      assertNotNull(lock);
      assertEquals(LockType.WRITE, lock.getType());
      assertEquals(lockName, lock.getName());
    }
  }

  /**
   * @see SharedResourcesUtils#getLockFromBuildParam(String)
   * @throws Exception if something goes wrong
   */
  @Test
  public void testGetLockFromBuildParam_Invalid() throws Exception {
    { // invalid format
      String paramName = TestUtils.generateRandomSystemParam();
      Lock lock = SharedResourcesUtils.getLockFromBuildParam(paramName);
      assertNull(lock);
    }

    { // invalid type
      String paramName = SharedResourcesPluginConstants.LOCK_PREFIX + "someInvalidLockType";
      Lock lock = SharedResourcesUtils.getLockFromBuildParam(paramName);
      assertNull(lock);
    }

    { // no name specified
      String paramName = SharedResourcesPluginConstants.LOCK_PREFIX + "readLock";
      Lock lock = SharedResourcesUtils.getLockFromBuildParam(paramName);
      assertNull(lock);
    }

    { // invalid name (empty string) specified
      String paramName = SharedResourcesPluginConstants.LOCK_PREFIX + "writeLock.";
      Lock lock = SharedResourcesUtils.getLockFromBuildParam(paramName);
      assertNull(lock);
    }
  }

  /**
   * @see SharedResourcesUtils#extractLocksFromPromotion(jetbrains.buildServer.serverSide.buildDistribution.BuildPromotionInfo)
   * @throws Exception if something goes wrong
   */
  @Test
  public void testExtractLocksFromPromotion() throws Exception {
    final Map<String, String> params = new HashMap<String, String>();
    final int numReadLocks = 1 + R.nextInt(RANDOM_UPPER_BOUNDARY);
    final int numWriteLocks = 1 + R.nextInt(RANDOM_UPPER_BOUNDARY);
    final int numOtherParams = 1 + R.nextInt(RANDOM_UPPER_BOUNDARY);
    for (int i = 0; i < numReadLocks; i++) {
      params.put(TestUtils.generateLockAsParam(LockType.READ, "read" + i), "");
    }
    for (int i = 0; i < numWriteLocks; i++) {
      params.put(TestUtils.generateLockAsParam(LockType.WRITE, "write" + i), "");
    }
    for (int i = 0; i < numOtherParams; i++) {
      params.put(TestUtils.generateRandomSystemParam(), "");
    }

    m.checking(new Expectations() {{
      oneOf(myBuildPromotion).getParametersProvider();
      will(returnValue(myParametersProvider));

      oneOf(myParametersProvider).getAll();
      will(returnValue(params));

    }});

    Collection<Lock> locks = SharedResourcesUtils.extractLocksFromPromotion(myBuildPromotion);
    assertNotNull(locks);
    assertEquals(numReadLocks + numWriteLocks, locks.size());
  }


  @Test
  public void testGetBuildPromotions() throws Exception {
    m.checking(new Expectations() {{
      for (RunningBuildInfo myRunningBuild : myRunningBuilds) {
        oneOf(myRunningBuild).getBuildPromotionInfo();
      }
      for (QueuedBuildInfo myQueuedBuild : myQueuedBuilds) {
        oneOf(myQueuedBuild).getBuildPromotionInfo();
      }

    }});

    Collection<BuildPromotionInfo> buildPromotions = SharedResourcesUtils.getBuildPromotions(myRunningBuilds, myQueuedBuilds);
    assertNotNull(buildPromotions);
    assertEquals(myRunningBuilds.size() + myQueuedBuilds.size(), buildPromotions.size());
  }

  // todo: implement
  public void testGetUnavailableLocks() throws Exception {
    // taken locks
    final List<Map<String, String>> paramsList = new ArrayList<Map<String, String>>();
    paramsList.add(new HashMap<String, String>() {{
      put(TestUtils.generateLockAsParam(LockType.READ, "lock1"), "");
      put(TestUtils.generateLockAsParam(LockType.READ, "lock2"), "");
    }});
    paramsList.add(new HashMap<String, String>() {{
      put(TestUtils.generateLockAsParam(LockType.WRITE, "lock1"), "");
      put(TestUtils.generateLockAsParam(LockType.READ, "lock2"), "");
    }});

    final List<Lock> locksToTake = new ArrayList<Lock>() {{
      add(new Lock("lock1", LockType.WRITE));
      add(new Lock("lock2", LockType.READ));
    }};

    final List<BuildPromotionInfo> promotions = new ArrayList<BuildPromotionInfo>();
    promotions.add(m.mock(BuildPromotionInfo.class, "promo-1"));
    promotions.add(m.mock(BuildPromotionInfo.class, "promo-2"));

    m.checking(new Expectations() {{
      oneOf(promotions.get(0)).getParameters();
      will(returnValue(paramsList.get(0)));

      oneOf(promotions.get(1)).getParameters();
      will(returnValue(paramsList.get(1)));
    }});

    Collection<Lock> unavailableLocks = SharedResourcesUtils.getUnavailableLocks(locksToTake, promotions);
    assertNotNull(unavailableLocks);
    assertNotEmpty(unavailableLocks);
    assertEquals(1, unavailableLocks.size());
  }

}
