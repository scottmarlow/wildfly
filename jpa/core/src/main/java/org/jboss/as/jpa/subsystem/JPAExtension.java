/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.jpa.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.jpa.JpaLogger.JPA_LOGGER;

import java.util.Collections;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.transform.RejectExpressionValuesTransformer;
import org.jboss.as.controller.transform.TransformersSubRegistration;
import org.jboss.as.jpa.config.Configuration;
import org.jboss.as.jpa.config.ExtendedPersistenceInheritance;
import org.jboss.as.jpa.persistenceprovider.PersistenceProviderLoader;
import org.jboss.as.jpa.processor.PersistenceProviderAdaptorLoader;
import org.jboss.as.jpa.spi.ManagementAdaptor;
import org.jboss.as.jpa.spi.PersistenceProviderAdaptor;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.ModuleLoadException;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * Domain extension used to initialize the JPA subsystem element handlers.
 *
 * @author Scott Marlow
 */
public class JPAExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "jpa";

    private static final JPASubsystemElementParser1_1 parser1_1 = new JPASubsystemElementParser1_1();
    private static final JPASubsystemElementParser1_0 parser1_0 = new JPASubsystemElementParser1_0();

    private static final String RESOURCE_NAME = JPAExtension.class.getPackage().getName() + ".LocalDescriptions";

    static StandardResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix) {
        String prefix = SUBSYSTEM_NAME + (keyPrefix == null ? "" : "." + keyPrefix);
        return new StandardResourceDescriptionResolver(prefix, RESOURCE_NAME, JPAExtension.class.getClassLoader(), true, true);
    }

    private static ModelNode createAddOperation(
            String defaultDatasource
    ) {
        final ModelNode update = new ModelNode();
        update.get(OP).set(ADD);
        update.get(OP_ADDR).add(SUBSYSTEM, SUBSYSTEM_NAME);
        update.get(CommonAttributes.DEFAULT_DATASOURCE).set(defaultDatasource != null ? defaultDatasource : "");
        return update;
    }

    private static ModelNode createAddOperation(
            String defaultDatasource,
            String providerModule,
            ExtendedPersistenceInheritance extendedPersistenceInheritanceType,
            Boolean vfs
    ) {
        final ModelNode update = new ModelNode();
        update.get(OP).set(ADD);
        update.get(OP_ADDR).add(SUBSYSTEM, SUBSYSTEM_NAME);
        update.get(CommonAttributes.DEFAULT_DATASOURCE).set(defaultDatasource != null ? defaultDatasource : "");

        if (providerModule != null) {
            update.get(CommonAttributes.DEFAULT_PROVIDERMODULE).set(providerModule);
        }
        if (extendedPersistenceInheritanceType != null) {
            update.get(CommonAttributes.DEFAULT_EXTENDEDPERSISTENCE_INHERITANCE).set(extendedPersistenceInheritanceType.toString());
        }
        if (vfs != null) {
            update.get(CommonAttributes.DEFAULT_VFS).set(vfs.toString());
        }
        return update;
    }

    private static final int MANAGEMENT_API_MAJOR_VERSION = 1;
    private static final int MANAGEMENT_API_MINOR_VERSION = 2;
    private static final int MANAGEMENT_API_MICRO_VERSION = 0;


    @Override
    public void initialize(ExtensionContext context) {
        SubsystemRegistration registration = context.registerSubsystem(SUBSYSTEM_NAME, MANAGEMENT_API_MAJOR_VERSION,
                MANAGEMENT_API_MINOR_VERSION, MANAGEMENT_API_MICRO_VERSION);
        final ManagementResourceRegistration nodeRegistration = registration.registerSubsystemModel(JPADefinition.INSTANCE);
        nodeRegistration.registerOperationHandler(DESCRIBE, GenericSubsystemDescribeHandler.INSTANCE, GenericSubsystemDescribeHandler.INSTANCE, false, OperationEntry.EntryType.PRIVATE);
        registration.registerXMLElementWriter(parser1_1);

        initializeTransformers_1_1_0(registration);

        try {
            PersistenceProviderLoader.loadDefaultProvider();
        } catch (ModuleLoadException e) {
            JPA_LOGGER.errorPreloadingDefaultProvider(e);
        }

        try {
            // load the default persistence provider adaptor
            PersistenceProviderAdaptor provider = PersistenceProviderAdaptorLoader.loadPersistenceAdapterModule(Configuration.ADAPTER_MODULE_DEFAULT);
            final ManagementAdaptor managementAdaptor = provider.getManagementAdaptor();
            if (managementAdaptor != null && context.isRuntimeOnlyRegistrationValid()) {
                final ManagementResourceRegistration jpaSubsystemDeployments = registration.registerDeploymentModel(JPADefinition.INSTANCE);

                managementAdaptor.register(jpaSubsystemDeployments, PersistenceUnitRegistryImpl.INSTANCE);
            }
        } catch (ModuleLoadException e) {
            JPA_LOGGER.errorPreloadingDefaultProviderAdaptor(e);
        }
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.JPA_1_1.getUriString(), parser1_1);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.JPA_1_0.getUriString(), parser1_0);
    }

    private void initializeTransformers_1_1_0(SubsystemRegistration subsystemRegistration) {
        ModelVersion oldVersion = ModelVersion.create(1, 0, 0);
        RejectExpressionValuesTransformer rejectDefaultDataSourceExpressions = new RejectExpressionValuesTransformer(JPADefinition.DEFAULT_DATASOURCE);
        TransformersSubRegistration reg = subsystemRegistration.registerModelTransformers(oldVersion, rejectDefaultDataSourceExpressions);
        reg.registerOperationTransformer(ADD, rejectDefaultDataSourceExpressions);
        reg.registerOperationTransformer(WRITE_ATTRIBUTE_OPERATION, rejectDefaultDataSourceExpressions.getWriteAttributeTransformer());
    }

    static class JPASubsystemElementParser1_1 implements XMLStreamConstants, XMLElementReader<List<ModelNode>>,
        XMLElementWriter<SubsystemMarshallingContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
            ModelNode subsystemAdd = null;
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                final Element element = Element.forName(reader.getLocalName());
                Namespace readerNS = Namespace.forUri(reader.getNamespaceURI());

                switch (element) {
                    case JPA: {
                        subsystemAdd = parseJPA(reader, readerNS);
                        break;
                    }
                    default: {
                        throw ParseUtils.unexpectedElement(reader);
                    }
                }
            }
            if (subsystemAdd == null) {
                throw ParseUtils.missingRequiredElement(reader, Collections.singleton(Element.JPA.getLocalName()));
            }
            list.add(subsystemAdd);
        }

        private ModelNode parseJPA(XMLExtendedStreamReader reader, Namespace readerNS) throws XMLStreamException {
            String dataSourceName = null;
            String providerModule = null;
            ExtendedPersistenceInheritance extendedPersistenceInheritanceType = null;
            Boolean vfs = null;

            int count = reader.getAttributeCount();
            for (int i = 0; i < count; i++) {
                final String value = reader.getAttributeValue(i);
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case DEFAULT_DATASOURCE_NAME: {
                        dataSourceName = value;
                        break;
                    }
                    case DEFAULT_PROVIDERMODULE_NAME:
                        providerModule = value;
                        break;
                    case DEFAULT_EXTENDEDPERSISTENCEINHERITANCE_NAME:
                        if (value == null || value.isEmpty()) {
                            extendedPersistenceInheritanceType = ExtendedPersistenceInheritance.SHALLOW;
                        } else {
                            extendedPersistenceInheritanceType = ExtendedPersistenceInheritance.valueOf(value);
                        }
                        break;
                    case DEFAULT_VFS_NAME:
                        if (value == null || value.isEmpty()) {
                            vfs = Boolean.TRUE;
                        } else {
                            vfs = Boolean.valueOf(value);
                        }
                        break;
                    default: {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                }
            }
            // Require no content
            ParseUtils.requireNoContent(reader);
            if (dataSourceName == null) {
                throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.DEFAULT_DATASOURCE_NAME));
            }
            return createAddOperation(dataSourceName, providerModule, extendedPersistenceInheritanceType, vfs);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void writeContent(final XMLExtendedStreamWriter writer, final SubsystemMarshallingContext context) throws
            XMLStreamException {

            ModelNode node = context.getModelNode();
            if (node.hasDefined(CommonAttributes.DEFAULT_DATASOURCE) ||
                    node.hasDefined(CommonAttributes.DEFAULT_PROVIDERMODULE) ||
                    node.hasDefined(CommonAttributes.DEFAULT_EXTENDEDPERSISTENCE_INHERITANCE) ||
                    node.hasDefined(CommonAttributes.DEFAULT_VFS)
                    ) {
                context.startSubsystemElement(Namespace.JPA_1_1.getUriString(), false);
                writer.writeStartElement(Element.JPA.getLocalName());
                if (node.hasDefined(CommonAttributes.DEFAULT_DATASOURCE)) {
                    JPADefinition.DEFAULT_DATASOURCE.marshallAsAttribute(node, writer);
                }
                if (node.hasDefined(CommonAttributes.DEFAULT_PROVIDERMODULE)) {
                    JPADefinition.DEFAULT_PROVIDERMODULE.marshallAsAttribute(node,writer);
                }
                if (node.hasDefined(CommonAttributes.DEFAULT_EXTENDEDPERSISTENCE_INHERITANCE)) {
                    JPADefinition.DEFAULT_EXTENDEDPERSISTENCE_INHERITANCE.marshallAsAttribute(node,writer);
                }
                if (node.hasDefined(CommonAttributes.DEFAULT_VFS)) {
                    JPADefinition.DEFAULT_VFS.marshallAsAttribute(node,writer);
                }
                writer.writeEndElement();
                writer.writeEndElement();
            } else {
                //TODO seems to be a problem with empty elements cleaning up the queue in FormattingXMLStreamWriter.runAttrQueue
                //context.startSubsystemElement(NewNamingExtension.NAMESPACE, true);
                context.startSubsystemElement(Namespace.JPA_1_1.getUriString(), false);
                writer.writeEndElement();
            }

        }
    }

    static class JPASubsystemElementParser1_0 implements XMLStreamConstants, XMLElementReader<List<ModelNode>>,
        XMLElementWriter<SubsystemMarshallingContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
            ModelNode subsystemAdd = null;

            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                final Element element = Element.forName(reader.getLocalName());
                Namespace readerNS = Namespace.forUri(reader.getNamespaceURI());

                switch (element) {
                    case JPA: {
                        subsystemAdd = parseJPA(reader, readerNS);
                        break;
                    }
                    default: {
                        throw ParseUtils.unexpectedElement(reader);
                    }
                }
            }
            if (subsystemAdd == null) {
                throw ParseUtils.missingRequiredElement(reader, Collections.singleton(Element.JPA.getLocalName()));
            }
            list.add(subsystemAdd);
        }

        private ModelNode parseJPA(XMLExtendedStreamReader reader, Namespace readerNS) throws XMLStreamException {
            String dataSourceName = null;
            int count = reader.getAttributeCount();
            for (int i = 0; i < count; i++) {
                final String value = reader.getAttributeValue(i);
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case DEFAULT_DATASOURCE_NAME: {
                        dataSourceName = value;
                        break;
                    }
                    default: {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                }
            }
            // Require no content
            ParseUtils.requireNoContent(reader);
            if (dataSourceName == null) {
                throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.DEFAULT_DATASOURCE_NAME));
            }
            return createAddOperation(dataSourceName);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void writeContent(final XMLExtendedStreamWriter writer, final SubsystemMarshallingContext context) throws
            XMLStreamException {

            ModelNode node = context.getModelNode();
            if (node.hasDefined(CommonAttributes.DEFAULT_DATASOURCE)) {
                context.startSubsystemElement(Namespace.JPA_1_0.getUriString(), false);
                writer.writeStartElement(Element.JPA.getLocalName());
                JPADefinition.DEFAULT_DATASOURCE.marshallAsAttribute(node, writer);
                writer.writeEndElement();
                writer.writeEndElement();
            } else {
                //TODO seems to be a problem with empty elements cleaning up the queue in FormattingXMLStreamWriter.runAttrQueue
                //context.startSubsystemElement(NewNamingExtension.NAMESPACE, true);
                context.startSubsystemElement(Namespace.JPA_1_0.getUriString(), false);
                writer.writeEndElement();
            }

        }
    }

}
