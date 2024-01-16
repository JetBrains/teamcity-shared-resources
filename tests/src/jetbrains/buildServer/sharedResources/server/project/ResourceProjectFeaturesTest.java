

package jetbrains.buildServer.sharedResources.server.project;

import com.intellij.openapi.util.Pair;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.SProjectFeatureDescriptor;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants.ProjectFeatureParameters;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.model.resources.ResourceFactory;
import jetbrains.buildServer.sharedResources.model.resources.ResourceType;
import org.jetbrains.annotations.NotNull;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static jetbrains.buildServer.sharedResources.server.SharedResourcesBuildFeature.FEATURE_TYPE;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class ResourceProjectFeaturesTest extends BaseTestCase {

  private final AtomicInteger mockCounter = new AtomicInteger(0);

  private final String myProjectId = "PROJECT_ID";

  private Mockery m;
  private SProject myProject;

  private ResourceProjectFeatures myFeatures;

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    m = new Mockery();
    myFeatures = new ResourceProjectFeaturesImpl();
    myProject = m.mock(SProject.class, "My Project");
  }

  @Test
  public void testAddResource_toEmpty_Success() throws Exception {
    final Resource resource = ResourceFactory.newInfiniteResource("resource", myProjectId, "resource", true);
    m.checking(new Expectations() {{
      oneOf(myProject).addFeature(FEATURE_TYPE, resource.getParameters());
    }});

    myFeatures.addFeature(myProject, resource.getParameters());
  }

  @Test
  public void testEditResource_Success() throws Exception {
    final String oldName = "OldName";
    final Pair<String, SProjectFeatureDescriptor> existing = createExistingResource(oldName);

    final Resource resource = ResourceFactory.newInfiniteResource("NewName", myProjectId, "NewName", true);
    m.checking(new Expectations() {{
      allowing(myProject).getOwnFeaturesOfType(FEATURE_TYPE);
      will(returnValue(Collections.singletonList(existing.getSecond())));

      oneOf(myProject).updateFeature(existing.getFirst(), FEATURE_TYPE, resource.getParameters());
    }});

    myFeatures.updateFeature(myProject, existing.getFirst(), resource.getParameters());
  }

  @Test
  public void testDeleteResource() throws Exception {
    final Pair<String, SProjectFeatureDescriptor> existingResource = createExistingResource("MyResource");
    m.checking(new Expectations() {{
      allowing(myProject).getOwnFeaturesOfType(FEATURE_TYPE);
      will(returnValue(Collections.singletonList(existingResource.getSecond())));

      oneOf(myProject).removeFeature(existingResource.getFirst());
    }});

    myFeatures.removeFeature(myProject, existingResource.getFirst());
  }

  /**
   * Creates existing resource as a project feature
   * Adds expectations of the type 'allowing' for resource parameters access
   *
   * @param name resource name
   * @return mocked feature descriptor with generated feature id
   */
  private Pair<String, SProjectFeatureDescriptor> createExistingResource(@NotNull final String name) {
    final String id = "ExistingResource:<" + name + "> #" + mockCounter.incrementAndGet();
    final SProjectFeatureDescriptor result = m.mock(SProjectFeatureDescriptor.class, id);
    final Map<String, String> resultParams = new HashMap<>();
    resultParams.put(ProjectFeatureParameters.NAME, name);
    resultParams.put(ProjectFeatureParameters.TYPE, ResourceType.QUOTED.name());
    resultParams.put(ProjectFeatureParameters.QUOTA, "1");
    m.checking(new Expectations() {{
      allowing(result).getParameters();
      will(returnValue(resultParams));

      allowing(result).getId();
      will(returnValue(id));
    }});
    return new Pair<>(id, result);
  }

  @AfterMethod
  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    m.assertIsSatisfied();
  }
}