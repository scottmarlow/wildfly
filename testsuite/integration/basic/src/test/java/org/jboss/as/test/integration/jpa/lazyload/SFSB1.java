/*
 * Copyright 2023 Red Hat, Inc.
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

package org.jboss.as.test.integration.jpa.lazyload;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import jakarta.annotation.Resource;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateful;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.UserTransaction;

/**
 * stateful session bean
 *
 * @author Scott Marlow
 */
@Stateful
@TransactionManagement(TransactionManagementType.BEAN)
public class SFSB1 {
    @PersistenceContext
    EntityManager em;

    @Resource
    SessionContext sessionContext;

    public void createEmployee(String name, String address, int id) {

        Employee emp = new Employee();
        Company company = new Company();
        company.setId(id + 10000);
        emp.setId(id);
        emp.setAddress(address);
        emp.setName(name);
        emp.setCompany(company);

        UserTransaction tx1 = sessionContext.getUserTransaction();
        try {
            tx1.begin();
            em.joinTransaction();
            em.persist(emp);
            em.persist(company);
            tx1.commit();
        } catch (Exception e) {
            throw new RuntimeException("createEmployee couldn't start tx", e);
        }
    }

    public Employee getEmployee(int id) throws IOException, ClassNotFoundException {
        UserTransaction tx1 = sessionContext.getUserTransaction();
        try {
            tx1.begin();
            em.joinTransaction();

            Employee emp = em.find(Employee.class, id);
            // System.out.printf("emp company = " + emp.getCompany()); // uncomment to hack around failure
            // serialize the loaded employee
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream(stream);
            out.writeObject(emp);
            out.close();

            byte[] serialized = stream.toByteArray();
            stream.close();
            ByteArrayInputStream byteIn = new ByteArrayInputStream(serialized);
            ObjectInputStream in = new ObjectInputStream(byteIn);
            Employee emp2 = (Employee) in.readObject();
            // the following line fails unless previous call to getCompany is uncommented.
            // failure is  org.hibernate.LazyInitializationException: could not initialize proxy [org.jboss.as.test.integration.jpa.lazyload.Company#10001] - no Session
            System.out.printf("emp2 company = " + emp2.getCompany()); // this line fails with
            tx1.commit();
            return emp2;
        } catch (Exception e) {
            throw new RuntimeException("getEmployee couldn't start tx", e);
    }

    }
}
