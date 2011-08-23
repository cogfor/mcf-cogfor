/* $Id$ */

/**
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements. See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License. You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.apache.manifoldcf.scriptengine;

import org.apache.manifoldcf.core.interfaces.*;

/** Variable wrapper for ConfigurationNode object.
*/
public class VariableConfigurationNode extends VariableBase
{
  protected ConfigurationNode configurationNode;
  
  public VariableConfigurationNode(String name)
  {
    configurationNode = new ConfigurationNode(name);
  }
  
  public VariableConfigurationNode(ConfigurationNode node)
  {
    configurationNode = node;
  }
  
  /** Get the variable's value as a string */
  public String getStringValue()
    throws ScriptException
  {
    return configurationNode.toString();
  }

  /** Get a named attribute of the variable; e.g. xxx.yyy */
  public VariableReference getAttribute(String attributeName)
    throws ScriptException
  {
    // We recognize the __size__ attribute
    if (attributeName.equals(ATTRIBUTE_SIZE))
      return new VariableInt(configurationNode.getChildCount());
    // Also, the __name__ attribute
    if (attributeName.equals(ATTRIBUTE_NAME))
      return new VariableString(configurationNode.getType());
    // And the __value__ attribute
    if (attributeName.equals(ATTRIBUTE_VALUE))
    {
      return new ValueReference();
    }
    // All others are presumed to be attributes of the configuration node, which can be set or cleared.
    return new AttributeReference(attributeName);
  }
  
  /** Get an indexed property of the variable */
  public VariableReference getIndexed(int index)
    throws ScriptException
  {
    if (index < configurationNode.getChildCount())
      return new NodeReference(index);
    return super.getIndexed(index);
  }
  
  public ConfigurationNode getConfigurationNode()
  {
    return configurationNode;
  }
  
  /** Implement VariableReference to allow values to be set or cleared */
  protected class ValueReference implements VariableReference
  {
    public ValueReference()
    {
    }
    
    public void setReference(Variable v)
      throws ScriptException
    {
      if (v == null)
        configurationNode.setValue(null);
      else
      {
        String value = v.getStringValue();
        configurationNode.setValue(value);
      }
    }
    
    public Variable resolve()
      throws ScriptException
    {
      String value = configurationNode.getValue();
      if (value == null)
        throw new ScriptException("ConfigurationNode value is null");
      else
        return new VariableString(value);
    }
    
    public boolean isNull()
    {
      return configurationNode.getValue() == null;
    }
  }
  
  /** Implement VariableReference to allow attributes to be set or cleared */
  protected class AttributeReference implements VariableReference
  {
    protected String attributeName;
    
    public AttributeReference(String attributeName)
    {
      this.attributeName = attributeName;
    }
    
    public void setReference(Variable v)
      throws ScriptException
    {
      if (v == null)
        configurationNode.setAttribute(attributeName,null);
      else
      {
        String value = v.getStringValue();
        configurationNode.setAttribute(attributeName,value);
      }
    }

    public Variable resolve()
      throws ScriptException
    {
      String attrValue = configurationNode.getAttributeValue(attributeName);
      if (attrValue == null)
        throw new ScriptException("ConfigurationNode has no attribute named '"+attributeName+"'");
      return new VariableString(attrValue);
    }

    public boolean isNull()
    {
      return (configurationNode.getAttributeValue(attributeName) == null);
    }
  }
  
  /** Extend VariableReference class so we capture attempts to set the reference, and actually overwrite the child when that is done */
  protected class NodeReference implements VariableReference
  {
    protected int index;
    
    public NodeReference(int index)
    {
      this.index = index;
    }
    
    public void setReference(Variable v)
      throws ScriptException
    {
      if (!(v instanceof VariableConfigurationNode))
        throw new ScriptException("Cannot set ConfigurationNode child value to anything other than a ConfigurationNode object");
      if (index >= configurationNode.getChildCount())
        throw new ScriptException("Index out of range for ConfigurationNode children");
      configurationNode.removeChild(index);
      configurationNode.addChild(index,((VariableConfigurationNode)v).getConfigurationNode());
    }

    public Variable resolve()
      throws ScriptException
    {
      if (index >= configurationNode.getChildCount())
        throw new ScriptException("Index out of range for ConfigurationNode children");
      return new VariableConfigurationNode(configurationNode.findChild(index));
    }

    /** Check if this reference is null */
    public boolean isNull()
    {
      return index >= configurationNode.getChildCount();
    }

  }
}