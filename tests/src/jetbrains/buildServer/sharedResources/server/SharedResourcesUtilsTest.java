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

import com.intellij.openapi.util.text.StringUtil;
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
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.util.TestFor;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.*;

import static jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants.LOCK_PREFIX;
import static jetbrains.buildServer.sharedResources.TestUtils.*;

/**
 * Class {@code SharedResourcesUtilsTest}
 *
 * Contains tests for SharedResourcesUtils
 *
 * @see SharedResourcesUtils
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
@TestFor(testForClass = SharedResourcesUtils.class)
public class SharedResourcesUtilsTest extends BaseTestCase {

  /** Mocks provider */
  private Mockery m;

  /** Build promotion mock */
  private BuildPromotionEx myBuildPromotion;

  /** Parameters provider mock */
  private ParametersProvider myParametersProvider;

  /**
   * Collection of mocked queued builds
   */
  private Collection<QueuedBuildInfo> myQueuedBuilds;

  /**
   * Collection of mocked running builds
   */
  private Collection<RunningBuildInfo> myRunningBuilds;


  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    m = new Mockery();
    myBuildPromotion = m.mock(BuildPromotionEx.class);
    myQueuedBuilds = new ArrayList<QueuedBuildInfo>();
    myRunningBuilds = new ArrayList<RunningBuildInfo>();
    myParametersProvider = m.mock(ParametersProvider.class);

    {
      int numBuilds = generateBoundedRandomInt();
      for (int i = 0; i < numBuilds; i++) {
        myQueuedBuilds.add(m.mock(QueuedBuildInfo.class, "queuedBuild" + i));
      }
    }

    {
      int numBuilds = generateBoundedRandomInt();
      for (int i = 0; i < numBuilds; i++) {
        myRunningBuilds.add(m.mock(RunningBuildInfo.class, "runningBuild" + i));
      }
    }
  }



  /**
   * Tests {@code featureParamToBuildParams(String)} method on
   * {@code null} and empty inputs
   *
   * @throws Exception if something goes wrong
   */
  @Test //todo: fix test
  public void testFeatureParamToBuildParams_NullAndEmpty() throws Exception {
//    { // null input
//      final Map<String, String> result = SharedResourcesUtils.featureParamToBuildParams(null);
//      assertNotNull("Expected empty map on null input, received null", result);
//      assertTrue("Expected empty map on null input, got [" + result.toString() + "]", result.isEmpty());
//    }
//
//    { // empty input
//      final Map<String, String> result = SharedResourcesUtils.featureParamToBuildParams("");
//      assertNotNull("Expected empty map on empty input, received null", result);
//      assertTrue("Expected empty map on empty input, got [" + result.toString() + "]", result.isEmpty());
//    }
  }

  /**
   * @throws Exception if something goes wrong
   */
  @Test // todo: fix test
  public void testFeatureParamToBuildParams_Valid() throws Exception {
//    {
//      int num = generateBoundedRandomInt();
//      final List<String> serializedParams = new ArrayList<String>(num);
//      for (int i = 0; i < num; i++) {
//        serializedParams.add(TestUtils.generateSerializedLock());
//      }
//      final String paramsAsString = StringUtil.join(serializedParams, "\n");
//      final Map<String, String> buildParams = SharedResourcesUtils.featureParamToBuildParams(paramsAsString);
//      assertNotNull(buildParams);
//      assertFalse("Expected not empty map for input of size [" + num + "]", buildParams.isEmpty());
//      assertEquals("Expected that all params [" + num + "] are parsed correctly. Resulting map size is [" + buildParams.size() + "]", num, buildParams.size());
//    }
  }

  /**
   * Tests conversion of lock to build parameter name
   *
   * @see SharedResourcesPluginConstants#LOCK_PREFIX
   * @throws Exception if something goes wrong
   */
  @Test //todo: fix test
  public void testLockAsBuildParam() throws Exception {
//    for (LockType type: LockType.values()) {
//      final String name = generateRandomName();
//      final Lock lock = new Lock(name, type);
//      final String result = SharedResourcesUtils.lockAsBuildParam(lock);
//      assertNotNull(result);
//      assertEquals(LOCK_PREFIX + type + "." + name, result);
//    }
  }

  /**
   * Tests {@code getLockNames} method on null and empty inputs
   *
   * @see SharedResourcesUtils#getLockNames(String)
   * @throws Exception if something goes wrong
   */
  @Test
  public void testGetLockNames_NullAndEmpty() throws Exception {
    { // null case
      final List<List<String>> result = SharedResourcesUtils.getLockNames(null);
      assertNotNull(result);
      assertNotEmpty(result);
      assertEquals(2, result.size());
      assertNotNull(result.get(0));
      assertEmpty(result.get(0));
      assertNotNull(result.get(1));
      assertEmpty(result.get(1));
    }

    { // empty case
      final List<List<String>> result = SharedResourcesUtils.getLockNames("");
      assertNotNull(result);
      assertNotEmpty(result);
      assertEquals(2, result.size());
      assertNotNull(result.get(0));
      assertEmpty(result.get(0));
      assertNotNull(result.get(1));
      assertEmpty(result.get(1));
    }
  }


  /**
   * Tests {@code getLockNames} method on random valid inputs
   *
   * @see SharedResourcesUtils#getLockNames(String)
   * @throws Exception if something goes wrong
   */
  @Test
  public void testGetLockNames_Valid() throws Exception {
    final int readLocksNum = generateBoundedRandomInt();
    final int writeLocksNum = generateBoundedRandomInt();
    final List<String> serializedReadLocks = new ArrayList<String>(readLocksNum);
    final List<String> serializedWriteLocks = new ArrayList<String>(writeLocksNum);
    for (int i = 0; i < readLocksNum; i++) {
      serializedReadLocks.add(TestUtils.generateSerializedLock(LockType.READ));
    }

    for (int i = 0; i < writeLocksNum; i++) {
      serializedWriteLocks.add(TestUtils.generateSerializedLock(LockType.WRITE));
    }
    final Collection<String> allLocks = new ArrayList<String>();
    allLocks.addAll(serializedReadLocks);
    allLocks.addAll(serializedWriteLocks);

    final List<List<String>> result = SharedResourcesUtils.getLockNames(StringUtil.join(allLocks, "\n"));
    assertNotNull(result);
    assertNotEmpty(result);
    assertEquals(2, result.size());
    assertNotNull(result.get(0));
    assertNotEmpty(result.get(0));
    assertEquals(readLocksNum, result.get(0).size());
    assertNotNull(result.get(1));
    assertNotEmpty(result.get(1));
    assertEquals(writeLocksNum, result.get(1).size());
  }

  /**
   * Tests {@code getLockFromBuildParam} method on invalid inputs
   *
   * @see SharedResourcesUtils#getLockFromBuildParam(String)
   * @throws Exception if something goes wrong
   */
  @Test
  public void testGetLockFromBuildParam_Invalid() throws Exception {
    { // invalid format
      String paramName = generateRandomConfigurationParam();
      Lock lock = SharedResourcesUtils.getLockFromBuildParam(paramName);
      assertNull(lock);
    }

    { // invalid type
      String paramName = LOCK_PREFIX + "someInvalidLockType";
      Lock lock = SharedResourcesUtils.getLockFromBuildParam(paramName);
      assertNull(lock);
    }

    { // no name specified
      String paramName = LOCK_PREFIX + "readLock";
      Lock lock = SharedResourcesUtils.getLockFromBuildParam(paramName);
      assertNull(lock);
    }

    { // invalid name (empty string) specified
      String paramName = LOCK_PREFIX + "writeLock.";
      Lock lock = SharedResourcesUtils.getLockFromBuildParam(paramName);
      assertNull(lock);
    }
  }


  /**
   * Tests {@code getLockFromBuildParam} method on valid inputs
   *
   * @see SharedResourcesUtils#getLockFromBuildParam(String)
   * @throws Exception if something goes wrong
   */
  @Test
  public void testGetLockFromBuildParam_Valid() throws Exception {
    final String lockName = generateRandomName();
    {
      final String paramName = TestUtils.generateLockAsParam(lockName, LockType.READ);
      final Lock lock = SharedResourcesUtils.getLockFromBuildParam(paramName);
      assertNotNull(lock);
      assertEquals(LockType.READ, lock.getType());
    }

    {
      final String paramName = TestUtils.generateLockAsParam(lockName, LockType.WRITE);
      final Lock lock = SharedResourcesUtils.getLockFromBuildParam(paramName);
      assertNotNull(lock);
      assertEquals(LockType.WRITE, lock.getType());
      assertEquals(lockName, lock.getName());
    }
  }


  /**
   * @see SharedResourcesUtils#extractLocksFromPromotion(jetbrains.buildServer.serverSide.buildDistribution.BuildPromotionInfo)
   * @throws Exception if something goes wrong
   */
  @Test
  public void testExtractLocksFromPromotion() throws Exception {
    final Map<String, String> params = new HashMap<String, String>();
    final int numReadLocks = generateBoundedRandomInt();
    final int numWriteLocks = generateBoundedRandomInt();
    final int numOtherParams = generateBoundedRandomInt();
    for (int i = 0; i < numReadLocks; i++) {
      params.put(TestUtils.generateLockAsParam("read" + i, LockType.READ), "");
    }
    for (int i = 0; i < numWriteLocks; i++) {
      params.put(TestUtils.generateLockAsParam("write" + i, LockType.WRITE), "");
    }
    for (int i = 0; i < numOtherParams; i++) {
      params.put(generateRandomConfigurationParam(), "");
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
    m.assertIsSatisfied();
  }


  /**
   * @see SharedResourcesUtils#getBuildPromotions(java.util.Collection, java.util.Collection)
   * @throws Exception
   */
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
    m.assertIsSatisfied();
  }

  /**
   * @see SharedResourcesUtils#getUnavailableLocks(java.util.Collection, java.util.Collection, java.util.Map)
   * @throws Exception if something goes wrong
   */
  @Test
  public void testGetUnavailableLocks_NoResources() throws Exception {
    // taken locks
    final List<Map<String, String>> paramsList = new ArrayList<Map<String, String>>();
    paramsList.add(new HashMap<String, String>() {{
      put(TestUtils.generateLockAsParam("lock1", LockType.READ), "");
      put(TestUtils.generateLockAsParam("lock2", LockType.WRITE), "");
    }});
    paramsList.add(new HashMap<String, String>() {{
      put(TestUtils.generateLockAsParam("lock1", LockType.WRITE), "");
      put(TestUtils.generateLockAsParam("lock2", LockType.READ), "");
    }});

    final List<Lock> locksToTake = new ArrayList<Lock>() {{
      add(new Lock("lock1", LockType.WRITE));
      add(new Lock("lock2", LockType.READ));
    }};

    final List<BuildPromotionInfo> promotions = new ArrayList<BuildPromotionInfo>();
    final List<ParametersProvider> providers = new ArrayList<ParametersProvider>();

    for (int i = 0; i < 2; i++) {
      promotions.add(m.mock(BuildPromotionEx.class, "promo-" + i));
      providers.add(m.mock(ParametersProvider.class, "pp-" + i));
    }

    m.checking(new Expectations() {{
      for (int i = 0; i < 2; i++) {
        oneOf(((BuildPromotionEx)promotions.get(i))).getParametersProvider();
        will(returnValue(providers.get(i)));

        oneOf(providers.get(i)).getAll();
        will(returnValue(paramsList.get(i)));

      }

    }});

    // todo: add resources to test
    final Collection<Lock> unavailableLocks = SharedResourcesUtils.getUnavailableLocks(locksToTake, promotions, Collections.<String, Resource>emptyMap());
    assertNotNull(unavailableLocks);
    assertNotEmpty(unavailableLocks);
    assertEquals(2, unavailableLocks.size());
    m.assertIsSatisfied();
  }

  /**
   * @see SharedResourcesUtils#getUnavailableLocks(java.util.Collection, java.util.Collection, java.util.Map)
   * @throws Exception if something goes wrong
   */
  @Test
  public void testGetUnavailableLocks_WithResources() throws Exception  {

    { // against infinite resource

    }

    { // against finite resource

    }

  }




  /**
   * @see SharedResourcesUtils#extractLocksFromParams(java.util.Map)
   * @throws Exception if something goes wrong
   */
  @Test
  public void testExtractLocksFromParams() throws Exception {
    final String lockName = generateRandomName();
    final String validBuildParamRead = SharedResourcesPluginConstants.LOCK_PREFIX + "readLock." + lockName;
    final String validBuildParamWrite = SharedResourcesPluginConstants.LOCK_PREFIX + "writeLock." + lockName;
    final String invalidLockBuildParam = generateRandomConfigurationParam();

    final Map<String, String> buildParams = new HashMap<String, String>() {{
       put(validBuildParamRead, "");
       put(validBuildParamWrite, "");
       put(invalidLockBuildParam, "");
    }};

    final Collection<Lock> locks = SharedResourcesUtils.extractLocksFromParams(buildParams);
    assertNotNull(locks);
    assertNotEmpty(locks);
    assertContains(locks, new Lock(lockName, LockType.READ));
    assertContains(locks, new Lock(lockName, LockType.WRITE));
  }

  @Test
  public void testLocksAsString() throws Exception {
    final String expected = "lock1 readLock\nlock2 writeLock";
    final Collection<Lock> locks = new ArrayList<Lock>() {{
      add(new Lock("lock1", LockType.READ));
      add(new Lock("lock2", LockType.WRITE));
    }};
    final String result = SharedResourcesUtils.locksAsString(locks);
    assertEquals(expected, result);
  }
}
