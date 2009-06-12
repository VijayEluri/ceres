package com.bc.ceres.binding.dom;

import com.bc.ceres.core.Assert;

import java.util.ArrayList;
import java.util.HashMap;


public class DefaultDomElement implements DomElement {

    private DomElement parent;
    private String name;
    private String value;
    private ArrayList<String> attributeList;
    private HashMap<String, String> attributeMap;
    private ArrayList<DomElement> elementList;
    private HashMap<String, DomElement> elementMap;


    public DefaultDomElement(String name) {
        this(name, null);
    }

    public DefaultDomElement(String name, String value) {
        Assert.notNull(name, "name");
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public DomElement getParent() {
        return parent;
    }

    public void setParent(DomElement parent) {
        this.parent = parent;
    }

    public String getAttribute(String name) {
        Assert.notNull(name, "name");
        return attributeMap != null ? attributeMap.get(name) : null;
    }

    public void setAttribute(String name, String value) {
        Assert.notNull(name, "name");
        Assert.notNull(value, "value");
        if (attributeList == null) {
            attributeList = new ArrayList<String>();
            attributeMap = new HashMap<String, String>();
        }
        if (!attributeMap.containsKey(name)) {
            attributeList.add(name);
        }
        attributeMap.put(name, value);
    }

    public String[] getAttributeNames() {
        return attributeList != null ? attributeList.toArray(new String[attributeList.size()]) : new String[0];
    }

    public DomElement getChild(String elementName) {
        return elementMap != null ? elementMap.get(elementName) : null;
    }

    public DomElement[] getChildren() {
        return elementList != null ? elementList.toArray(new DomElement[elementList.size()]) : new DomElement[0];
    }

    public DomElement[] getChildren(String elementName) {
        if (elementList == null) {
            return new DomElement[0];
        }
        ArrayList<DomElement> children = new ArrayList<DomElement>(elementList.size());
        for (DomElement domElement : elementList) {
            if (elementName.equals(domElement.getName())) {
                children.add(domElement);
            }
        }
        return children.toArray(new DomElement[children.size()]);
    }

    public DomElement getChild(int index) {
        return elementList != null ? elementList.get(index) : null;
    }

    public int getChildCount() {
        return elementList != null ? elementList.size() : 0;
    }

    public DomElement createChild(String name) {
        final DefaultDomElement child = new DefaultDomElement(name);
        addChild(child);
        return child;
    }

    public void addChild(DomElement child) {
        if (elementList == null) {
            this.elementList = new ArrayList<DomElement>();
            this.elementMap = new HashMap<String, DomElement>();
        }
        elementList.add(child);
        elementMap.put(child.getName(), child);
        child.setParent(this);
    }

    public String toXml() {
        StringBuilder builder = new StringBuilder(256);

        builder.append("<");
        builder.append(getName());
        if (attributeList != null) {
            for (String name1 : attributeList) {
                builder.append(' ');
                builder.append(name1);
                builder.append('=');
                builder.append('"');
                builder.append(attributeMap.get(name1));
                builder.append('"');
            }
        }

        if (elementList != null) {
            builder.append(">");
            builder.append('\n');
            if (getValue() != null) {
                builder.append(getValue());
                builder.append('\n');
            }
            for (DomElement element : elementList) {
                for (String line : element.toXml().split("\\n")) {
                    builder.append("    ");
                    builder.append(line);
                    builder.append("\n");
                }
            }
            builder.append("</");
            builder.append(getName());
            builder.append(">");
        } else {
            if (getValue() != null) {
                builder.append(">");
                builder.append(getValue());
                builder.append("</");
                builder.append(getName());
            } else {
                builder.append("/");
            }
            builder.append(">");
        }

        return builder.toString();
    }

}