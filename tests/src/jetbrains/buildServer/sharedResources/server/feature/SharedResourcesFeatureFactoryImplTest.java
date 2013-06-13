package jetbrains.buildServer.sharedResources.server.feature;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.util.TestFor;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
@TestFor(testForClass = {SharedResourcesFeatureFactory.class, SharedResourcesFeatureFactoryImpl.class})
public class SharedResourcesFeatureFactoryImplTest extends BaseTestCase {

  private Mockery m;

  private Locks myLocks;

  private Resources myResources;

  private SBuildFeatureDescriptor myBuildFeatureDescriptor;

  private SharedResourcesFeatureFactory myFactory;


  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    m = new Mockery();
    myLocks = m.mock(Locks.class);
    myBuildFeatureDescriptor = m.mock(SBuildFeatureDescriptor.class);
    myResources = m.mock(Resources.class);
    myFactory = new SharedResourcesFeatureFactoryImpl(myLocks, myResources);
  }

  @Test
  public void testCreateFeature() throws Exception {
    m.checking(new Expectations() {{
      oneOf(myLocks).fromFeatureParameters(myBuildFeatureDescriptor);
      will(returnValue(new HashMap<String, Lock>()));
    }});

    final SharedResourcesFeature feature = myFactory.createFeature(myBuildFeatureDescriptor);
    assertNotNull(feature);
  }
}
