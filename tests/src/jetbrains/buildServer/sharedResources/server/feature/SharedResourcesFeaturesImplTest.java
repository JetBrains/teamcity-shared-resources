package jetbrains.buildServer.sharedResources.server.feature;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.serverSide.ResolvedSettings;
import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.sharedResources.server.SharedResourcesBuildFeature;
import jetbrains.buildServer.util.TestFor;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Class {@code SharedResourcesFeaturesImplTest}
 *
 * Contains tests for SharedResourcesFeaturesImpl
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
@TestFor (testForClass = {SharedResourcesFeatures.class, SharedResourcesFeaturesImpl.class})
public class SharedResourcesFeaturesImplTest extends BaseTestCase {

  /** Number of test samples for collections */
  private final int NUM = 10;

  /** Factory for mocks */
  private Mockery m;

  /** Mocked build type */
  private SBuildType myBuildType;

  /** Descriptors, that represent {@code SharedResourcesBuildFeature} */
  private List<SBuildFeatureDescriptor> myValidDescriptors;

  /** Descriptors that represent other features*/
  private List<SBuildFeatureDescriptor> myInvalidDescriptors;

  /** Contains all feature descriptors for buildType */
  private List<SBuildFeatureDescriptor> myAllFeatureDescriptors;

  /** Class under test */
  private SharedResourcesFeatures mySharedResourcesFeatures;

  /** Factory for shared resources features */
  private SharedResourcesFeatureFactory mySharedResourcesFeatureFactory;

  /** Mock for feature */
  private SharedResourcesFeature myFeature;

  /** Resolved setting of */
  private ResolvedSettings myResolvedSettings;

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    m = new Mockery();

    myBuildType = m.mock(SBuildType.class, "my-build-type");

    myInvalidDescriptors = new ArrayList<SBuildFeatureDescriptor>(NUM);
    myValidDescriptors = new ArrayList<SBuildFeatureDescriptor>(NUM);

    for (int i = 0; i < NUM; i++) {
      myInvalidDescriptors.add(m.mock(SBuildFeatureDescriptor.class, "invalid-descriptor-" + i));
      myValidDescriptors.add(m.mock(SBuildFeatureDescriptor.class, "valid-descriptor-" + i));
    }

    myAllFeatureDescriptors = new ArrayList<SBuildFeatureDescriptor>();
    myAllFeatureDescriptors.addAll(myInvalidDescriptors);
    myAllFeatureDescriptors.addAll(myValidDescriptors);

    myResolvedSettings = m.mock(ResolvedSettings.class);
    mySharedResourcesFeatureFactory = m.mock(SharedResourcesFeatureFactory.class);

    mySharedResourcesFeatures = new SharedResourcesFeaturesImpl(mySharedResourcesFeatureFactory);
    myFeature = m.mock(SharedResourcesFeature.class);
  }


  @Test
  public void testSearchForFeatures() {
    m.checking(new Expectations() {{

      oneOf(myBuildType).getBuildFeatures();
      will(returnValue(myAllFeatureDescriptors));

      atLeast(1).of(myBuildType).isEnabled(with(any(String.class)));
      will(returnValue(true));

      for (int i=0; i<myAllFeatureDescriptors.size(); i++) {
        allowing(myAllFeatureDescriptors.get(i)).getId();
        will(returnValue(String.valueOf(i)));
      }

      for (int i=0; i<NUM; i++) {
        oneOf(myValidDescriptors.get(i)).getType();
        will(returnValue(SharedResourcesBuildFeature.FEATURE_TYPE));

        oneOf(myInvalidDescriptors.get(i)).getType();
        will(returnValue("$$some_other_type$$"));
      }

      for (int i = 0; i < NUM; i++) {
        oneOf(mySharedResourcesFeatureFactory).createFeature(myValidDescriptors.get(i));
        will(returnValue(myFeature));
      }
    }});

    final Collection<SharedResourcesFeature> myFeatures = mySharedResourcesFeatures.searchForFeatures(myBuildType);
    assertNotNull(myFeatures);
    assertNotEmpty(myFeatures);
    assertEquals(myValidDescriptors.size(), myFeatures.size());
    m.assertIsSatisfied();
  }

  @Test
  public void testSearchForResolvedFeatures() {
    m.checking(new Expectations() {{
      never(myBuildType).getBuildFeatures();

      never(myBuildType).isEnabled(with(any(String.class)));

      oneOf(myBuildType).getResolvedSettings();
      will(returnValue(myResolvedSettings));

      oneOf(myResolvedSettings).getBuildFeatures();
      will(returnValue(myValidDescriptors));

      for (int i = 0; i < NUM; i++) {
        oneOf(myValidDescriptors.get(i)).getType();
        will(returnValue(SharedResourcesBuildFeature.FEATURE_TYPE));
      }

      for (int i = 0; i < NUM; i++) {
          oneOf(mySharedResourcesFeatureFactory).createFeature(myValidDescriptors.get(i));
          will(returnValue(myFeature));
        }
    }});

    final Collection<SharedResourcesFeature> myFeatures = mySharedResourcesFeatures.searchForResolvedFeatures(myBuildType);
    assertNotNull(myFeatures);
    assertNotEmpty(myFeatures);
    assertEquals(NUM, myFeatures.size());
    m.assertIsSatisfied();
  }

  @Test
  public void testFeaturesPresent() {

    m.checking(new Expectations() {{
      oneOf(myBuildType).getBuildFeatures();
      will(returnValue(myAllFeatureDescriptors));

      atLeast(1).of(myBuildType).isEnabled(with(any(String.class)));
      will(returnValue(true));

      for (int i=0; i<myAllFeatureDescriptors.size(); i++) {
        allowing(myAllFeatureDescriptors.get(i)).getId();
        will(returnValue(String.valueOf(i)));
      }

      for (int i=0; i<NUM; i++) {
        oneOf(myValidDescriptors.get(i)).getType();
      will(returnValue(SharedResourcesBuildFeature.FEATURE_TYPE));

        oneOf(myInvalidDescriptors.get(i)).getType();
        will(returnValue("$$some_other_type$$"));
      }
    }});

    assertTrue(mySharedResourcesFeatures.featuresPresent(myBuildType));
  }

  @Test
  public void testFeaturesPresentNull() throws Exception {
    assertFalse(mySharedResourcesFeatures.featuresPresent(null));
  }
}
