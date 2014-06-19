package jetbrains.buildServer.sharedResources.server.feature;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.SecurityContextEx;
import jetbrains.buildServer.serverSide.settings.ProjectSettingsManager;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.TestUtils;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.model.resources.ResourceFactory;
import jetbrains.buildServer.sharedResources.server.exceptions.DuplicateResourceException;
import jetbrains.buildServer.sharedResources.settings.PluginProjectSettings;
import jetbrains.buildServer.util.TestFor;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
@TestFor(testForClass = {Resources.class, ResourcesImpl.class})
public class ResourcesImplTest extends BaseTestCase {

  private Mockery m;

  private ProjectSettingsManager myProjectSettingsManager;

  private PluginProjectSettings myPluginProjectSettings;

  private ProjectManager myProjectManager;

  private SProject myProject;

  private SProject myRootProject;

  private SecurityContextEx mySecurityContextEx;

  private PluginProjectSettings myRootProjectSettings;

  private Map<String, Resource> myProjectResourceMap;

  private Map<String, Resource> myRootResourceMap;

  private ResourceFactory myResourceFactory;

  private ResourceFactory myRootResourceFactory;

  /**
   * Class under test
   */
  private ResourcesImpl resources;

  private final String myProjectId = TestUtils.generateRandomName();

  private final String myRootProjectId = "<ROOT>";

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    m = new Mockery() {{
      setImposteriser(ClassImposteriser.INSTANCE);
    }};

    myProjectSettingsManager = m.mock(ProjectSettingsManager.class);
    myPluginProjectSettings = m.mock(PluginProjectSettings.class);
    myProjectManager = m.mock(ProjectManager.class);
    mySecurityContextEx = m.mock(SecurityContextEx.class);
    myProject = m.mock(SProject.class, "currentProject");
    myRootProject = m.mock(SProject.class, "rootProject");
    myRootProjectSettings = m.mock(PluginProjectSettings.class, "myRootProjectSettings");
    resources = new ResourcesImpl(myProjectSettingsManager, myProjectManager, mySecurityContextEx);

    myRootResourceFactory = ResourceFactory.getFactory(myRootProjectId);
    myResourceFactory = ResourceFactory.getFactory(myProjectId);


    myProjectResourceMap = new HashMap<String, Resource>() {{
      put("resource1", myResourceFactory.newQuotedResource("resource1", 1, true));
      put("resource2", myResourceFactory.newInfiniteResource("resource2", true));
      put("resource3", myResourceFactory.newCustomResource("resource3", Arrays.asList("value1", "value2", "value3"), true));
    }};

