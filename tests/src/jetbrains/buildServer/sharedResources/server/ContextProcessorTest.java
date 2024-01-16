

package jetbrains.buildServer.sharedResources.server;

import com.intellij.openapi.util.text.StringUtil;
import java.util.*;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.LockType;
import jetbrains.buildServer.sharedResources.model.resources.CustomResource;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.model.resources.ResourceFactory;
import jetbrains.buildServer.sharedResources.server.feature.Locks;
import jetbrains.buildServer.sharedResources.server.feature.Resources;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeature;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeatures;
import jetbrains.buildServer.sharedResources.server.report.BuildUsedResourcesReport;
import jetbrains.buildServer.sharedResources.server.runtime.LocksStorage;
import jetbrains.buildServer.util.TestFor;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
@TestFor(testForClass = SharedResourcesContextProcessor.class)
public class ContextProcessorTest extends BaseTestCase {
  private final String PROJECT_ID = "PROJECT_ID";
  private Mockery m;
  private SharedResourcesFeatures myFeatures;
  private Locks myLocks;
  private Resources myResources;
  private LocksStorage myLocksStorage;
  private RunningBuildsManager myRunningBuildsManager;
  private SRunningBuild myRunningBuild;
  private BuildTypeEx myBuildType;
  private BuildPromotionEx myBuildPromotion;
  private BuildStartContext myBuildStartContext;
  private BuildUsedResourcesReport myReport;

