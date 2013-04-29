package org.jboss.as.jpa.management;

import org.jipijapa.management.spi.DynamicName;

/**
 * DynamicNameLookup
 *
 * @author Scott Marlow
 */
public class DynamicNameLookup implements DynamicName {

    private final String name;

    public DynamicNameLookup(String name) {
        this.name = name;
    }

    public static DynamicNameLookup dynamicNameLookup(String name) {
        return new DynamicNameLookup(name);
    }

    @Override
    public String getName() {
        return name;
    }
}