    myRootResourceMap = new HashMap<String, Resource>() {{
      put("root_resource", myRootResourceFactory.newInfiniteResource("root_resource", true));
    }};
  }

  @Test
  public void testAddResource_Success() throws Exception {
    final Resource resource = myRootResourceFactory.newQuotedResource("new_resource1", 1, true);
    m.checking(createExpectationsCheck());
    m.checking(new Expectations() {{
      oneOf(myRootProjectSettings).getResourceMap();
      will(returnValue(myProjectResourceMap));

      oneOf(myPluginProjectSettings).addResource(resource);
    }});
    resources.addResource(myProjectId, resource);
    m.assertIsSatisfied();
  }

  @Test (expectedExceptions = DuplicateResourceException.class)
  public void testAddResource_Fail() throws Exception {
    final Resource resource = myProjectResourceMap.get("resource1");
    m.checking(createExpectationsCheck());
    m.checking(new Expectations() {{
      oneOf(myRootProjectSettings).getResourceMap();
      will(returnValue(myProjectResourceMap));
    }});
    resources.addResource(myProjectId, resource);
  }

  @Test
  public void testEditResource_Success() throws Exception {
    final String name = "resource1";
    final Resource resource = myResourceFactory.newQuotedResource("resource1_newName", 1, true);
    m.checking(createExpectationsCheck());
    m.checking(new Expectations() {{
      oneOf(myRootProjectSettings).getResourceMap();
      will(returnValue(myRootResourceMap));

      oneOf(myPluginProjectSettings).editResource(name, resource);
    }});
    resources.editResource(myProjectId, name, resource);
    m.assertIsSatisfied();
  }

  @Test (expectedExceptions = DuplicateResourceException.class)
  public void testEditResource_Fail() throws Exception {
    final Resource resource = myProjectResourceMap.get("resource1");
    final String newName = "resource2";
    m.checking(createExpectationsCheck());
    m.checking(new Expectations() {{
      oneOf(myRootProjectSettings).getResourceMap();
      will(returnValue(myProjectResourceMap));
    }});
    resources.editResource(myProjectId, newName, resource);
  }

  @Test
  public void testDeleteResource() {
    final String name = "myName1";
    m.checking(new Expectations() {{
      oneOf(myProjectSettingsManager).getSettings(myProjectId, SharedResourcesPluginConstants.SERVICE_NAME);
      will(returnValue(myPluginProjectSettings));

      oneOf(myPluginProjectSettings).deleteResource(name);
    }});

    resources.deleteResource(myProjectId, name);
    m.assertIsSatisfied();
  }

  @Test
  public void testAsMap() {
    m.checking(new Expectations() {{
      oneOf(myProjectSettingsManager).getSettings(myProjectId, SharedResourcesPluginConstants.SERVICE_NAME);
      will(returnValue(myPluginProjectSettings));

      oneOf(myPluginProjectSettings).getResourceMap();
      will(returnValue(myProjectResourceMap));

      oneOf(myProjectManager).findProjectById(myProjectId);
      will(returnValue(myProject));

      oneOf(myProject).getProjectId();
      will(returnValue(myProjectId));

      oneOf(myProject).getProjectPath();
      will(returnValue(Collections.singletonList(myProject)));
    }});

    final Map<String, Resource> result = resources.asMap(myProjectId);
    assertNotNull(result);
    assertEquals(myProjectResourceMap.size(), result.size());
  }

  @Test
  public void testGetCountFlat() {
    m.checking(new Expectations() {{
      oneOf(myProjectManager).findProjectById(myProjectId);
      will(returnValue(myProject));

      oneOf(myProject).getProjectPath();
      will(returnValue(Arrays.asList(myProject)));

      oneOf(myProject).getProjectId();
      will(returnValue(myProjectId));

      oneOf(myProjectSettingsManager).getSettings(myProjectId, SharedResourcesPluginConstants.SERVICE_NAME);
      will(returnValue(myPluginProjectSettings));

      oneOf(myPluginProjectSettings).getCount();
      will(returnValue(myProjectResourceMap.size()));

    }});

    final int count = resources.getCount(myProjectId);
    assertEquals(myProjectResourceMap.size(), count);
  }

  @Test
  public void testGetCountHierarchy() {
    m.checking(new Expectations() {{
      oneOf(myProjectManager).findProjectById(myProjectId);
      will(returnValue(myProject));

      oneOf(myProject).getProjectPath();
      will(returnValue(Arrays.asList(myProject, myRootProject)));

      oneOf(myProject).getProjectId();
      will(returnValue(myProjectId));

      oneOf(myProjectManager).findProjectById(myRootProjectId);
      will(returnValue(myRootProject));

      oneOf(myRootProject).getProjectId();
      will(returnValue(myRootProjectId));

      oneOf(myProjectSettingsManager).getSettings(myRootProjectId, SharedResourcesPluginConstants.SERVICE_NAME);
      will(returnValue(myRootProjectSettings));

      oneOf(myRootProjectSettings).getCount();
      will(returnValue(myRootResourceMap.size()));

      oneOf(myProjectSettingsManager).getSettings(myProjectId, SharedResourcesPluginConstants.SERVICE_NAME);
      will(returnValue(myPluginProjectSettings));

      oneOf(myPluginProjectSettings).getCount();
      will(returnValue(myProjectResourceMap.size()));

    }});

    final int count = resources.getCount(myProjectId);
    assertEquals(myProjectResourceMap.size() + myRootResourceMap.size(), count);

  }

  private Expectations createExpectationsCheck() {
    return new Expectations() {{
      // get root project
      oneOf(myProjectManager).getRootProject();
      will(returnValue(myRootProject));

      // get all subprojects as SYSTEM user
      exactly(1).of(same(mySecurityContextEx)).method("runAsSystem");
      will(returnValue(Arrays.asList(myProject)));

      // get root project id
      oneOf(myRootProject).getProjectId();
      will(returnValue(myRootProjectId));

      // get root project settings
      atLeast(1).of(myProjectSettingsManager).getSettings(myRootProjectId, SharedResourcesPluginConstants.SERVICE_NAME);
      will(returnValue(myRootProjectSettings));

      // get my project id
      atLeast(1).of(myProject).getProjectId();
      will(returnValue(myProjectId));

      // get my resources
      atLeast(1).of(myPluginProjectSettings).getResourceMap();
      will(returnValue(myProjectResourceMap));

      // get my settings
      atLeast(1).of(myProjectSettingsManager).getSettings(myProjectId, SharedResourcesPluginConstants.SERVICE_NAME);
      will(returnValue(myPluginProjectSettings));
    }};
  }
}
