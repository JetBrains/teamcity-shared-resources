package jetbrains.buildServer.sharedResources.server.feature;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.serverSide.BuildTypeTemplate;
import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.sharedResources.TestUtils;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.LockType;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.model.resources.ResourceFactory;
import jetbrains.buildServer.util.TestFor;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
@TestFor(testForClass = {SharedResourcesFeature.class, SharedResourcesFeatureImpl.class})
public class SharedResourcesFeatureImplTest extends BaseTestCase {

  private Mockery m;

  private Locks myLocks;

  private Resources myResources;

  private SBuildFeatureDescriptor myBuildFeatureDescriptor;

  private SBuildType myBuildType;

  private BuildTypeTemplate myBuildTypeTemplate;

  private Map<String, String> params;

  private Map<String, Lock> myLockedResources;

  private Map<String, String> expectedBuildParameters;

  private final String myProjectId = "PROJECT_ID";

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    m = new Mockery();
    myLocks = m.mock(Locks.class);
    myResources = m.mock(Resources.class);
    myBuildFeatureDescriptor = m.mock(SBuildFeatureDescriptor.class);
    myBuildType = m.mock(SBuildType.class);
    myBuildTypeTemplate = m.mock(BuildTypeTemplate.class);
    params = new HashMap<String, String>();
    myLockedResources = new HashMap<String, Lock>() {{
      put("lock1", new Lock("lock1", LockType.READ));
      put("lock2", new Lock("lock2", LockType.WRITE));
      put("lock_with_value1", new Lock("lock_with_value1", LockType.WRITE, "lock_value"));
    }};
    expectedBuildParameters = new HashMap<String, String>() {{
      for (String str : myLockedResources.keySet()) {
        put(TestUtils.generateLockAsBuildParam(str, myLockedResources.get(str).getType()), "");
      }
    }};
  }

  @Test
  public void testGetLockedResources() throws Exception {
    m.checking(new Expectations() {{
      oneOf(myLocks).fromFeatureParameters(myBuildFeatureDescriptor);
      will(returnValue(myLockedResources));
    }});

    final SharedResourcesFeature feature = new SharedResourcesFeatureImpl(myLocks, myResources, myBuildFeatureDescriptor);
    Map<String, Lock> lockedResources = feature.getLockedResources();
    assertNotNull(lockedResources);
    assertEquals(myLockedResources.size(), lockedResources.size());
    for (Lock lock : myLockedResources.values()) {
      assertEquals(lock, lockedResources.get(lock.getName()));
    }
    m.assertIsSatisfied();
  }

  @Test
  public void testGetBuildParameters() throws Exception {
    m.checking(new Expectations() {{
      oneOf(myLocks).fromFeatureParameters(myBuildFeatureDescriptor);
      will(returnValue(myLockedResources));

      oneOf(myLocks).asBuildParameters(myLockedResources.values());
      will(returnValue(expectedBuildParameters));
    }});
    final SharedResourcesFeature feature = new SharedResourcesFeatureImpl(myLocks, myResources, myBuildFeatureDescriptor);
    final Map<String, String> params = feature.getBuildParameters();
    assertNotNull(params);
    assertEquals(expectedBuildParameters.size(), params.size());
    m.assertIsSatisfied();
  }

  private final String oldName = "lock2";
  private final String newName = "lock3";

  private void setupCommonExpectations() throws Exception {
    final String newLocksAsString = "lock1 readLock\nlock3 writeLock";
    params.put(FeatureParams.LOCKS_FEATURE_PARAM_KEY, newLocksAsString);

    m.checking(new Expectations() {{
      oneOf(myLocks).fromFeatureParameters(myBuildFeatureDescriptor);
      will(returnValue(myLockedResources));

      oneOf(myLocks).asFeatureParameter(myLockedResources.values());
      will(returnValue(newLocksAsString));

      oneOf(myBuildFeatureDescriptor).getId();
      will(returnValue(""));

      oneOf(myBuildFeatureDescriptor).getType();
      will(returnValue(""));

      oneOf(myBuildFeatureDescriptor).getParameters();
      will(returnValue(new HashMap<String, String>()));

    }});
  }

  @Test
  public void testUpdateLock_BuildType() throws Exception {
    setupCommonExpectations();
    m.checking(new Expectations() {{
      oneOf(myBuildType).updateBuildFeature("", "", params);
      will(returnValue(true));
    }});
    final SharedResourcesFeature feature = new SharedResourcesFeatureImpl(myLocks, myResources, myBuildFeatureDescriptor);
    feature.updateLock(myBuildType, oldName, newName);
    final Map<String, Lock> locks = feature.getLockedResources();
    Lock lock = locks.get(oldName);
    assertNull(lock);
    lock = locks.get(newName);
    assertNotNull(lock);
  }

  @Test
  @TestFor (issues = "TW-26249")
  public void testUpdateLock_Value() throws Exception {
    setupCommonExpectations();
    m.checking(new Expectations() {{
      oneOf(myBuildType).updateBuildFeature("", "", params);
      will(returnValue(true));
    }});
    final SharedResourcesFeature feature = new SharedResourcesFeatureImpl(myLocks, myResources, myBuildFeatureDescriptor);
    feature.updateLock(myBuildType, "lock_with_value1", "lock_with_value2");
    final Map<String, Lock> locks = feature.getLockedResources();
    Lock lock = locks.get("lock_with_value1");
    assertNull(lock);
    lock = locks.get("lock_with_value2");
    assertNotNull(lock);
    assertEquals("lock_value", lock.getValue());
  }

  @Test
  public void testUpdateLock_BuildTypeTemplate() throws Exception {
    setupCommonExpectations();
    m.checking(new Expectations() {{
      oneOf(myBuildType).updateBuildFeature("", "", params);
      will(returnValue(false));

      oneOf(myBuildType).getTemplate();
      will(returnValue(myBuildTypeTemplate));

      oneOf(myBuildFeatureDescriptor).getId();
      will(returnValue(""));

      oneOf(myBuildFeatureDescriptor).getType();
      will(returnValue(""));

      oneOf(myBuildTypeTemplate).updateBuildFeature("", "", params);
      will(returnValue(true));

    }});
    final SharedResourcesFeature feature = new SharedResourcesFeatureImpl(myLocks, myResources, myBuildFeatureDescriptor);
    feature.updateLock(myBuildType, oldName, newName);
    final Map<String, Lock> locks = feature.getLockedResources();
    Lock lock = locks.get(oldName);
    assertNull(lock);
    lock = locks.get(newName);
    assertNotNull(lock);
  }

  @Test
  public void testGetInvalidLocks_Empty() throws Exception {
    m.checking(new Expectations() {{
      oneOf(myLocks).fromFeatureParameters(myBuildFeatureDescriptor);
      will(returnValue(Collections.emptyMap()));
    }});

    final SharedResourcesFeature feature = new SharedResourcesFeatureImpl(myLocks, myResources, myBuildFeatureDescriptor);
    final Map<Lock, String> invalidLocks = feature.getInvalidLocks(myProjectId);
    assertNotNull(invalidLocks);
    assertEmpty(invalidLocks.entrySet());
  }

  @Test
  public void testGetInvalidLocks_AllRight() throws Exception {

    final Map<String, Lock> lockedResources = new HashMap<String, Lock>() {{
      put("lock1", new Lock("lock1", LockType.READ));
      put("lock2", new Lock("lock2", LockType.WRITE));
      put("lock3", new Lock("lock2", LockType.WRITE, "value1"));
    }};

    final Map<String, Resource> resources = new HashMap<String, Resource>() {{
      put("lock1", ResourceFactory.newInfiniteResource("lock1"));
      put("lock2", ResourceFactory.newQuotedResource("lock2", 123));
      put("lock3", ResourceFactory.newCustomResource("lock3", Collections.singletonList("value1")));
    }};

    m.checking(new Expectations() {{
      oneOf(myLocks).fromFeatureParameters(myBuildFeatureDescriptor);
      will(returnValue(lockedResources));

      oneOf(myResources).asMap(myProjectId);
      will(returnValue(resources));
    }});

    final SharedResourcesFeature feature = new SharedResourcesFeatureImpl(myLocks, myResources, myBuildFeatureDescriptor);
    final Map<Lock, String> invalidLocks = feature.getInvalidLocks(myProjectId);
    assertNotNull(invalidLocks);
    assertEmpty(invalidLocks.entrySet());
  }

  @Test
  public void testGetInvalidLocks_NoResource() throws Exception {
    final Lock lock1 = new Lock("lock1", LockType.READ);
    final Map<String, Lock> lockedResources = new HashMap<String, Lock>() {{
      put(lock1.getName(), lock1);
      put("lock2", new Lock("lock2", LockType.WRITE));
      put("lock3", new Lock("lock2", LockType.WRITE, "value1"));
    }};

    final Map<String, Resource> resources = new HashMap<String, Resource>() {{
      put("lock2", ResourceFactory.newQuotedResource("lock2", 123));
      put("lock3", ResourceFactory.newCustomResource("lock3", Collections.singletonList("value1")));
    }};

    m.checking(new Expectations() {{
      oneOf(myLocks).fromFeatureParameters(myBuildFeatureDescriptor);
      will(returnValue(lockedResources));

      oneOf(myResources).asMap(myProjectId);
      will(returnValue(resources));
    }});

    final SharedResourcesFeature feature = new SharedResourcesFeatureImpl(myLocks, myResources, myBuildFeatureDescriptor);
    final Map<Lock, String> invalidLocks = feature.getInvalidLocks(myProjectId);
    assertNotNull(invalidLocks);
    assertNotEmpty(invalidLocks.entrySet());
    assertEquals(1, invalidLocks.size());
    assertContains(invalidLocks.keySet(), lock1);
  }

  @Test
  public void testGetInvalidLocks_WrongType() throws Exception {
    final Lock lock3 = new Lock("lock3", LockType.WRITE, "value1");
    final Map<String, Lock> lockedResources = new HashMap<String, Lock>() {{
      put("lock1", new Lock("lock1", LockType.READ));
      put("lock2", new Lock("lock2", LockType.WRITE));
      put("lock3", lock3);
    }};

    final Map<String, Resource> resources = new HashMap<String, Resource>() {{
      put("lock1", ResourceFactory.newInfiniteResource("lock1"));
      put("lock2", ResourceFactory.newQuotedResource("lock2", 123));
      // wrong type here
      put("lock3", ResourceFactory.newInfiniteResource("lock3"));
    }};

    m.checking(new Expectations() {{
      oneOf(myLocks).fromFeatureParameters(myBuildFeatureDescriptor);
      will(returnValue(lockedResources));

      oneOf(myResources).asMap(myProjectId);
      will(returnValue(resources));
    }});

    final SharedResourcesFeature feature = new SharedResourcesFeatureImpl(myLocks, myResources, myBuildFeatureDescriptor);
    final Map<Lock, String> invalidLocks = feature.getInvalidLocks(myProjectId);
    assertNotNull(invalidLocks);
    assertNotEmpty(invalidLocks.entrySet());
    assertEquals(1, invalidLocks.size());
    assertContains(invalidLocks.keySet(), lock3);
  }

  @Test
  public void testGetInvalidLocks_MissingValue() throws Exception {
    final Lock lock3 = new Lock("lock3", LockType.WRITE, "value1");
    final Map<String, Lock> lockedResources = new HashMap<String, Lock>() {{
      put("lock1", new Lock("lock1", LockType.READ));
      put("lock2", new Lock("lock2", LockType.WRITE));
      put("lock3", lock3);
    }};

    final Map<String, Resource> resources = new HashMap<String, Resource>() {{
      put("lock1", ResourceFactory.newInfiniteResource("lock1"));
      put("lock2", ResourceFactory.newQuotedResource("lock2", 123));
      put("lock3", ResourceFactory.newCustomResource("lock3", Collections.singletonList("value2")));
    }};

    m.checking(new Expectations() {{
      oneOf(myLocks).fromFeatureParameters(myBuildFeatureDescriptor);
      will(returnValue(lockedResources));

      oneOf(myResources).asMap(myProjectId);
      will(returnValue(resources));
    }});

    final SharedResourcesFeature feature = new SharedResourcesFeatureImpl(myLocks, myResources, myBuildFeatureDescriptor);
    final Map<Lock, String> invalidLocks = feature.getInvalidLocks(myProjectId);
    assertNotNull(invalidLocks);
    assertNotEmpty(invalidLocks.entrySet());
    assertEquals(1, invalidLocks.size());
    assertContains(invalidLocks.keySet(), lock3);
  }
}

