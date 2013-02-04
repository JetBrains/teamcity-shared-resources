package jetbrains.buildServer.sharedResources.server.feature;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.serverSide.BuildTypeTemplate;
import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.sharedResources.TestUtils;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.LockType;
import jetbrains.buildServer.util.TestFor;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
@SuppressWarnings("UnusedShould")
@TestFor (testForClass = {SharedResourcesFeature.class , SharedResourcesFeatureImpl.class})
public class SharedResourcesFeatureImplTest extends BaseTestCase {

  private Mockery m;

  private Locks myLocks;

  private SBuildFeatureDescriptor myBuildFeatureDescriptor;

  private SBuildType myBuildType;

  private BuildTypeTemplate myBuildTypeTemplate;

  private final Map<String, Lock> myLockedResources = new HashMap<String, Lock>() {{
    put("lock1", new Lock("lock1", LockType.READ));
    put("lock2", new Lock("lock2", LockType.WRITE));
  }};

  private Map<String, String> expectedBuildParameters = new HashMap<String, String>() {{
    for(String str: myLockedResources.keySet()) {
      put(TestUtils.generateLockAsBuildParam(str, myLockedResources.get(str).getType()), "");
    }
  }};

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    m = new Mockery();
    myLocks = m.mock(Locks.class);
    myBuildFeatureDescriptor = m.mock(SBuildFeatureDescriptor.class);
    myBuildType = m.mock(SBuildType.class);
    myBuildTypeTemplate = m.mock(BuildTypeTemplate.class);
  }

  @Test
  public void testGetLockedResources() {
    m.checking(new Expectations() {{
      oneOf(myLocks).fromFeatureParameters(myBuildFeatureDescriptor);
      will(returnValue(myLockedResources));
    }});

    final SharedResourcesFeature feature = new SharedResourcesFeatureImpl(myLocks, myBuildFeatureDescriptor);
    Map<String, Lock> lockedResources = feature.getLockedResources();
    assertNotNull(lockedResources);
    assertEquals(myLockedResources.size(), lockedResources.size());
    for (Lock lock: myLockedResources.values()) {
      assertEquals(lock, lockedResources.get(lock.getName()));
    }
    m.assertIsSatisfied();
  }

  @Test
  public void testGetBuildParameters() {
    m.checking(new Expectations() {{
      oneOf(myLocks).fromFeatureParameters(myBuildFeatureDescriptor);
      will(returnValue(myLockedResources));

      oneOf(myLocks).asBuildParameters(myLockedResources.values());
      will(returnValue(expectedBuildParameters));
    }});
    final SharedResourcesFeature feature = new SharedResourcesFeatureImpl(myLocks, myBuildFeatureDescriptor);
    final Map<String, String> params = feature.getBuildParameters();
    assertNotNull(params);
    assertEquals(expectedBuildParameters.size(), params.size());
    m.assertIsSatisfied();
  }


  private final String oldName = "lock2";
  private final String newName = "lock3";
  private final Map<String, String> params = new HashMap<String, String>();

  private void setupCommonExpectations() {
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
  public void testUpdateLock_BuildType() {
    setupCommonExpectations();
    m.checking(new Expectations(){{
      oneOf(myBuildType).updateBuildFeature("", "", params);
      will(returnValue(true));
    }});
    final SharedResourcesFeature feature = new SharedResourcesFeatureImpl(myLocks, myBuildFeatureDescriptor);
    feature.updateLock(myBuildType, oldName, newName);
  }

  @Test
  public void testUpdateLock_BuildTypeTemplate() {
    setupCommonExpectations();
    m.checking(new Expectations(){{
      oneOf(myBuildType).updateBuildFeature("", "", params);
      will(returnValue(false));

      oneOf(myBuildType).getTemplate();
      will(returnValue(myBuildTypeTemplate));

      oneOf(myBuildTypeTemplate).updateBuildFeature("", "", params);
      will(returnValue(true));

    }});
    final SharedResourcesFeature feature = new SharedResourcesFeatureImpl(myLocks, myBuildFeatureDescriptor);
    feature.updateLock(myBuildType, oldName, newName);
  }



}
