package jetbrains.buildServer.sharedResources.server.feature;

import java.util.*;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.SProjectFeatureDescriptor;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.TestUtils;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.model.resources.ResourceFactory;
import jetbrains.buildServer.sharedResources.server.project.ResourceProjectFeature;
import jetbrains.buildServer.sharedResources.server.project.ResourceProjectFeatureImpl;
import jetbrains.buildServer.sharedResources.server.project.ResourceProjectFeatures;
import jetbrains.buildServer.util.TestFor;
import org.jetbrains.annotations.NotNull;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
@TestFor(testForClass = {Resources.class, ResourcesImpl.class})
public class ResourcesImplTest extends BaseTestCase {

  private Mockery m;

  private ResourceProjectFeatures myResourceProjectFeatures;

  private ProjectManager myProjectManager;

  private SProject myProject;

  private SProject myRootProject;

  private Map<String, Resource> myProjectResourceMap;

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

    myProjectManager = m.mock(ProjectManager.class);
    myProject = m.mock(SProject.class, "currentProject");
    myRootProject = m.mock(SProject.class, "rootProject");

    myResourceProjectFeatures = m.mock(ResourceProjectFeatures.class);
    resources = new ResourcesImpl(myProjectManager, myResourceProjectFeatures);

    myProjectResourceMap = new HashMap<String, Resource>() {{
      put("resource1", ResourceFactory.newQuotedResource("resource1", myProjectId, "resource1", 1, true));
      put("resource2", ResourceFactory.newInfiniteResource("resource2", myProjectId, "resource2", true));
      put("resource3", ResourceFactory.newCustomResource("resource3", myProjectId, "resource3", Arrays.asList("value1", "value2", "value3"), true));
    }};

  }

  @Override
  @AfterMethod
  public void tearDown() throws Exception {
    super.tearDown();
    m.assertIsSatisfied();
  }

  @Test
  public void testAsMap() {
    m.checking(new Expectations() {{
      oneOf(myProjectManager).findProjectById(myProjectId);
      will(returnValue(myProject));

      oneOf(myResourceProjectFeatures).asMap(myProject);
      will(returnValue(myProjectResourceMap));
    }});

    final Map<String, Resource> result = resources.asMap(myProjectId);
    assertNotNull(result);
    assertEquals(myProjectResourceMap.size(), result.size());
  }

  //@Test (enabled = false) // todo: move to project resource features test
  //@TestFor(issues = "TW-37406")
  //public void testAsProjectResourceMap_PreserveOrder() {
  //  m.checking(new Expectations() {{
  //    oneOf(myProjectManager).findProjectById(myProjectId);
  //    will(returnValue(myProject));
  //
  //    allowing(myProject).getProjectId();
  //    will(returnValue(myProjectId));
  //
  //    oneOf(myProject).getProjectPath();
  //    will(returnValue(Arrays.asList(myRootProject, myProject)));
  //
  //    allowing(myRootProject).getProjectId();
  //    will(returnValue(myRootProjectId));
  //  }});
  //
  //  final Map<SProject, Map<String, Resource>> result = resources.asProjectResourceMap(myProjectId);
  //  assertEquals(2, result.size());
  //  Iterator<SProject> it = result.keySet().iterator();
  //  assertTrue(it.hasNext());
  //  SProject p = it.next();
  //  assertEquals(myProjectId, p.getProjectId());
  //  assertTrue(it.hasNext());
  //  p = it.next();
  //  assertEquals(myRootProjectId, p.getProjectId());
  //}

  @Test
  public void testGetOwnResources_NoDuplicates() {
    final List<ResourceProjectFeature> projectFeatures = Arrays.asList(
      createFeature(ResourceFactory.newInfiniteResource("project1", myProjectId, "RESOURCE_1", true)),
      createFeature(ResourceFactory.newInfiniteResource("project2", myProjectId, "RESOURCE_2", true)),
      createFeature(ResourceFactory.newInfiniteResource("project3", myProjectId, "RESOURCE_3", true))
    );

    m.checking(new Expectations() {{
      oneOf(myResourceProjectFeatures).getOwnFeatures(myProject);
      will(returnValue(projectFeatures));
    }});

    final List<Resource> result = resources.getOwnResources(myProject);
    assertEquals(3, result.size());
  }

  @Test
  public void testGetOwnResources_IgnoreDuplicates() {
    final List<ResourceProjectFeature> projectFeatures = Arrays.asList(
      createFeature(ResourceFactory.newInfiniteResource("project1", myProjectId, "RESOURCE_1", true)),
      createFeature(ResourceFactory.newInfiniteResource("project2", myProjectId, "RESOURCE_1", true)),
      createFeature(ResourceFactory.newInfiniteResource("project3", myProjectId, "RESOURCE_3", true))
    );

    m.checking(new Expectations() {{
      oneOf(myResourceProjectFeatures).getOwnFeatures(myProject);
      will(returnValue(projectFeatures));
    }});

    final List<Resource> result = resources.getOwnResources(myProject);
    assertEquals(1, result.size());
  }

  @Test
  @SuppressWarnings("Duplicates")
  public void testGetResources_NoDuplicates() {
    final List<ResourceProjectFeature> projectFeatures = Arrays.asList(
      createFeature(ResourceFactory.newInfiniteResource("project1", myProjectId, "RESOURCE_1", true)),
      createFeature(ResourceFactory.newInfiniteResource("project2", myProjectId, "RESOURCE_2", true))
    );

    final List<ResourceProjectFeature> rootFeatures = Arrays.asList(
      createFeature(ResourceFactory.newInfiniteResource("root1", myProjectId, "RESOURCE_3", true)),
      createFeature(ResourceFactory.newInfiniteResource("root2", myProjectId, "RESOURCE_4", true))
    );

    m.checking(new Expectations() {{
      oneOf(myProject).getProjectPath();
      will(returnValue(Arrays.asList(myRootProject, myProject)));

      oneOf(myResourceProjectFeatures).getOwnFeatures(myProject);
      will(returnValue(projectFeatures));

      oneOf(myResourceProjectFeatures).getOwnFeatures(myRootProject);
      will(returnValue(rootFeatures));

    }});

    final List<Resource> result = resources.getResources(myProject);
    assertEquals(4, result.size());
  }

  @Test
  @SuppressWarnings("Duplicates")
  public void testGetResources_IgnoreDuplicates() {
    final List<ResourceProjectFeature> projectFeatures = Arrays.asList(
      createFeature(ResourceFactory.newInfiniteResource("project1", myProjectId, "RESOURCE_1", true)),
      createFeature(ResourceFactory.newInfiniteResource("project2", myProjectId, "RESOURCE_1", true)),
      createFeature(ResourceFactory.newInfiniteResource("project3", myProjectId, "RESOURCE_2", true))
    );

    final List<ResourceProjectFeature> rootFeatures = Arrays.asList(
      createFeature(ResourceFactory.newInfiniteResource("root1", myProjectId, "RESOURCE_3", true)),
      createFeature(ResourceFactory.newInfiniteResource("root2", myProjectId, "RESOURCE_3", true)),
      createFeature(ResourceFactory.newInfiniteResource("root3", myProjectId, "RESOURCE_4", true))
    );

    m.checking(new Expectations() {{
      oneOf(myProject).getProjectPath();
      will(returnValue(Arrays.asList(myRootProject, myProject)));

      oneOf(myResourceProjectFeatures).getOwnFeatures(myProject);
      will(returnValue(projectFeatures));

      oneOf(myResourceProjectFeatures).getOwnFeatures(myRootProject);
      will(returnValue(rootFeatures));

    }});

    final List<Resource> result = resources.getResources(myProject);
    assertEquals(2, result.size());
  }

  @Test
  @SuppressWarnings("Duplicates")
  public void testGetAllOwnResources_IncludeDuplicates() {
    final List<ResourceProjectFeature> projectFeatures = Arrays.asList(
      createFeature(ResourceFactory.newInfiniteResource("project1", myProjectId, "RESOURCE_1", true)),
      createFeature(ResourceFactory.newInfiniteResource("project2", myProjectId, "RESOURCE_1", true)),
      createFeature(ResourceFactory.newInfiniteResource("project3", myProjectId, "RESOURCE_3", true))
    );

    m.checking(new Expectations() {{
      oneOf(myResourceProjectFeatures).getOwnFeatures(myProject);
      will(returnValue(projectFeatures));
    }});

    final List<Resource> result = resources.getAllOwnResources(myProject);
    assertEquals(3, result.size());
  }


  @Test
  public void testGetCount_SingleProject() {
    final List<ResourceProjectFeature> projectFeatures = Collections.singletonList(
      createFeature(ResourceFactory.newInfiniteResource("project1", myProjectId, "RESOURCE_2", true))
    );

    m.checking(new Expectations() {{
      oneOf(myProject).getProjectPath();
      will(returnValue(Collections.singletonList(myProject)));

      oneOf(myResourceProjectFeatures).getOwnFeatures(myProject);
      will(returnValue(projectFeatures));

    }});
    assertEquals(1, resources.getCount(myProject));
  }

  @Test
  @SuppressWarnings("Duplicates")
  public void testGetCount_Hierarchy() {
    final List<ResourceProjectFeature> rootFeatures = Collections.singletonList(
      createFeature(ResourceFactory.newInfiniteResource("root1", myRootProjectId, "RESOURCE_1", true))
    );

    final List<ResourceProjectFeature> projectFeatures = Collections.singletonList(
      createFeature(ResourceFactory.newInfiniteResource("project1", myProjectId, "RESOURCE_2", true))
    );

    m.checking(new Expectations() {{
      oneOf(myProject).getProjectPath();
      will(returnValue(Arrays.asList(myRootProject, myProject)));

      oneOf(myResourceProjectFeatures).getOwnFeatures(myProject);
      will(returnValue(projectFeatures));

      oneOf(myResourceProjectFeatures).getOwnFeatures(myRootProject);
      will(returnValue(rootFeatures));
    }});
    assertEquals(2, resources.getCount(myProject));
  }


  @Test
  @SuppressWarnings("Duplicates")
  public void testGetCount_HierarchyOverriding() throws Exception {
    final List<ResourceProjectFeature> rootFeatures = Collections.singletonList(
      createFeature(ResourceFactory.newInfiniteResource("root1", myRootProjectId, "RESOURCE", true))
    );

    final List<ResourceProjectFeature> projectFeatures = Collections.singletonList(
      createFeature(ResourceFactory.newInfiniteResource("project1", myProjectId, "RESOURCE", true))
    );

    m.checking(new Expectations() {{
      oneOf(myProject).getProjectPath();
      will(returnValue(Arrays.asList(myRootProject, myProject)));

      oneOf(myResourceProjectFeatures).getOwnFeatures(myProject);
      will(returnValue(projectFeatures));

      oneOf(myResourceProjectFeatures).getOwnFeatures(myRootProject);
      will(returnValue(rootFeatures));
    }});
    assertEquals(1, resources.getCount(myProject));
  }

  private ResourceProjectFeature createFeature(@NotNull final Resource resource) {
    final SProjectFeatureDescriptor descriptor = m.mock(SProjectFeatureDescriptor.class, "descriptor" + resource.getProjectId() + "_" + resource.getId());
    m.checking(new Expectations() {{
      allowing(descriptor).getId();
      will(returnValue(resource.getId()));

      allowing(descriptor).getParameters();
      will(returnValue(resource.getParameters()));

      allowing(descriptor).getProjectId();
      will(returnValue(resource.getProjectId()));

      allowing(descriptor).getType();
      will(returnValue(SharedResourcesPluginConstants.FEATURE_TYPE));
    }});
    return new ResourceProjectFeatureImpl(descriptor);
  }

}
