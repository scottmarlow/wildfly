/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.persistence.jipijapa.hibernate8;

import org.hibernate.bytecode.enhance.spi.DefaultEnhancementModel;
import org.hibernate.bytecode.enhance.spi.EnhancementOptions;
import org.hibernate.jpa.internal.enhance.EnhancingClassTransformerImpl;

/**
 * WildFlyClassTransformer is simple wrapper of Hibernate ORM EnhancingClassTransformerImpl that provides
 * bytecode enhancing of application deployment classes (e.g. entity classes).
 *
 * @author Scott Marlow
 */
public class WildFlyClassTransformer extends EnhancingClassTransformerImpl {

    public WildFlyClassTransformer() {
        // Enhance managed classes with lazy loading and dirty tracking, without
        // bidirectional association management or extended client enhancement.
        super(new DefaultEnhancementModel(), EnhancementOptions.of(true, true, false), null);
    }
}
