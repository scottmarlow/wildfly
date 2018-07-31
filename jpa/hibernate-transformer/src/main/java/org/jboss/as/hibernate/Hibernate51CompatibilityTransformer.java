/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.hibernate;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.Set;

import org.jboss.modules.ModuleClassLoader;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.TraceClassVisitor;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * A class file transformer which makes deployment classes written for Hibernate 5.1 be compatible with Hibernate 5.3.
 *
 * @deprecated
 */
public class Hibernate51CompatibilityTransformer implements ClassFileTransformer {

    private static final Hibernate51CompatibilityTransformer instance = new Hibernate51CompatibilityTransformer();
    private static final boolean disableAmbiguousChanges = Boolean.parseBoolean(
            WildFlySecurityManager.getPropertyPrivileged("Hibernate51CompatibilityTransformer.disableAmbiguousChanges", "false"));
    private static final boolean showTransformedClass = Boolean.parseBoolean(
            WildFlySecurityManager.getPropertyPrivileged("Hibernate51CompatibilityTransformer.showTransformedClass", "false"));
    private static final String markerAlreadyTransformed = "$_org_jboss_as_hibernate_Hibernate51CompatibilityTransformer_transformed_$";
    public static Hibernate51CompatibilityTransformer getInstance() {
        return instance;
    }

