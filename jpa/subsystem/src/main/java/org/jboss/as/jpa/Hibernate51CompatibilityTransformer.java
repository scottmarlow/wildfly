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

package org.jboss.as.jpa;

import static org.jboss.as.jpa.messages.JpaLogger.ROOT_LOGGER;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

import org.jboss.modules.ModuleClassLoader;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * A class file transformer which makes deployment classes written for Hibernate 5.1 be compatible with Hibernate 5.3.
 * <p>
 * TODO: remove asm-commmons artifact from WF as we aren't using it.  Do this before creating a pr for this change.
 *
 * @deprecated
 */
public class Hibernate51CompatibilityTransformer implements ClassFileTransformer {

    private static final Hibernate51CompatibilityTransformer instance = new Hibernate51CompatibilityTransformer();
    private static final boolean disableAmbiguousChanges = Boolean.getBoolean("Hibernate51CompatibilityTransformer.disableAmbiguousChanges");

    private Hibernate51CompatibilityTransformer() {
    }

    public static Hibernate51CompatibilityTransformer getInstance() {
        return instance;
    }

    public byte[] transform(final ClassLoader loader, final String className, final Class<?> classBeingRedefined, final ProtectionDomain protectionDomain, final byte[] classfileBuffer) {
        ROOT_LOGGER.infoTransformingApplicationClasses(className, getModuleName(loader));
        final ClassReader classReader = new ClassReader(classfileBuffer);
        final ClassWriter cv = new ClassWriter(classReader, 0);
        classReader.accept(new ClassVisitor(Opcodes.ASM6, cv) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM6, super.visitMethod(access, name, desc, signature, exceptions)) {
                    // Change call to org.hibernate.BasicQueryContract.getFlushMode() to instead call BasicQueryContract.getHibernateFlushMode().
                    // Change call to org.hibernate.Session.getFlushMode, to instead call Session.getHibernateFlushMode()
                    // Calls to Hibernate ORM 5.3 getFlushMode(), will not be changed as the desc will not match (desc == "()Ljavax.persistence.FlushModeType;")
                    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                        if (opcode == Opcodes.INVOKEINTERFACE &&
                                (owner.equals("org/hibernate/Session") || owner.equals("org/hibernate/BasicQueryContract"))
                                && name.equals("getFlushMode") && desc.equals("()Lorg/hibernate/FlushMode;")) {
                            ROOT_LOGGER.warnFlushModeTransformed(getModuleName(loader), className, owner);
                            name = "getHibernateFlushMode";
                            super.visitMethodInsn(opcode, owner, name, desc, itf);
                        }
                        // Modify calls to org.hibernate.Query.getFirstResult() to convert returned int, to Integer result.
                        // Modify calls to org.hibernate.Query.getMaxResults() to convert returned int, to Integer result, if returned int value is <= 0, null should be used
                        else if (opcode == Opcodes.INVOKEINTERFACE &&
                                owner.equals("org/hibernate/Query") &&
                                (name.equals("getFirstResult") || name.equals("getMaxResults")) &&
                                desc.equals("()Ljava/lang/Integer;")) {
                            ROOT_LOGGER.warnIntResultTransformed(getModuleName(loader), className, name, owner);
                            super.visitMethodInsn(opcode, owner, name, "()I", itf); // call the orm 5.3 method that returns int, then convert to Integer
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", itf);
                        } else if (!disableAmbiguousChanges && opcode == Opcodes.INVOKEINTERFACE &&
                                owner.equals("org/hibernate/Query") &&
                                name.equals("setMaxResults") &&
                                desc.equals("(I)Lorg/hibernate/Query;")) {
                            ROOT_LOGGER.warnSetMaxRowsCallTransformed(getModuleName(loader), className);
                            super.visitInsn(Opcodes.DUP); // save extra copy of value on stack
                            Label label2 = new Label();
                            Label label3 = new Label();
                            super.visitJumpInsn(Opcodes.IFGE, label2); // branch to label2 if greater than/equal to zero
                            super.visitInsn(Opcodes.POP);  // discard saved extra copy
                            super.visitInsn(Opcodes.ICONST_0); // replace negative int value with 0
                            super.visitJumpInsn(Opcodes.GOTO, label3);
                            super.visitLabel(label2);
                            super.visitInsn(Opcodes.POP); // restore saved int value
                            super.visitLabel(label3);
                            super.visitMethodInsn(opcode, owner, name, desc, itf);
                        } else

                        {
                            super.visitMethodInsn(opcode, owner, name, desc, itf);
                        }

                    }

                    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
                        // App References to Enum org.hibernate.FlushMode.NEVER (0) should be transformed to reference FlushMode.MANUAL (0) instead.
                        if (opcode == Opcodes.GETSTATIC &&
                                owner.equals("org/hibernate/FlushMode") &&
                                name.equals("NEVER") &&
                                desc.equals("Lorg/hibernate/FlushMode;")) {
                            ROOT_LOGGER.warnUseOfRemovedField(getModuleName(loader), className);
                            super.visitFieldInsn(opcode, owner, "MANUAL", desc);
                        } else {
                            super.visitFieldInsn(opcode, owner, name, desc);
                        }
                    }
                }

                        ;
            }
        }, 0);
        return cv.toByteArray();
    }

    private static final String getModuleName(ClassLoader loader) {
        if (loader instanceof ModuleClassLoader) {
            return ((ModuleClassLoader) loader).getName();
        }
        return loader.toString();
    }
}
