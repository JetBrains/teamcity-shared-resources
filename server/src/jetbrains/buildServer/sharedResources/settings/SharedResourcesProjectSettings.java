package jetbrains.buildServer.sharedResources.settings;

import com.intellij.openapi.diagnostic.Logger;
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

  private static final Logger LOG = Logger.getInstance(SharedResourcesProjectSettings.class.getName());

  /**
   * XML storage structure
   *
   * // todo: make proper javadoc
   *
   * {@code
   *   <JetBrains.SharedResources>
   *     <resource>
   *       <name>my_db_connections</name>
   *       <description>Database Connections</description>
   *       <values type="quota">
   *         <quota>10</quota>
   *       </values>
   *     </resource>
   *   </JetBrains.SharedResources>
   *
   *   <JetBrains.SharedResources>
   *     <resource>
   *       <name>my_test_server_url</name>
   *       <description>Test servers</description>
   *       <values type="custom">
   *         <value>http://myserver1.com</value>
   *         <value>http://myserver2.com</value>
   *         <value>http://myserver3.com</value>
   *       </values>
   *      </resource>
   *    </JetBrains.SharedResources>
   * }
   *
   *
   */
  private interface XML {
    public static final String TAG_RESOURCE = "resource";
    public static final String TAG_RESOURCE_NAME = "name";
    public static final String ATTR_VALUES_TYPE = "type";
    public static final String TAG_VALUES = "values";
    public static final String TAG_VALUE = "value";
    public static final String TAG_QUOTA = "quota";
    public static final String VALUE_TYPE_QUOTA = "quota";
    public static final String VALUE_TYPE_CUSTOM = "custom";
    public static final String VALUE_QUOTA_INFINITE = "infinite";
  }

  private Map<String, Resource> myResourceMap = new HashMap<String, Resource>();

  private Set<Resource> myResources = new LinkedHashSet<Resource>();

  public SharedResourcesProjectSettings() {
  }

  @Override
  public void dispose() {
  }

  @Override
  public void readFrom(Element rootElement) {
    final List children = rootElement.getChildren(XML.TAG_RESOURCE);
    myResources = new LinkedHashSet<Resource>();
    myResourceMap = new HashMap<String, Resource>();
    if (!children.isEmpty()) {
      for (Object o : children) {
        Element el = (Element) o;
        final String resourceName = el.getChild(XML.TAG_RESOURCE_NAME).getTextTrim();
        Element e = el.getChild(XML.TAG_VALUES);
        final String valuesType = e.getAttributeValue(XML.ATTR_VALUES_TYPE);
        if (XML.VALUE_TYPE_QUOTA.equals(valuesType)) {
          final String resourceQuota = e.getChild(XML.TAG_QUOTA).getTextTrim();
          Resource parsedResource; // todo: move parsing into Resource class
          if (XML.VALUE_QUOTA_INFINITE.equals(resourceQuota)) {
            parsedResource = Resource.newInfiniteResource(resourceName);
          } else {
            parsedResource = Resource.newResource(resourceName,  Integer.parseInt(resourceQuota));
          }
          myResources.add(parsedResource);
          myResourceMap.put(resourceName, parsedResource);
        } else {
          LOG.warn("Values type [" + valuesType + "] is not yet supported =((");
        }
      }
    }
  }

  @Override
  public void writeTo(Element parentElement) {
    for (Resource r: myResources) {
      final Element el = new Element(XML.TAG_RESOURCE);
      final Element resourceName = new Element(XML.TAG_RESOURCE_NAME);
      resourceName.setText(r.getName());
      el.addContent(resourceName);
      final Element values = new Element(XML.TAG_VALUES);
      // todo: resources by type.
      values.setAttribute(XML.ATTR_VALUES_TYPE, XML.VALUE_TYPE_QUOTA); // todo: support custom
      Element quota = new Element(XML.TAG_QUOTA);
      quota.setText(r.isInfinite() ? XML.VALUE_QUOTA_INFINITE : Integer.toString(r.getQuota()));
      values.addContent(quota);
      el.addContent(values);
      parentElement.addContent(el);
    }
  }

  public void addResource(Resource resource) {
    myResourceMap.put(resource.getName(), resource);
    myResources.add(resource);
  }

  public void deleteResource(String name) {
    final Resource r = myResourceMap.remove(name);
    if (r != null) {
      myResources.remove(r);
    }
  }

  public Collection<Resource> getResources() {
    return Collections.unmodifiableSet(myResources);
  }

  public void putSampleData() {
    final int n = new Random(System.currentTimeMillis()).nextInt(1000);
    for (int i = 0; i < n % 4; i++) {
      addResource(Resource.newResource(UUID.randomUUID().toString(), (n % 3 == 0 ? -1: n % 100)));
    }
  }

}