    @Override
    public byte[] transform(final ClassLoader loader, final String className, final Class<?> classBeingRedefined, final ProtectionDomain protectionDomain, final byte[] classfileBuffer) {
        TransformerLogger.LOGGER.debugf("Hibernate51CompatibilityTransformer transforming deployment class '%s' from '%s'", className, getModuleName(loader));
        final Set<String> parentClassesAndInterfaces = new HashSet<>();
        collectClassesAndInterfaces( parentClassesAndInterfaces, loader, className );
        TransformerLogger.LOGGER.tracef("Class %s extends or implements %s", className, parentClassesAndInterfaces);
        final TransformedState transformedState = new TransformedState();
        final ClassReader classReader = new ClassReader(classfileBuffer);
        final ClassWriter classWriter = new ClassWriter(classReader, 0);
        ClassVisitor traceClassVisitor = classWriter;
        try {
            if (showTransformedClass) {
                traceClassVisitor = new TraceClassVisitor(classWriter, new PrintWriter(new File(className.replace('/', '_') + ".asm")));
            }
        } catch (FileNotFoundException ignored) {

        }

        classReader.accept(new ClassVisitor(Opcodes.ASM6, traceClassVisitor) {

            // visit application class and add Transformed marker interface or mark class as already transformed
            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                // clear per class state
                transformedState.clear();
                cv.visit(version, access, name, signature, superName, interfaces);
            }

            @Override
            public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
                // check if class has already been modified
                if(markerAlreadyTransformed.equals(name) &&
                        desc.equals("Z")) {
                    transformedState.setAlreadyTransformed(true);
                }
                return cv.visitField(access, name, desc, signature, value);
            }

            @Override
            public void visitEnd() {
                if (transformedState.transformationsMade()) {
                    cv.visitField(ACC_PUBLIC + ACC_STATIC, markerAlreadyTransformed, "Z", null, null).visitEnd();
                }
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {

                // Handle changing SessionImplementor parameter to SharedSessionContractImplementor in the following methods.
                // NOTE: handle each of the different checked interfaces, as if a class could implement multiple checked interfaces since that is possible.

                boolean rewriteSessionImplementor = false;

                TransformerLogger.LOGGER.tracef("method %s, description %s, signature %s", name, desc, signature);
                if (parentClassesAndInterfaces.contains( "org/hibernate/usertype/UserType" )) {
                    if (name.equals("nullSafeGet") &&
                            "(Ljava/sql/ResultSet;[Ljava/lang/String;Lorg/hibernate/engine/spi/SessionImplementor;Ljava/lang/Object;)Ljava/lang/Object;".equals(desc)) {
                        desc = replaceSessionImplementor( desc );
                    } else if (name.equals("nullSafeSet") &&
                            "(Ljava/sql/PreparedStatement;Ljava/lang/Object;ILorg/hibernate/engine/spi/SessionImplementor;)V".equals(desc)) {
                        desc = replaceSessionImplementor( desc );
                    }
                    transformedState.setClassTransformed(true);
                    rewriteSessionImplementor = true;
                }

                if (parentClassesAndInterfaces.contains( "org/hibernate/usertype/CompositeUserType" )) {
                    if (name.equals("nullSafeGet") &&
                            "(Ljava/sql/ResultSet;[Ljava/lang/String;Lorg/hibernate/engine/spi/SessionImplementor;Ljava/lang/Object;)Ljava/lang/Object;".equals(desc)) {
                        desc = replaceSessionImplementor( desc );
                    } else if (name.equals("nullSafeSet") &&
                            "(Ljava/sql/PreparedStatement;Ljava/lang/Object;ILorg/hibernate/engine/spi/SessionImplementor;)V".equals(desc)) {
                        desc = replaceSessionImplementor( desc );
                    }
                    else if (name.equals("assemble") &&
                            "(Ljava/io/Serializable;Lorg/hibernate/engine/spi/SessionImplementor;Ljava/lang/Object;)Ljava/lang/Object;".equals(desc)) {
                        desc = replaceSessionImplementor( desc );
                    } else if (name.equals("disassemble") &&
                            "(Ljava/lang/Object;Lorg/hibernate/engine/spi/SessionImplementor;)Ljava/io/Serializable;".equals(desc)) {
                        desc = replaceSessionImplementor( desc );
                    }
                    else if (name.equals("replace") &&
                            "(Ljava/lang/Object;Ljava/lang/Object;Lorg/hibernate/engine/spi/SessionImplementor;Ljava/lang/Object;)Ljava/lang/Object;".equals(desc)) {
                        desc = replaceSessionImplementor( desc );
                    }

                    transformedState.setClassTransformed(true);
                    rewriteSessionImplementor = true;
                }

                if (parentClassesAndInterfaces.contains( "org/hibernate/usertype/UserCollectionType" )) {
                    if (name.equals("instantiate") &&
                            "(Lorg/hibernate/engine/spi/SessionImplementor;Lorg/hibernate/persister/collection/CollectionPersister;)Lorg/hibernate/collection/spi/PersistentCollection;".equals(desc)) {
                        desc = replaceSessionImplementor( desc );
                    } else if (name.equals("replaceElements") &&
                            "(Ljava/lang/Object;Ljava/lang/Object;Lorg/hibernate/persister/collection/CollectionPersister;Ljava/lang/Object;Ljava/util/Map;Lorg/hibernate/engine/spi/SessionImplementor;)Ljava/lang/Object;".equals(desc)) {
                        desc = replaceSessionImplementor( desc );
                    } else if (name.equals("wrap") &&
                            "(Lorg/hibernate/engine/spi/SessionImplementor;Ljava/lang/Object;)Lorg/hibernate/collection/spi/PersistentCollection;".equals(desc)) {
                        desc = replaceSessionImplementor( desc );
                    }

                    transformedState.setClassTransformed(true);
                    rewriteSessionImplementor = true;
                }

                if (parentClassesAndInterfaces.contains( "org/hibernate/usertype/UserVersionType" )) {
                    if (name.equals("seed") &&
                            "(Lorg/hibernate/engine/spi/SessionImplementor;)Ljava/lang/Object;".equals(desc)) {
                        desc = replaceSessionImplementor( desc );
                    } else if (name.equals("next") &&
                            "(Ljava/lang/Object;Lorg/hibernate/engine/spi/SessionImplementor;)Ljava/lang/Object;".equals(desc)) {
                        desc = replaceSessionImplementor( desc );
                    }

                    transformedState.setClassTransformed(true);
                    rewriteSessionImplementor = true;
                }

                if (parentClassesAndInterfaces.contains( "org/hibernate/type/Type" )) {
                    if (name.equals("assemble") &&
                            "(Ljava/io/Serializable;Lorg/hibernate/engine/spi/SessionImplementor;Ljava/lang/Object;)Ljava/lang/Object;".equals(desc)) {
                        desc = replaceSessionImplementor( desc );
                    } else if (name.equals("disassemble") &&
                            "(Ljava/lang/Object;Lorg/hibernate/engine/spi/SessionImplementor;)Ljava/io/Serializable;".equals(desc)) {
                        desc = replaceSessionImplementor( desc );
                    } else if (name.equals("beforeAssemble") &&
                            "(Ljava/io/Serializable;Lorg/hibernate/engine/spi/SessionImplementor;)V".equals(desc)) {
                        desc = replaceSessionImplementor( desc );
                    } else if (name.equals("hydrate") &&
                            "(Ljava/sql/ResultSet;[Ljava/lang/String;Lorg/hibernate/engine/spi/SessionImplementor;Ljava/lang/Object;)Ljava/lang/Object;".equals(desc)) {
                        desc = replaceSessionImplementor( desc );
                    } else if (name.equals("isDirty") &&
                            "(Ljava/lang/Object;Ljava/lang/Object;Lorg/hibernate/engine/spi/SessionImplementor;)Z".equals(desc)) {
                        desc = replaceSessionImplementor( desc );
                    } else if (name.equals("isDirty") &&
                            "(Ljava/lang/Object;Ljava/lang/Object;[ZLorg/hibernate/engine/spi/SessionImplementor;)Z".equals(desc)) {
                        desc = replaceSessionImplementor( desc );
                    } else if (name.equals("isModified") &&
                            "(Ljava/lang/Object;Ljava/lang/Object;[ZLorg/hibernate/engine/spi/SessionImplementor;)Z".equals(desc)) {
                        desc = replaceSessionImplementor( desc );
                    } else if (name.equals("nullSafeGet") &&
                            "(Ljava/sql/ResultSet;[Ljava/lang/String;Lorg/hibernate/engine/spi/SessionImplementor;Ljava/lang/Object;)Ljava/lang/Object;".equals(desc)) {
                        desc = replaceSessionImplementor( desc );
                    } else if (name.equals("nullSafeGet") &&
                            "(Ljava/sql/ResultSet;Ljava/lang/String;Lorg/hibernate/engine/spi/SessionImplementor;Ljava/lang/Object;)Ljava/lang/Object;".equals(desc)) {
                        desc = replaceSessionImplementor( desc );
                    } else if (name.equals("nullSafeSet") &&
                            "(Ljava/sql/PreparedStatement;Ljava/lang/Object;I[ZLorg/hibernate/engine/spi/SessionImplementor;)V".equals(desc)) {
                        desc = replaceSessionImplementor( desc );
                    } else if (name.equals("nullSafeSet") &&
                            "(Ljava/sql/PreparedStatement;Ljava/lang/Object;ILorg/hibernate/engine/spi/SessionImplementor;)V".equals(desc)) {
                        desc = replaceSessionImplementor( desc );
                    } else if (name.equals("replace") &&
                            "(Ljava/lang/Object;Ljava/lang/Object;Lorg/hibernate/engine/spi/SessionImplementor;Ljava/lang/Object;Ljava/util/Map;)Ljava/lang/Object;".equals(desc)) {
                        desc = replaceSessionImplementor( desc );
                    } else if (name.equals("replace") &&
                            "(Ljava/lang/Object;Ljava/lang/Object;Lorg/hibernate/engine/spi/SessionImplementor;Ljava/lang/Object;Ljava/util/Map;Lorg/hibernate/type/ForeignKeyDirection;)Ljava/lang/Object;".equals(desc)) {
                        desc = replaceSessionImplementor( desc );
                    } else if (name.equals("resolve") &&
                            "(Ljava/lang/Object;Lorg/hibernate/engine/spi/SessionImplementor;Ljava/lang/Object;)Ljava/lang/Object;".equals(desc)) {
                        desc = replaceSessionImplementor( desc );
                    } else if (name.equals("resolve") &&
                            "(Ljava/lang/Object;Lorg/hibernate/engine/spi/SessionImplementor;Ljava/lang/Object;Ljava/lang/Boolean;)Ljava/lang/Object;".equals(desc)) {
                        desc = replaceSessionImplementor( desc );
                    } else if (name.equals("semiResolve") &&
                            "(Ljava/lang/Object;Lorg/hibernate/engine/spi/SessionImplementor;Ljava/lang/Object;)Ljava/lang/Object;".equals(desc)) {
                        desc = replaceSessionImplementor( desc );
                    }

                    transformedState.setClassTransformed(true);
                    rewriteSessionImplementor = true;
                }

                if (parentClassesAndInterfaces.contains( "org/hibernate/type/SingleColumnType" )) {
                    if (name.equals("nullSafeGet") &&
                            "(Ljava/sql/ResultSet;Ljava/lang/String;Lorg/hibernate/engine/spi/SessionImplementor;)Ljava/lang/Object;".equals(desc)) {
                        desc = replaceSessionImplementor( desc );
                    } else if (name.equals("get") &&
                            "(Ljava/sql/ResultSet;Ljava/lang/String;Lorg/hibernate/engine/spi/SessionImplementor;)Ljava/lang/Object;".equals(desc)) {
                        desc = replaceSessionImplementor( desc );
                    } else if (name.equals("set") &&
                            "(Ljava/sql/PreparedStatement;Ljava/lang/Object;ILorg/hibernate/engine/spi/SessionImplementor;)V".equals(desc)) {
                        desc = replaceSessionImplementor( desc );
                    }

                    transformedState.setClassTransformed(true);
                    rewriteSessionImplementor = true;
                }

                if (parentClassesAndInterfaces.contains( "org/hibernate/type/AbstractStandardBasicType" )) {
                    if (name.equals("get") &&
                            "(Ljava/sql/ResultSet;Ljava/lang/String;Lorg/hibernate/engine/spi/SessionImplementor;)Ljava/lang/Object;".equals(desc)) {
                        desc = replaceSessionImplementor( desc );
                    } else if (name.equals("nullSafeGet") &&
                            "(Ljava/sql/ResultSet;Ljava/lang/String;Lorg/hibernate/engine/spi/SessionImplementor;)Ljava/lang/Object;".equals(desc)) {
                        desc = replaceSessionImplementor( desc );
                    } else if (name.equals("set") &&
                            "(Ljava/sql/PreparedStatement;Ljava/lang/Object;ILorg/hibernate/engine/spi/SessionImplementor;)V".equals(desc)) {
                        desc = replaceSessionImplementor( desc );
                    }

                    transformedState.setClassTransformed(true);
                    rewriteSessionImplementor = true;
                }

                if (parentClassesAndInterfaces.contains( "org/hibernate/type/ProcedureParameterExtractionAware" )) {
                    if ( name.equals( "extract" )
                            && ( desc.startsWith(
                                    "(Ljava/sql/CallableStatement;ILorg/hibernate/engine/spi/SessionImplementor;)" )
                                    || desc.startsWith(
                                            "(Ljava/sql/CallableStatement;[Ljava/lang/String;Lorg/hibernate/engine/spi/SessionImplementor;)" ) ) ) {
                        desc = replaceSessionImplementor( desc );
                        transformedState.setClassTransformed(true);
                    }
                }

                if (parentClassesAndInterfaces.contains( "org/hibernate/type/ProcedureParameterNamedBinder" )) {
                    if ( name.equals( "nullSafeSet" ) &&
                            "(Ljava/sql/CallableStatement;Ljava/lang/Object;Ljava/lang/String;Lorg/hibernate/engine/spi/SessionImplementor;)V".equals( desc )) {
                        desc = replaceSessionImplementor( desc );
                        transformedState.setClassTransformed(true);
                    }
                }

                if ( parentClassesAndInterfaces.contains( "org/hibernate/type/VersionType" ) ) {
                    if ( name.equals( "seed" ) &&
                            desc.startsWith( "(Lorg/hibernate/engine/spi/SessionImplementor;)" ) ) {
                        desc = replaceSessionImplementor( desc );
                    }
                    else if ( name.equals( "next" ) &&
                            desc.contains( "Lorg/hibernate/engine/spi/SessionImplementor;" ) ) {
                        desc = replaceSessionImplementor( desc );
                        transformedState.setClassTransformed(true);
                    }
                }

/* the PersistentBag constructor changes are not currently needed, instead, will use an ORM change.
   TODO:  remove this block before merging to WildFly master.
                if (extendsPersistentBag) {
                    if (name.equals("<init>") &&
                            "(Lorg/hibernate/engine/spi/SessionImplementor;)V".equals(desc)) {
                        desc = "(Lorg/hibernate/engine/spi/SharedSessionContractImplementor;)V";
                    }
                    else if (name.equals("<init>") &&
                            "(Lorg/hibernate/engine/spi/SessionImplementor;Ljava/util/Collection;)V".equals(desc)) {
                        desc = "(Lorg/hibernate/engine/spi/SharedSessionContractImplementor;Ljava/util/Collection;)V";
                    }
                    else if (name.equals("<init>") &&
                            "(Lorg/hibernate/engine/spi/SessionImplementor;Ljava/util/List;)V".equals(desc)) {
                        desc = "(Lorg/hibernate/engine/spi/SharedSessionContractImplementor;Ljava/util/List;)V";
                    }
                }
*/
                // TODO: org.hibernate.collection.spi.PersistentCollection

                return new MethodAdapter(rewriteSessionImplementor, Opcodes.ASM6, super.visitMethod(access, name, desc,
                        signature, exceptions), loader, className, transformedState);
            }
        }, 0);
        if (!transformedState.transformationsMade()) {
            // no change was actually made, indicate so by returning null
            return null;
        }
        return classWriter.toByteArray();
    }

    private static String getModuleName(ClassLoader loader) {
        if (loader == null) {
            return "(null)";
        }
        if (loader instanceof ModuleClassLoader) {
            return ((ModuleClassLoader) loader).getName();
        }
        return loader.toString();
    }

    protected static class MethodAdapter extends MethodVisitor {

        private final boolean rewriteSessionImplementor;
        private final TransformedState transformedState;
        private final MethodVisitor mv;
        private final ClassLoader loader;
        private final String className;

        private MethodAdapter(boolean rewriteSessionImplementor, int api, MethodVisitor mv,
                              final ClassLoader loader, final String className, TransformedState transformedState) {
            super(api, mv);
            this.rewriteSessionImplementor = rewriteSessionImplementor;
            this.mv = mv;
            this.loader = loader;
            this.className = className;
            this.transformedState = transformedState;
        }


        // Change call to org.hibernate.BasicQueryContract.getFlushMode() to instead call BasicQueryContract.getHibernateFlushMode().
        // Change call to org.hibernate.Session.getFlushMode, to instead call Session.getHibernateFlushMode()
        // Calls to Hibernate ORM 5.3 getFlushMode(), will not be changed as the desc will not match (desc == "()Ljavax.persistence.FlushModeType;")
        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            if (rewriteSessionImplementor &&
                    ( opcode == Opcodes.INVOKESPECIAL || opcode == Opcodes.INVOKEVIRTUAL ) &&
                    owner.startsWith( "org/hibernate/" ) ) {
                // if we have a user type calling a method from org.hibernate, we rewrite it to use SharedSessionContractImplementor
                TransformerLogger.LOGGER.debugf("Deprecated Hibernate51CompatibilityTransformer transformed application classes in '%s', " +
                        "class '%s' is calling method %s.%s, which must be changed to use SharedSessionContractImplementor as parameter.",
                getModuleName(loader), className, owner, name);
                mv.visitMethodInsn(opcode, owner, name, replaceSessionImplementor( desc ), itf);
                transformedState.setClassTransformed(true);
            } else if (opcode == Opcodes.INVOKEINTERFACE &&
                    (owner.equals("org/hibernate/Session") || owner.equals("org/hibernate/BasicQueryContract"))
                    && name.equals("getFlushMode") && desc.equals("()Lorg/hibernate/FlushMode;")) {
                TransformerLogger.LOGGER.debugf("Deprecated Hibernate51CompatibilityTransformer transformed application classes in '%s', " +
                                "class '%s' is calling %s.getFlushMode, which must be changed to call getHibernateFlushMode().",
                        getModuleName(loader), className, owner);
                name = "getHibernateFlushMode";
                mv.visitMethodInsn(opcode, owner, name, desc, itf);
                transformedState.setClassTransformed(true);
            } else if (opcode == Opcodes.INVOKEINTERFACE &&
                    owner.equals("org/hibernate/Query") &&
                    name.equals("getFirstResult") &&
                    desc.equals("()Ljava/lang/Integer;")) {
                TransformerLogger.LOGGER.debugf("Deprecated Hibernate51CompatibilityTransformer transformed application classes in '%s', " +
                                "class '%s' is calling %s.%s, which must be changed to expect int result, instead of Integer.",
                        getModuleName(loader), className, name, owner);
                TransformerLogger.LOGGER.debugf("Deprecated Hibernate51CompatibilityTransformer transformed application classes in '%s', " +
                                "class '%s', is calling org.hibernate.Query.getFirstResult, which must be changed to call getHibernateFirstResult() " +
                                "so null can be returned when the value is uninitialized. Please note that if a negative value was set using " +
                                "org.hibernate.Query.setFirstResult, then getHibernateFirstResult() will return 0.",
                        getModuleName(loader), className);
                name = "getHibernateFirstResult";
                mv.visitMethodInsn(opcode, owner, name, desc, itf);
                transformedState.setClassTransformed(true);
            } else if (opcode == Opcodes.INVOKEINTERFACE &&
                    owner.equals("org/hibernate/Query") &&
                    name.equals("getMaxResults") &&
                    desc.equals("()Ljava/lang/Integer;")) {
                TransformerLogger.LOGGER.debugf("Deprecated Hibernate51CompatibilityTransformer transformed application classes in '%s', " +
                                "class '%s' is calling %s.%s, which must be changed to expect int result, instead of Integer.",
                        getModuleName(loader), className, name, owner);
                TransformerLogger.LOGGER.debugf("Deprecated Hibernate51CompatibilityTransformer transformed application classes in '%s', " +
                                "class '%s', is calling org.hibernate.Query.getMaxResults, which must be changed to call getHibernateMaxResults() " +
                                "so that null will be returned when the value is uninitialized or ORM 5.1 org.hibernate.Query#setMaxResults was " +
                                "used to set a value <= 0"
                        , getModuleName(loader), className);
                name = "getHibernateMaxResults";
                mv.visitMethodInsn(opcode, owner, name, desc, itf);
                transformedState.setClassTransformed(true);
            } else if (!disableAmbiguousChanges && opcode == Opcodes.INVOKEINTERFACE &&
                    owner.equals("org/hibernate/Query") &&
                    name.equals("setFirstResult") &&
                    desc.equals("(I)Lorg/hibernate/Query;")) {
                TransformerLogger.LOGGER.debugf("Deprecated Hibernate51CompatibilityTransformer transformed application classes in '%s', " +
                                "class '%s', is calling org.hibernate.Query.setFirstResult, which must be changed to call setHibernateFirstResult() " +
                                "so setting a value < 0 results in pagination starting with the 0th row as was done in Hibernate ORM 5.1 " +
                                "(instead of throwing IllegalArgumentException as specified by JPA)."
                        , getModuleName(loader), className);
                name = "setHibernateFirstResult";
                mv.visitMethodInsn(opcode, owner, name, desc, itf);
                transformedState.setClassTransformed(true);
            } else if (!disableAmbiguousChanges && opcode == Opcodes.INVOKEINTERFACE &&
                    owner.equals("org/hibernate/Query") &&
                    name.equals("setMaxResults") &&
                    desc.equals("(I)Lorg/hibernate/Query;")) {
                TransformerLogger.LOGGER.debugf("Deprecated Hibernate51CompatibilityTransformer transformed application classes in '%s', " +
                                "class '%s', is calling org.hibernate.Query.setMaxResults, which must be changed to call setHibernateMaxResults() " +
                                "so that values <= 0 are the same as uninitialized."
                        , getModuleName(loader), className);
                name = "setHibernateMaxResults";
                mv.visitMethodInsn(opcode, owner, name, desc, itf);
                transformedState.setClassTransformed(true);
            } else {
                mv.visitMethodInsn(opcode, owner, name, desc, itf);
            }

        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
            // App References to Enum org.hibernate.FlushMode.NEVER (0) should be transformed to reference FlushMode.MANUAL (0) instead.
            if (opcode == Opcodes.GETSTATIC &&
                    owner.equals("org/hibernate/FlushMode") &&
                    name.equals("NEVER") &&
                    desc.equals("Lorg/hibernate/FlushMode;")) {
                TransformerLogger.LOGGER.debugf("Deprecated Hibernate51CompatibilityTransformer transformed application classes in '%s', " +
                                "class '%s' is using org.hibernate.FlushMode.NEVER, change to org.hibernate.FlushMode.MANUAL."
                        , getModuleName(loader), className);
                mv.visitFieldInsn(opcode, owner, "MANUAL", desc);
                transformedState.setClassTransformed(true);
            } else {
                mv.visitFieldInsn(opcode, owner, name, desc);
            }
        }
    }

    private static String replaceSessionImplementor(String desc) {
        return desc.replace( "Lorg/hibernate/engine/spi/SessionImplementor;",
                "Lorg/hibernate/engine/spi/SharedSessionContractImplementor;" );
    }

    private void collectClassesAndInterfaces(Set<String> classesAndInterfaces, ClassLoader classLoader, String className) {
        if ( className == null || "java/lang/Object".equals( className ) ) {
            return;
        }

        try ( InputStream is = classLoader.getResourceAsStream( className.replace('.', '/') + ".class" ) ) {
            ClassReader classReader = new ClassReader( is );
            classReader.accept( new ClassVisitor( Opcodes.ASM6 ) {

                @Override
                public void visit(int version, int access, String name, String signature,
                        String superName, String[] interfaces) {
                    if ( interfaces != null ) {
                        for ( String interfaceName : interfaces ) {
                            classesAndInterfaces.add( interfaceName );
                            collectClassesAndInterfaces( classesAndInterfaces, classLoader, interfaceName );
                        }
                    }

                    classesAndInterfaces.add( superName );
                    collectClassesAndInterfaces( classesAndInterfaces, classLoader, superName );
                }

                @Override
                public void visitInnerClass(String name, String outerName, String innerName, int access) {
                    if (innerName != null) {
                        classesAndInterfaces.add(innerName);
                    }
                }

            }, 0 );
        }
        catch (IOException e) {
            TransformerLogger.LOGGER.warn( "Unable to open class file %1$s", className, e );
        }
    }

    private static class TransformedState {
        private boolean classTransformed;
        private boolean alreadyTransformed;

        public void setClassTransformed(boolean classTransformed) {
            this.classTransformed = classTransformed;
        }

        public void setAlreadyTransformed(boolean alreadyTransformed) {
            this.alreadyTransformed = alreadyTransformed;
        }

        public boolean transformationsMade() {
            return !alreadyTransformed && classTransformed;
        }

        public void clear() {
            alreadyTransformed = classTransformed = false;
        }
    }
}

