/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import org.jdom.Element;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Class {@code SharedResourcesProjectSettings}
 *
 * @author Oleg Rybak
 */
public final class SharedResourcesProjectSettings implements ProjectSettings {

  private final ReadWriteLock myLock = new ReentrantReadWriteLock(true);

  private static final Logger LOG = Logger.getInstance(SharedResourcesProjectSettings.class.getName());

  /**
   * XML storage structure
   *
   * // todo: make proper javadoc
   *
   *
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
   */
  private interface XML {
    public static final String TAG_RESOURCE = "resource";
    public static final String TAG_RESOURCE_NAME = "name";
    public static final String ATTR_VALUES_TYPE = "type";
    public static final String TAG_VALUES = "values";
    //    public static final String TAG_VALUE = "value";
    public static final String TAG_QUOTA = "quota";
    public static final String VALUE_TYPE_QUOTA = "quota";
    //    public static final String VALUE_TYPE_CUSTOM = "custom";
    public static final String VALUE_QUOTA_INFINITE = "infinite";
  }

  private Map<String, Resource> myResourceMap = new HashMap<String, Resource>();

  public SharedResourcesProjectSettings() {
  }

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
            myResourceMap.put(resourceName, parsedResource);
          } else {
            LOG.warn("Values type [" + valuesType + "] is not yet supported =((");
          }
        }
      }
    } finally {
      myLock.writeLock().unlock();
    }
  }

  @Override
  public void writeTo(Element parentElement) {
    try {
      myLock.readLock().lock();
      for (Resource r: myResourceMap.values()) {
        final Element el = new Element(XML.TAG_RESOURCE);
        final Element resourceName = new Element(XML.TAG_RESOURCE_NAME);
        resourceName.setText(r.getName());
        el.addContent(resourceName);
        final Element values = new Element(XML.TAG_VALUES);
        // todo: resources by type.
        values.setAttribute(XML.ATTR_VALUES_TYPE, XML.VALUE_TYPE_QUOTA); // todo: support custom (3rd stage)
        Element quota = new Element(XML.TAG_QUOTA);
        quota.setText(r.isInfinite() ? XML.VALUE_QUOTA_INFINITE : Integer.toString(r.getQuota()));
        values.addContent(quota);
        el.addContent(values);
        parentElement.addContent(el);
      }
    } finally {
      myLock.readLock().unlock();
    }
  }

  public void addResource(Resource resource) {
    try {
      myLock.writeLock().lock();
      myResourceMap.put(resource.getName(), resource);
    } finally {
      myLock.writeLock().unlock();
    }
  }

  public void deleteResource(String name) {
    try {
      myLock.writeLock().lock();
      myResourceMap.remove(name);
    } finally {
      myLock.writeLock().unlock();
    }
  }

  public void editResource(String oldName, Resource resource) {
    try {
      myLock.writeLock().lock();
      myResourceMap.remove(oldName);
      myResourceMap.put(resource.getName(), resource);
    } finally {
      myLock.writeLock().unlock();
    }
  }

  public Collection<Resource> getResources() {
    try {
      myLock.readLock().lock();
      return Collections.unmodifiableCollection(myResourceMap.values());
    } finally {
      myLock.readLock().unlock();
    }
  }

  public Map<String, Resource> getResourceMap() {
    try {
      myLock.readLock().lock();
      return Collections.unmodifiableMap(myResourceMap);
    } finally {
      myLock.readLock().unlock();
    }
  }
}
