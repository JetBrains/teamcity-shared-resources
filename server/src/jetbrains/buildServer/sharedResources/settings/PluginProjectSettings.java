/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.sharedResources.settings;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.serverSide.settings.ProjectSettings;
import jetbrains.buildServer.sharedResources.model.resources.CustomResource;
import jetbrains.buildServer.sharedResources.model.resources.QuotedResource;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.model.resources.ResourceFactory;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Class {@code PluginProjectSettings}
 *
 * @author Oleg Rybak
 */
public class PluginProjectSettings implements ProjectSettings {

  @NotNull
  private static final Logger LOG = Logger.getInstance(PluginProjectSettings.class.getName());

  @NotNull
  private final ReadWriteLock myLock = new ReentrantReadWriteLock(true);

  /**
   * XML storage structure
   *
   * // todo: make proper javadoc
   *
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
   *
   *    since 8.1
   *    <resource>
   *      <name></name>
   *      ...
   *      <enabled>false</enabled> means resource is disabled and will not get processed by runtime
   *      ...
   *    </resource>
   *    ...
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
    public static final String TAG_ENABLED = "enabled";
    public static final String VALUE_TYPE_QUOTA = "quota";
    public static final String VALUE_TYPE_CUSTOM = "custom";
    public static final String VALUE_QUOTA_INFINITE = "infinite";
  }

  @NotNull
  private Map<String, Resource> myResourceMap = new HashMap<String, Resource>();

  public PluginProjectSettings() {}

  @Override
  public void dispose() {
  }

  @Override
  public void readFrom(Element rootElement) {
    try {
      myLock.writeLock().lock();
      final List children = rootElement.getChildren(XML.TAG_RESOURCE);
      myResourceMap = new HashMap<String, Resource>();
      if (!children.isEmpty()) {
        for (Object child : children) { // children = resources
          parseResource((Element) child);
        }
      }
    } finally {
      myLock.writeLock().unlock();
    }
  }

  private void parseResource(@NotNull final Element resourceElement) {
    final String resourceName = resourceElement.getChild(XML.TAG_RESOURCE_NAME).getTextTrim();

    final Element stateElement = resourceElement.getChild(XML.TAG_ENABLED);
    boolean state = true;
    if (stateElement != null) {
      state = Boolean.parseBoolean(stateElement.getTextTrim());
    }
    Element e = resourceElement.getChild(XML.TAG_VALUES);
    final String valuesType = e.getAttributeValue(XML.ATTR_VALUES_TYPE);
    Resource parsedResource = null;
    if (XML.VALUE_TYPE_QUOTA.equals(valuesType)) {
      parsedResource = parseQuotedResource(resourceName, e, state);
    } else if (XML.VALUE_TYPE_CUSTOM.equals(valuesType)) {
      parsedResource = parseCustomResource(resourceName, e, state);
    } else {
      LOG.warn("Wrong resource values type [" + valuesType + "] for resource [" + resourceName + "]");
    }
    if (parsedResource != null) {
      myResourceMap.put(resourceName, parsedResource);
    }
  }

  private Resource parseQuotedResource(@NotNull final String resourceName, @NotNull final Element valuesElement, boolean state) {
    final String resourceQuota = valuesElement.getChild(XML.TAG_QUOTA).getTextTrim();
    Resource parsedResource;
    if (XML.VALUE_QUOTA_INFINITE.equals(resourceQuota)) {
      parsedResource = ResourceFactory.newInfiniteResource(resourceName, state);
    } else {
      parsedResource = ResourceFactory.newQuotedResource(resourceName,  Integer.parseInt(resourceQuota), state);
    }
    return parsedResource;
  }

  private Resource parseCustomResource(@NotNull final String resourceName, @NotNull final Element valuesElement, boolean state) {
    final List<String> c = new ArrayList<String>();
    final List children = valuesElement.getChildren(XML.TAG_VALUE);
    for(Object o: children) {
      c.add(((Element)o).getTextTrim());
    }
    return ResourceFactory.newCustomResource(resourceName, c, state);
  }


  @Override
  public void writeTo(Element parentElement) {
    try {
      myLock.readLock().lock();
      for (Resource resource : myResourceMap.values()) {
        final Element el = new Element(XML.TAG_RESOURCE);
        final Element resourceNameElement = new Element(XML.TAG_RESOURCE_NAME);
        resourceNameElement.setText(resource.getName());
        el.addContent(resourceNameElement);
        if (!resource.isEnabled()) {
          Element stateElement = new Element(XML.TAG_ENABLED);
          stateElement.setText(Boolean.toString(false));
          el.addContent(stateElement);
        }
        final Element values = new Element(XML.TAG_VALUES);
        // serializing here
        switch (resource.getType()) {
          case QUOTED:
            values.setAttribute(XML.ATTR_VALUES_TYPE, XML.VALUE_TYPE_QUOTA);
            final QuotedResource quotedResource = (QuotedResource)resource;
            final Element quota = new Element(XML.TAG_QUOTA);
            quota.setText(quotedResource.isInfinite() ? XML.VALUE_QUOTA_INFINITE : Integer.toString(quotedResource.getQuota()));
            values.addContent(quota);
            el.addContent(values);
            break;
          case CUSTOM:
            values.setAttribute(XML.ATTR_VALUES_TYPE, XML.VALUE_TYPE_CUSTOM);
            final CustomResource customResource = (CustomResource)resource;
            for (String str: customResource.getValues()) {
              Element value = new Element(XML.TAG_VALUE);
              value.setText(str.trim());
              values.addContent(value);
            }
            el.addContent(values);
            break;
          default:
            LOG.warn("Serialization for resource [" + resource.getName() + "] of type [" + resource.getType() + "] is not implemented");
            break;
        }
        parentElement.addContent(el);
      }
    } finally {
      myLock.readLock().unlock();
    }
  }

  public void addResource(@NotNull final Resource resource) {
    try {
      myLock.writeLock().lock();
      myResourceMap.put(resource.getName(), resource);
    } finally {
      myLock.writeLock().unlock();
    }
  }

  public void deleteResource(@NotNull final String name) {
    try {
      myLock.writeLock().lock();
      myResourceMap.remove(name);
    } finally {
      myLock.writeLock().unlock();
    }
  }

  public void editResource(@NotNull final String oldName,
                           @NotNull final Resource resource) {
    try {
      myLock.writeLock().lock();
      myResourceMap.remove(oldName);
      myResourceMap.put(resource.getName(), resource);
    } finally {
      myLock.writeLock().unlock();
    }
  }

  @NotNull
  public Collection<Resource> getResources() {
    try {
      myLock.readLock().lock();
      return Collections.unmodifiableCollection(myResourceMap.values());
    } finally {
      myLock.readLock().unlock();
    }
  }

  @NotNull
  public Map<String, Resource> getResourceMap() {
    try {
      myLock.readLock().lock();
      return Collections.unmodifiableMap(myResourceMap);
    } finally {
      myLock.readLock().unlock();
    }
  }

  public int getCount() {
    try {
      myLock.readLock().lock();
      return myResourceMap.size();
    } finally {
      myLock.readLock().unlock();
    }
  }
}
