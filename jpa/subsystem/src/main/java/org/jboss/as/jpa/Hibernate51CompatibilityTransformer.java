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

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
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

    private Hibernate51CompatibilityTransformer() {
    }

    public static Hibernate51CompatibilityTransformer getInstance() {
        return instance;
    }

    public byte[] transform(final ClassLoader loader, final String className, final Class<?> classBeingRedefined, final ProtectionDomain protectionDomain, final byte[] classfileBuffer) {
        ROOT_LOGGER.debugf("Hibernate51CompatibilityTransformer transforming deployment class %s from classloader %s", className, loader.toString());
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
                            ROOT_LOGGER.warnFlushModeTransformed(loader.toString(), className, owner);
                            name = "getHibernateFlushMode";
                            super.visitMethodInsn(opcode, owner, name, desc, itf);
                        }
                        // Modify calls to org.hibernate.Query.getFirstResult() to convert returned int, to Integer result.
                        // Modify calls to org.hibernate.Query.getMaxResults() to convert returned int, to Integer result, if returned int value is <= 0, null should be used
                        else if (opcode == Opcodes.INVOKEINTERFACE &&
                                owner.equals("org/hibernate/Query") &&
                                (name.equals("getFirstResult") || name.equals("getMaxResults")) &&
                                desc.equals("()Ljava/lang/Integer;")) {
                            ROOT_LOGGER.warnIntResultransformed(loader.toString(), className, name, owner);
                            super.visitMethodInsn(opcode, owner, name, "()I", itf); // call the orm 5.3 method that returns int, then convert to Integer
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", itf);
                        } else {
                            super.visitMethodInsn(opcode, owner, name, desc, itf);
                        }

                    }

                    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
                        // App References to Enum org.hibernate.FlushMode.NEVER (0) should be transformed to reference FlushMode.MANUAL (0) instead.
                        if (opcode == Opcodes.GETSTATIC &&
                                owner.equals("org/hibernate/FlushMode") &&
                                name.equals("NEVER") &&
                                desc.equals("Lorg/hibernate/FlushMode;")) {
                            ROOT_LOGGER.warnUseOfRemovedField(loader.toString(), className);
                            super.visitFieldInsn(opcode, owner, "MANUAL", desc);
                        } else {
                            super.visitFieldInsn(opcode, owner, name, desc);
                        }
                    }
                };
            }
        }, 0);
        return cv.toByteArray();
    }
}
