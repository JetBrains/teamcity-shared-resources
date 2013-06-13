package jetbrains.buildServer.sharedResources.server.feature;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.parameters.ParametersProvider;
import jetbrains.buildServer.serverSide.BuildPromotionEx;
import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor;
import jetbrains.buildServer.sharedResources.TestUtils;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.LockType;
import jetbrains.buildServer.util.TestFor;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.*;

import static jetbrains.buildServer.sharedResources.server.feature.FeatureParams.LOCKS_FEATURE_PARAM_KEY;

/**
 * Class {@code LocksImplTest}
 *
 * Contains tests for {@code LocksImpl} implementation of {@code Locks}
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
@TestFor(testForClass = {Locks.class, LocksImpl.class})
public class LocksImplTest extends BaseTestCase {

  /** Class under test */
  private Locks myLocks;

  private Mockery m;

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myLocks = new LocksImpl();
    m = new Mockery();
  }

  @Test
  public void testFromFeatureParameters_Empty() throws Exception {
    final Map<String, String> params = new HashMap<String, String>();
    { // pure params
      final Map<String, Lock> result = myLocks.fromFeatureParameters(params);
      assertNotNull(result);
      assertEquals(0, result.size());
    }

    { // SBuildFeatureDescriptor
      final SBuildFeatureDescriptor descriptor = m.mock(SBuildFeatureDescriptor.class);
      m.checking(new Expectations() {{
        oneOf(descriptor).getParameters();
        will(returnValue(params));
      }});
      final Map<String, Lock> result = myLocks.fromFeatureParameters(descriptor);
      assertNotNull(result);
      assertEquals(0, result.size());
    }
  }


  @Test
  public void testFromFeatureParameters_NoLocks() throws Exception {
    final Map<String, String> params = new HashMap<String, String>() {{
      for (int i = 0; i < TestUtils.RANDOM_UPPER_BOUNDARY; i++) {
        put(TestUtils.generateRandomName(), TestUtils.generateRandomName());
      }
    }};

    {
      final Map<String, Lock> result = myLocks.fromFeatureParameters(params);
      assertNotNull(result);
      assertEquals(0, result.size());
    }

    {
      final SBuildFeatureDescriptor descriptor = m.mock(SBuildFeatureDescriptor.class);
      m.checking(new Expectations() {{
        oneOf(descriptor).getParameters();
        will(returnValue(params));
      }});
      final Map<String, Lock> result = myLocks.fromFeatureParameters(descriptor);
      assertNotNull(result);
      assertEquals(0, result.size());
    }
  }

  @Test
  public void testFromFeatureParameters_SomeLocks() throws Exception {
    final Map<String, String> params = new HashMap<String, String>() {{
      for (int i = 0; i < TestUtils.RANDOM_UPPER_BOUNDARY; i++) {
        put(TestUtils.generateRandomName(), TestUtils.generateRandomName());
      }
    }};
    params.put(LOCKS_FEATURE_PARAM_KEY, "lock1 readLock\nlock2 writeLock\n");

    {
      final Map<String, Lock> result = myLocks.fromFeatureParameters(params);
      assertNotNull(result);
      assertEquals(2, result.size());
    }

    {
      final SBuildFeatureDescriptor descriptor = m.mock(SBuildFeatureDescriptor.class);
      m.checking(new Expectations() {{
        oneOf(descriptor).getParameters();
        will(returnValue(params));
      }});
      final Map<String, Lock> result = myLocks.fromFeatureParameters(descriptor);
      assertNotNull(result);
      assertEquals(2, result.size());
    }
  }

  @Test
  public void testFromFeatureParameters_LocksTricky() throws Exception {
    final Map<String, String> params = new HashMap<String, String>() {{
      for (int i = 0; i < TestUtils.RANDOM_UPPER_BOUNDARY; i++) {
        put(TestUtils.generateRandomName(), TestUtils.generateRandomName());
      }
    }};
    params.put(LOCKS_FEATURE_PARAM_KEY, "lock 1 readLock\nlock 2 writeLock\nlock 3 readLock value 1 2 3\nlock 4 wrongType qwerty");

    {
      final Map<String, Lock> result = myLocks.fromFeatureParameters(params);
      assertNotNull(result);
      assertEquals(3, result.size());
      final Lock lock = result.get("lock 3");
      assertNotNull(lock);
      assertEquals(LockType.READ, lock.getType());
      assertEquals("value 1 2 3", lock.getValue());
    }

    {
      final SBuildFeatureDescriptor descriptor = m.mock(SBuildFeatureDescriptor.class);
      m.checking(new Expectations() {{
        oneOf(descriptor).getParameters();
        will(returnValue(params));
      }});
      final Map<String, Lock> result = myLocks.fromFeatureParameters(descriptor);
      assertNotNull(result);
      assertEquals(3, result.size());
    }

  }

  @Test
  public void testAsFeatureParameter() throws Exception {
    { // empty collection of locks
      final String str = myLocks.asFeatureParameter(Collections.<Lock>emptyList());
      assertNotNull(str);
      assertEquals(0, str.length());
    }
    {
      int N = TestUtils.RANDOM_UPPER_BOUNDARY;
      final Collection<Lock> locks = new ArrayList<Lock>(N);
      for (int i = 0; i < N; i++) {
        locks.add(TestUtils.generateRandomLock());
      }

      final String str = myLocks.asFeatureParameter(locks);
      assertNotNull(str);
      assertTrue(str.length() > 0);
      for (Lock lock: locks) {
        assertTrue(str.contains(lock.getName()));
      }
    }
  }

  @Test
  public void testAsBuildParameters() throws Exception {
    int N = TestUtils.RANDOM_UPPER_BOUNDARY;
    final Collection<Lock> locks = new ArrayList<Lock>(N);
    for (int i = 0; i < N; i++) {
      locks.add(TestUtils.generateRandomLock());
    }
    final Map<String, String> buildParams = myLocks.asBuildParameters(locks);
    assertNotNull(buildParams);
    assertEquals(N, buildParams.size());
    for (Lock lock: locks) {
      assertContains(buildParams.keySet(), TestUtils.generateLockAsBuildParam(lock.getName(), lock.getType()));
    }
  }

  @Test
  public void testFromBuildPromotion() throws Exception {
    final BuildPromotionEx promo = m.mock(BuildPromotionEx.class);
    final ParametersProvider pp = m.mock(ParametersProvider.class);

    final Map<String, String> buildParams = new HashMap<String, String>();
    final Collection<Lock> expectedLocks = new ArrayList<Lock>();

    int N = TestUtils.RANDOM_UPPER_BOUNDARY;
    for (int i = 0; i < N * 2; i++) {
      expectedLocks.add(TestUtils.generateRandomLock());
    }
    for (Lock lock: expectedLocks) {
      buildParams.put(TestUtils.generateLockAsBuildParam(lock.getName(), lock.getType()), "");
    }

    m.checking(new Expectations() {{
      atMost(2).of(promo).getParametersProvider();
      will(returnValue(pp));

      atMost(2).of(pp).getAll();
      will(returnValue(buildParams));
    }});

    {
      final Collection<Lock> locks = myLocks.fromBuildPromotion(promo);
      assertNotNull(locks);
      assertNotEmpty(locks);
      assertEquals(N * 2, locks.size());
      for (Lock lock: expectedLocks) {
        assertContains(locks, lock);
      }
    }

    {
      final Map<String, Lock> locks = myLocks.fromBuildPromotionAsMap(promo);
      assertNotNull(locks);
      assertEquals(N * 2, locks.size());
      for (Lock lock: expectedLocks) {
        assertContains(locks.values(), lock);
      }
    }
    m.assertIsSatisfied();
  }

  @Test
  public void testAsBuildParam() throws Exception {
    final Lock lock1 = new Lock("lock1", LockType.READ);
    final Lock lock2 = new Lock("lock2", LockType.WRITE);
    final String param1 = myLocks.asBuildParameter(lock1);
    final String param2 = myLocks.asBuildParameter(lock2);
    assertEquals("teamcity.locks.readLock.lock1", param1);
    assertEquals("teamcity.locks.writeLock.lock2", param2);
  }

  @Test
  public void testFromFeatureParams_Values() throws Exception {
    final Map<String, String> params = new HashMap<String, String>() {{
      for (int i = 0; i < TestUtils.RANDOM_UPPER_BOUNDARY; i++) {
        put(TestUtils.generateRandomName(), TestUtils.generateRandomName());
      }
    }};
    params.put(LOCKS_FEATURE_PARAM_KEY, "lock3 writeLock VAL3\nlock1 readLock\nlock2 writeLock VAL2\n");

    {
      final SBuildFeatureDescriptor descriptor = m.mock(SBuildFeatureDescriptor.class);
      m.checking(new Expectations() {{
        oneOf(descriptor).getParameters();
        will(returnValue(params));
      }});
      final Map<String, Lock> result = myLocks.fromFeatureParameters(descriptor);
      assertNotNull(result);
      assertEquals(3, result.size());
      Lock lock = result.get("lock3");
      assertNotNull(lock);
      assertEquals("VAL3", lock.getValue());
      lock = result.get("lock2");
      assertNotNull(lock);
      assertEquals("VAL2", lock.getValue());
    }
  }

  @Test
  public void testToFeatureParams_Values() throws Exception {
    int N = TestUtils.RANDOM_UPPER_BOUNDARY;
    final Collection<Lock> locks = new ArrayList<Lock>(N);
    for (int i = 0; i < N; i++) {
      locks.add(TestUtils.generateRandomLockWithValue());
    }

    final String str = myLocks.asFeatureParameter(locks);
    assertNotNull(str);
    assertTrue(str.length() > 0);
    for (Lock lock: locks) {
      assertTrue(str.contains(lock.getName()));
      assertTrue(str.contains(lock.getValue()));
    }
  }

  @Test
  public void testAsBuildParams_Values() throws Exception {
    int N = TestUtils.RANDOM_UPPER_BOUNDARY;
    final Collection<Lock> locks = new ArrayList<Lock>(N);
    for (int i = 0; i < N; i++) {
      locks.add(TestUtils.generateRandomLockWithValue());
    }
    final Map<String, String> buildParams = myLocks.asBuildParameters(locks);
    assertNotNull(buildParams);
    assertEquals(N, buildParams.size());
    for (Lock lock: locks) {
      String lockName = TestUtils.generateLockAsBuildParam(lock.getName(), lock.getType());
      String val = buildParams.get(lockName);
      assertNotNull(val);
      assertEquals(lock.getValue(), val);
    }
  }

  @Test
  public void testFromBuildParams_Values() throws Exception {
    final BuildPromotionEx promo = m.mock(BuildPromotionEx.class);
    final ParametersProvider pp = m.mock(ParametersProvider.class);

    final Map<String, String> buildParams = new HashMap<String, String>();
    final Collection<Lock> expectedLocks = new ArrayList<Lock>();

    int N = TestUtils.RANDOM_UPPER_BOUNDARY;
    for (int i = 0; i < N * 2; i++) {
      expectedLocks.add(TestUtils.generateRandomLockWithValue());
    }
    for (Lock lock: expectedLocks) {
      buildParams.put(TestUtils.generateLockAsBuildParam(lock.getName(), lock.getType()), lock.getValue());
    }

    m.checking(new Expectations() {{
      atMost(2).of(promo).getParametersProvider();
      will(returnValue(pp));

      atMost(2).of(pp).getAll();
      will(returnValue(buildParams));
    }});

    {
      final Map<String, Lock> locks = myLocks.fromBuildPromotionAsMap(promo);
      assertNotNull(locks);
      assertEquals(N * 2, locks.size());
      for (Lock lock: expectedLocks) {
        Lock l = locks.get(lock.getName());
        assertNotNull(l);
        assertEquals(lock, l);
      }
    }
  }
}
