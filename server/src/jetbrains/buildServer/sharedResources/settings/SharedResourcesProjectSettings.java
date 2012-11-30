package jetbrains.buildServer.sharedResources.settings;

import jetbrains.buildServer.serverSide.settings.ProjectSettings;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import org.jdom.Element;

import java.util.*;

/**
 * Class {@code SharedResourcesProjectSettings}
 *
 * @author Oleg Rybak
 */
public final class SharedResourcesProjectSettings implements ProjectSettings {

  /**
   * XML Tag names
   */
  private interface XML {
    public static final String RESOURCE = "resource";
    public static final String RESOURCE_NAME = "resource-name";
    public static final String RESOURCE_QUOTA = "resource-quota";
  }

  private List<Resource> myResources = new ArrayList<Resource>();

  public SharedResourcesProjectSettings() {
  }

  @Override
  public void dispose() {
  }

  @Override
  public void readFrom(Element rootElement) {
    final List children = rootElement.getChildren(XML.RESOURCE);
    myResources = new ArrayList<Resource>();
    if (!children.isEmpty()) {
      for (Object o : children) {
        Element el = (Element) o;
        final String resourceName = el.getAttributeValue(XML.RESOURCE_NAME);
        final String resourceQuota = el.getAttributeValue(XML.RESOURCE_QUOTA);
        myResources.add(Resource.newResource(resourceName, Integer.parseInt(resourceQuota)));
      }
    }
  }

  @Override
  public void writeTo(Element parentElement) {
    for (Resource r: myResources) {
      final Element el = new Element(XML.RESOURCE);
      el.setAttribute(XML.RESOURCE_NAME, r.getName());
      el.setAttribute(XML.RESOURCE_QUOTA, Integer.toString(r.getQuota()));
      parentElement.addContent(el);
    }
  }

  public void addResource(Resource resource) {
    myResources.add(resource);
  }

  public void deleteResource(String name) {

  }

  public List<Resource> getResources() {
    return Collections.unmodifiableList(myResources);
  }

  public void putSampleData() {
    int n = new Random(System.currentTimeMillis()).nextInt(1000);
    for (int i = 0; i < n % 4; i++) {
      myResources.add(Resource.newResource(UUID.randomUUID().toString(), (n % 3 == 0 ? -1: n % 100)));
    }
  }

}
