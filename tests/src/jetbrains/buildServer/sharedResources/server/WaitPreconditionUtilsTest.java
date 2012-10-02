package jetbrains.buildServer.sharedResources.server;

import jetbrains.buildServer.BaseTestCase;
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
public class WaitPreconditionUtilsTest extends BaseTestCase {

  private static final int MAX_BUILDS = 20;

  private Mockery m;
  private BuildPromotionInfo myBuildPromotion;
  private Collection<QueuedBuildInfo> myQueuedBuilds;
  private Collection<RunningBuildInfo> myRunningBuilds;

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    m = new Mockery();
    myBuildPromotion = m.mock(BuildPromotionInfo.class);
    myQueuedBuilds = new ArrayList<QueuedBuildInfo>();
    myRunningBuilds = new ArrayList<RunningBuildInfo>();

    {
      int numBuilds = new Random(System.currentTimeMillis()).nextInt(MAX_BUILDS);
      for (int i = 0; i < numBuilds; i++) {
        myQueuedBuilds.add(m.mock(QueuedBuildInfo.class, "queuedBuild" + i));
      }
    }

    {
      int numBuilds = new Random(System.currentTimeMillis()).nextInt(MAX_BUILDS);
      for (int i = 0; i < numBuilds; i++) {
        myRunningBuilds.add(m.mock(RunningBuildInfo.class, "runningBuild" + i));
      }
    }
  }


  @Test
  public void testGetLockFromBuildParam_Read() throws Exception {
    final String lockName = "myReadLock";
    String paramName = TestUtils.generateLockAsParam(LockType.READ, lockName);
    Lock lock = WaitPreconditionUtils.getLockFromBuildParam(paramName);
    assertNotNull(lock);
    assertEquals(LockType.READ, lock.getType());
    assertEquals(lockName, lock.getName());
  }

  @Test
  public void testGetLockFromBuildParam_Write() throws Exception {
    final String lockName = "myWriteLock";
    String paramName = TestUtils.generateLockAsParam(LockType.WRITE, lockName);
    Lock lock = WaitPreconditionUtils.getLockFromBuildParam(paramName);
    assertNotNull(lock);
    assertEquals(LockType.WRITE, lock.getType());
    assertEquals(lockName, lock.getName());
  }

  @Test
  public void testGetLockFromBuildParam_invalid() throws Exception {
    { // invalid format
      String paramName = TestUtils.generateRandomSystemParam();
      Lock lock = WaitPreconditionUtils.getLockFromBuildParam(paramName);
      assertNull(lock);
    }

    { // invalid type
      String paramName = SharedResourcesPluginConstants.LOCK_PREFIX + "someInvalidLockType";
      Lock lock = WaitPreconditionUtils.getLockFromBuildParam(paramName);
      assertNull(lock);
    }

    { // no name specified
      String paramName = SharedResourcesPluginConstants.LOCK_PREFIX + "readLock";
      Lock lock = WaitPreconditionUtils.getLockFromBuildParam(paramName);
      assertNull(lock);
    }

    { // invalid name (empty string) specified
      String paramName = SharedResourcesPluginConstants.LOCK_PREFIX + "writeLock.";
      Lock lock = WaitPreconditionUtils.getLockFromBuildParam(paramName);
      assertNull(lock);
    }
  }

  @Test
  public void testExtractLocksFromPromotion() throws Exception {
    final Map<String, String> params = new HashMap<String, String>();
    params.put(TestUtils.generateLockAsParam(LockType.READ, "read1"), "");
    params.put(TestUtils.generateLockAsParam(LockType.READ, "read2"), "");
    params.put(TestUtils.generateLockAsParam(LockType.READ, "read3"), "");
    params.put(TestUtils.generateLockAsParam(LockType.WRITE, "write1"), "");
    params.put(TestUtils.generateLockAsParam(LockType.WRITE, "write2"), "");
    params.put(TestUtils.generateLockAsParam(LockType.WRITE, "write3"), "");
    params.put(TestUtils.generateRandomSystemParam(), "");
    params.put(TestUtils.generateRandomSystemParam(), "");
    params.put(TestUtils.generateRandomSystemParam(), "");
    m.checking(new Expectations() {{
      oneOf(myBuildPromotion).getBuildParameters();
      will(returnValue(params));
    }});
    Collection<Lock> locks = WaitPreconditionUtils.extractLocksFromPromotion(myBuildPromotion);
    assertNotNull(locks);
    assertEquals(6, locks.size());
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

    Collection<BuildPromotionInfo> buildPromotions = WaitPreconditionUtils.getBuildPromotions(myRunningBuilds, myQueuedBuilds);
    assertNotNull(buildPromotions);
    assertEquals(myRunningBuilds.size() + myQueuedBuilds.size(), buildPromotions.size());

  }

}