  /** Class under test */
  private SharedResourcesContextProcessor myProcessor;


  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    m = new Mockery() {{
      setImposteriser(ClassImposteriser.INSTANCE);
    }};
    myFeatures = m.mock(SharedResourcesFeatures.class);
    myLocks = m.mock(Locks.class);
    myResources = m.mock(Resources.class);
    myLocksStorage = m.mock(LocksStorage.class);
    myRunningBuildsManager = m.mock(RunningBuildsManager.class);
    myBuildStartContext = m.mock(BuildStartContext.class);
    myRunningBuild = m.mock(SRunningBuild.class, "my-running-build");
    myBuildType = m.mock(BuildTypeEx.class);
    myBuildPromotion = m.mock(BuildPromotionEx.class, "my-build-promotion");
    myReport = m.mock(BuildUsedResourcesReport.class);
    myProcessor = new SharedResourcesContextProcessor(myFeatures, myLocks, myResources, myLocksStorage, myReport);
    m.checking(createCommonExpectations());
  }

  @Test(enabled = false)
  @TestFor(issues = "TW-29779")
  public void testWriteLockShouldProvideAllResourceValues() {
    final Map<String, Lock> myTakenLocks = new HashMap<>();
    final Lock lock = new Lock("CustomResource", LockType.WRITE);
    myTakenLocks.put(lock.getName(), lock);

    final Map<String, Resource> definedResources = new HashMap<>();
    final CustomResource resource = (CustomResource) ResourceFactory.newCustomResource("CustomResource", PROJECT_ID, "CustomResource", Arrays.asList("value1", "value2"), true);
    definedResources.put(resource.getName(), resource);

    final String lockParamName = "teamcity.locks.writeLock." + lock.getName();

    final SharedResourcesFeature feature = m.mock(SharedResourcesFeature.class);
    final Collection<SharedResourcesFeature> features = Collections.singleton(feature);

    m.checking(new Expectations() {{

      oneOf(myFeatures).searchForFeatures(myBuildType);
      will(returnValue(features));

      oneOf(feature).getLockedResources();
      will(returnValue(myTakenLocks));

      oneOf(myResources).getResourcesMap(PROJECT_ID);
      will(returnValue(definedResources));

      oneOf(myRunningBuildsManager).getRunningBuilds();
      will(returnValue(Collections.emptyList()));

      oneOf(myLocks).asBuildParameter(lock);
      will(returnValue(lockParamName));

      oneOf(myBuildStartContext).addSharedParameter(lockParamName, StringUtil.join(resource.getValues(), ";"));

      allowing(myLocksStorage);
    }});
    myProcessor.updateParameters(myBuildStartContext);
  }

  @Test(enabled = false)
  public void testProvideValueAny() {
    final Map<String, Lock> myTakenLocks = new HashMap<>();
    final Lock lock = new Lock("CustomResource", LockType.READ);
    myTakenLocks.put(lock.getName(), lock);

    final Map<String, Resource> definedResources = new HashMap<>();
    final CustomResource resource = (CustomResource) ResourceFactory.newCustomResource("CustomResource", PROJECT_ID, "CustomResource", Arrays.asList("value1", "value2"), true);
    definedResources.put(resource.getName(), resource);

    final String lockParamName = "teamcity.locks.readLock." + lock.getName();

    final SharedResourcesFeature feature = m.mock(SharedResourcesFeature.class);
    final Collection<SharedResourcesFeature> features = Collections.singleton(feature);

    m.checking(new Expectations() {{
      oneOf(myFeatures).searchForFeatures(myBuildType);
      will(returnValue(features));

      oneOf(feature).getLockedResources();
      will(returnValue(myTakenLocks));

      oneOf(myResources).getResourcesMap(PROJECT_ID);
      will(returnValue(definedResources));

      oneOf(myRunningBuildsManager).getRunningBuilds();
      will(returnValue(Collections.emptyList()));

      oneOf(myLocks).asBuildParameter(lock);
      will(returnValue(lockParamName));

      oneOf(myBuildStartContext).addSharedParameter(lockParamName, resource.getValues().iterator().next());

      allowing(myLocksStorage);
    }});
    myProcessor.updateParameters(myBuildStartContext);
  }

  @Test(enabled = false)
  public void testProvideValueAny_SomeTaken() {
    final Map<String, Lock> myTakenLocks = new HashMap<>();
    final Lock lock = new Lock("CustomResource", LockType.READ);
    myTakenLocks.put(lock.getName(), lock);

    final Map<String, Resource> definedResources = new HashMap<>();
    final CustomResource resource = (CustomResource) ResourceFactory.newCustomResource("CustomResource", PROJECT_ID, "CustomResource", Arrays.asList("value1", "value2"), true);
    definedResources.put(resource.getName(), resource);

    final String lockParamName = "teamcity.locks.readLock." + lock.getName();

    final SRunningBuild otherRunningBuild = m.mock(SRunningBuild.class, "other-running-build");
    final BuildPromotion otherBuildPromotion = m.mock(BuildPromotion.class, "other-build-promotion");
    final Map<String, Lock> otherTakenLocks = new HashMap<>();
    otherTakenLocks.put("CustomResource", new Lock("CustomResource", LockType.READ, "value1"));

    final SharedResourcesFeature currentFeature = m.mock(SharedResourcesFeature.class, "my-build-feature");
    final Collection<SharedResourcesFeature> currentFeatures = Collections.singleton(currentFeature);

    m.checking(new Expectations() {{
      allowing(otherRunningBuild).getBuildPromotion();
      will(returnValue(otherBuildPromotion));

      oneOf(myFeatures).searchForFeatures(myBuildType);
      will(returnValue(currentFeatures));

      oneOf(currentFeature).getLockedResources();
      will(returnValue(myTakenLocks));

      oneOf(myResources).getResourcesMap(PROJECT_ID);
      will(returnValue(definedResources));

      oneOf(myRunningBuildsManager).getRunningBuilds();
      will(returnValue(Collections.singletonList(otherRunningBuild)));

      oneOf(myLocksStorage).load(otherBuildPromotion);
      will(returnValue(otherTakenLocks));

      oneOf(myLocks).asBuildParameter(lock);
      will(returnValue(lockParamName));

      final Map<Lock, String> storedLocks = new HashMap<>();
      storedLocks.put(lock, "value2");
      oneOf(myLocksStorage).store(myBuildPromotion, storedLocks);

      allowing(myBuildPromotion).getId();
      will(returnValue(0L));

      allowing(otherBuildPromotion).getId();
      will(returnValue(1L));

      allowing(myRunningBuild).getBuildId();
      will(returnValue(0L));

      allowing(otherRunningBuild).getBuildId();
      will(returnValue(1L));

      oneOf(myBuildStartContext).addSharedParameter(lockParamName, "value2");
    }});
    myProcessor.updateParameters(myBuildStartContext);
  }

  @Test(enabled = false)
  public void testProvideValueSpecific() {
    final Map<String, Lock> myTakenLocks = new HashMap<>();
    final Lock lock = new Lock("CustomResource", LockType.READ, "value2");
    myTakenLocks.put(lock.getName(), lock);

    final Map<String, Resource> definedResources = new HashMap<>();
    final CustomResource resource = (CustomResource) ResourceFactory.newCustomResource("CustomResource", PROJECT_ID, "CustomResource", Arrays.asList("value1", "value2"), true);
    definedResources.put(resource.getName(), resource);

    final String lockParamName = "teamcity.locks.readLock." + lock.getName();

    final SharedResourcesFeature feature = m.mock(SharedResourcesFeature.class);
    final Collection<SharedResourcesFeature> features = Collections.singleton(feature);

    m.checking(new Expectations() {{
      createCommonExpectations();

      oneOf(myFeatures).searchForFeatures(myBuildType);
      will(returnValue(features));

      oneOf(feature).getLockedResources();
      will(returnValue(myTakenLocks));

      oneOf(myResources).getResourcesMap(PROJECT_ID);
      will(returnValue(definedResources));

      oneOf(myRunningBuildsManager).getRunningBuilds();
      will(returnValue(Collections.emptyList()));

      oneOf(myLocks).asBuildParameter(lock);
      will(returnValue(lockParamName));

      oneOf(myBuildStartContext).addSharedParameter(lockParamName, lock.getValue());

      allowing(myLocksStorage);
    }});
    myProcessor.updateParameters(myBuildStartContext);
  }

  @Test(enabled = false)
  @TestFor(issues = "TW-44929")
  public void testProvideDuplicateValue() {
    final String VALUE = "a";
    final Map<String, Resource> definedResources = new HashMap<>();
    final CustomResource resource = (CustomResource) ResourceFactory.newCustomResource("CustomResource", PROJECT_ID, "CustomResource", Arrays.asList(VALUE, VALUE), true);
    definedResources.put(resource.getName(), resource);

    final Map<String, Lock> takenLocks = new HashMap<>();
    final Lock lock = new Lock("CustomResource", LockType.READ);
    takenLocks.put(lock.getName(), lock);

    final String lockParamName = "teamcity.locks.readLock." + lock.getName();
    final SharedResourcesFeature feature = m.mock(SharedResourcesFeature.class);
    final Collection<SharedResourcesFeature> features = Collections.singleton(feature);

    final SRunningBuild runningBuild = m.mock(SRunningBuild.class, "running-build");
    final BuildPromotionEx runningBuildPromotion = m.mock(BuildPromotionEx.class, "running-build-promotion");
    final Map<String, Lock> runningBuildLocks = new HashMap<>();
    runningBuildLocks.put(resource.getName(), new Lock(resource.getName(), LockType.READ, VALUE));

    m.checking(new Expectations() {{

      oneOf(myFeatures).searchForFeatures(myBuildType);
      will(returnValue(features));

      oneOf(feature).getLockedResources();
      will(returnValue(takenLocks));

      oneOf(myResources).getResourcesMap(PROJECT_ID);
      will(returnValue(definedResources));

      oneOf(myRunningBuildsManager).getRunningBuilds();
      will(returnValue(Collections.singletonList(runningBuild)));

      oneOf(myLocksStorage).load(runningBuildPromotion);
      will(returnValue(runningBuildLocks));

      oneOf(myLocks).asBuildParameter(lock);
      will(returnValue(lockParamName));

      final Map<Lock, String> storedLocks = new HashMap<>();
      storedLocks.put(lock, VALUE);

      oneOf(myLocksStorage).store(myBuildPromotion, storedLocks);

      oneOf(myBuildStartContext).addSharedParameter(lockParamName, VALUE);

      allowing(runningBuild).getBuildPromotion();
      will(returnValue(runningBuildPromotion));

      oneOf(runningBuildPromotion).isCompositeBuild();
      will(returnValue(false));

      allowing(myBuildPromotion).getId();
      will(returnValue(0L));

      allowing(runningBuild).getBuildId();
      will(returnValue(0L));

      allowing(runningBuildPromotion).getId();
      will(returnValue(1L));

      allowing(myLocksStorage);
    }});
    myProcessor.updateParameters(myBuildStartContext);
  }

  /**
   * 2 running builds hold same duplicate value
   * tests that the build about to start gets the different one
   */
  @Test(enabled = false)
  @TestFor(issues = "TW-44929")
  public void testDupValues_CollectLockedResources() {
    final String VALUE_HELD = "a";
    final String VALUE_AVAILABLE = "b";

    final Map<String, Resource> definedResources = new HashMap<>();
    final CustomResource resource = (CustomResource) ResourceFactory.newCustomResource("CustomResource", PROJECT_ID, "CustomResource", Arrays.asList(VALUE_HELD, VALUE_HELD, VALUE_AVAILABLE), true);
    definedResources.put(resource.getName(), resource);

    final Map<String, Lock> takenLocks = new HashMap<>();
    final Lock lock = new Lock("CustomResource", LockType.READ);
    takenLocks.put(lock.getName(), lock);

    final String lockParamName = "teamcity.locks.readLock." + lock.getName();
    final SharedResourcesFeature feature = m.mock(SharedResourcesFeature.class);
    final Collection<SharedResourcesFeature> features = Collections.singleton(feature);

    final SRunningBuild runningBuild1 = m.mock(SRunningBuild.class, "running-build-1");
    final BuildPromotionEx runningBuildPromotion1 = m.mock(BuildPromotionEx.class, "running-build-promotion-1");
    final Map<String, Lock> runningBuildLocks1 = new HashMap<>();
    runningBuildLocks1.put(resource.getName(), new Lock(resource.getName(), LockType.READ, VALUE_HELD));

    final SRunningBuild runningBuild2 = m.mock(SRunningBuild.class, "running-build-2");
    final BuildPromotionEx runningBuildPromotion2 = m.mock(BuildPromotionEx.class, "running-build-promotion-2");
    final Map<String, Lock> runningBuildLocks2 = new HashMap<>();
    runningBuildLocks2.put(resource.getName(), new Lock(resource.getName(), LockType.READ, VALUE_HELD));

    m.checking(new Expectations() {{
      oneOf(myFeatures).searchForFeatures(myBuildType);
      will(returnValue(features));

      oneOf(feature).getLockedResources();
      will(returnValue(takenLocks));

      oneOf(myResources).getResourcesMap(PROJECT_ID);
      will(returnValue(definedResources));

      oneOf(myRunningBuildsManager).getRunningBuilds();
      will(returnValue(Arrays.asList(runningBuild1, runningBuild2)));

      oneOf(myLocksStorage).load(runningBuildPromotion1);
      will(returnValue(runningBuildLocks1));

      oneOf(myLocksStorage).load(runningBuildPromotion2);
      will(returnValue(runningBuildLocks2));

      oneOf(myLocks).asBuildParameter(lock);
      will(returnValue(lockParamName));

      final Map<Lock, String> storedLocks = new HashMap<>();
      storedLocks.put(lock, VALUE_AVAILABLE);
      oneOf(myLocksStorage).store(myBuildPromotion, storedLocks);

      oneOf(myBuildStartContext).addSharedParameter(lockParamName, VALUE_AVAILABLE);

      allowing(myBuildPromotion).getId();
      will(returnValue(0L));

      allowing(runningBuildPromotion1).getId();
      will(returnValue(1L));

      allowing(runningBuildPromotion2).getId();
      will(returnValue(2L));

      allowing(runningBuild1).getBuildId();
      will(returnValue(1L));

      allowing(runningBuild2).getBuildId();
      will(returnValue(2L));

      allowing(runningBuild1).getBuildPromotion();
      will(returnValue(runningBuildPromotion1));

      allowing(runningBuildPromotion1).isCompositeBuild();
      will(returnValue(false));

      allowing(runningBuild2).getBuildPromotion();
      will(returnValue(runningBuildPromotion2));

      allowing(runningBuildPromotion2).isCompositeBuild();
      will(returnValue(false));

      allowing(myLocksStorage);
    }});
    myProcessor.updateParameters(myBuildStartContext);
  }

  private Expectations createCommonExpectations() {
    return new Expectations() {{
      oneOf(myBuildStartContext).getBuild();
      will(returnValue(myRunningBuild));

      allowing(myRunningBuild).getBuildType();
      will(returnValue(myBuildType));

      allowing(myRunningBuild).getProjectId();
      will(returnValue(PROJECT_ID));

      allowing(myRunningBuild).getBuildPromotion();
      will(returnValue(myBuildPromotion));

      allowing(myBuildPromotion).getBuildType();
      will(returnValue(myBuildType));

      allowing(myBuildPromotion).getProjectId();
      will(returnValue(PROJECT_ID));

      allowing(myBuildPromotion).isPartOfBuildChain();
      will(returnValue(false));

      allowing(myBuildPromotion).isCompositeBuild();
      will(returnValue(false));

      allowing(myReport);
    }};
  }
}