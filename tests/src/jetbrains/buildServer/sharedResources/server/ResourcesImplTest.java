package jetbrains.buildServer.sharedResources.server;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.serverSide.settings.ProjectSettingsManager;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.TestUtils;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.model.resources.ResourceFactory;
import jetbrains.buildServer.sharedResources.server.feature.Resources;
import jetbrains.buildServer.sharedResources.server.feature.ResourcesImpl;
import jetbrains.buildServer.sharedResources.settings.PluginProjectSettings;
import jetbrains.buildServer.util.TestFor;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
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
@TestFor (testForClass = {Resources.class, ResourcesImpl.class})
public class ResourcesImplTest extends BaseTestCase {

  private Mockery m;

  private ProjectSettingsManager myProjectSettingsManager;

  private PluginProjectSettings myPluginProjectSettings;

  /** Class under test */
  private Resources resources;

  final String projectId = TestUtils.generateRandomName();

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    m = new Mockery() {{
      setImposteriser(ClassImposteriser.INSTANCE);
    }};

    myProjectSettingsManager = m.mock(ProjectSettingsManager.class);
    myPluginProjectSettings = m.mock(PluginProjectSettings.class);
    resources = new ResourcesImpl(myProjectSettingsManager);
  }

  @Test
  public void testAddResource() {
    final Resource resource = ResourceFactory.newQuotedResource("resource1", 1);
    m.checking(new Expectations() {{
      oneOf(myProjectSettingsManager).getSettings(projectId, SharedResourcesPluginConstants.SERVICE_NAME);
      will(returnValue(myPluginProjectSettings));

      oneOf(myPluginProjectSettings).addResource(resource);
    }});

    resources.addResource(projectId, resource);
    m.assertIsSatisfied();
  }

  @Test
  public void testDeleteResource() {
    final String name = "myName1";
    m.checking(new Expectations() {{
      oneOf(myProjectSettingsManager).getSettings(projectId, SharedResourcesPluginConstants.SERVICE_NAME);
      will(returnValue(myPluginProjectSettings));

      oneOf(myPluginProjectSettings).deleteResource(name);
    }});

    resources.deleteResource(projectId, name);
    m.assertIsSatisfied();
  }

  @Test
  public void testEditResource() {
    final String name = "myName1";
    final Resource resource = ResourceFactory.newQuotedResource("resource1", 1);

    m.checking(new Expectations() {{
      oneOf(myProjectSettingsManager).getSettings(projectId, SharedResourcesPluginConstants.SERVICE_NAME);
      will(returnValue(myPluginProjectSettings));

      oneOf(myPluginProjectSettings).editResource(name, resource);
    }});

    resources.editResource(projectId, name, resource);
    m.assertIsSatisfied();
  }

  @Test
  public void testGetAllResources() {
    final Map<String, Resource> resourceMap = new HashMap<String, Resource>();
    resourceMap.put("r1", ResourceFactory.newQuotedResource("r1", 1));
    resourceMap.put("r2", ResourceFactory.newInfiniteResource("r2"));

    m.checking(new Expectations() {{
      oneOf(myProjectSettingsManager).getSettings(projectId, SharedResourcesPluginConstants.SERVICE_NAME);
      will(returnValue(myPluginProjectSettings));

      oneOf(myPluginProjectSettings).getResourceMap();
      will(returnValue(resourceMap));
    }});

    final Map<String, Resource> result = resources.getAllResources(projectId);
    assertNotNull(result);
    assertEquals(resourceMap.size(), result.size());
  }
}
