package jetbrains.buildServer.sharedResources.settings;

import jetbrains.buildServer.serverSide.settings.ProjectSettings;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Class {@code SharedResourcesProjectSettings}
 *
 * @author Oleg Rybak
 */
public final class SharedResourcesProjectSettings implements ProjectSettings {

  /**
   * XMl Tag names
   */
  private interface XML {
    public static final String RESOURCE = "resource";
    public static final String RESOURCE_NAME = "resource-name";
  }

  private List<String> mySharedResourceNames = new ArrayList<String>();

  public SharedResourcesProjectSettings() {
  }

  @Override
  public void dispose() {
  }

  @Override
  public void readFrom(Element rootElement) {
    final List children = rootElement.getChildren(XML.RESOURCE);
    mySharedResourceNames = new ArrayList<String>();
    if (!children.isEmpty()) {
      for (Object o : children) {
        Element el = (Element) o;
        mySharedResourceNames.add(el.getAttributeValue(XML.RESOURCE_NAME));
      }
    }
  }

  @Override
  public void writeTo(Element parentElement) {
    for (String str : mySharedResourceNames) {
      final Element el = new Element(XML.RESOURCE);
      el.setAttribute(XML.RESOURCE_NAME, str);
      parentElement.addContent(el);
    }
  }

  public List<String> getSharedResourceNames() {
    return Collections.unmodifiableList(mySharedResourceNames);
  }

  public void putSampleData() {
    for (int i = 0; i < 3; i++) {
      mySharedResourceNames.add(UUID.randomUUID().toString());
    }
  }

  public void addResource(@NotNull String resourceName) {
    mySharedResourceNames.add(resourceName);
  }

  public void remove(String str) {
    mySharedResourceNames.remove(str);
  }
}
