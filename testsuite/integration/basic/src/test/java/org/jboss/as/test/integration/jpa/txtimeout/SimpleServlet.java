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
package org.jboss.as.test.integration.jpa.txtimeout;

import java.io.IOException;
import java.io.Writer;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.EJBTransactionRolledbackException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

/**
 * @author Scott Marlow
 *
 */

@WebServlet(name="SimpleServlet", urlPatterns={"/simple"})
public class SimpleServlet extends HttpServlet {

    @Resource
    private UserTransaction userTransaction;

    @EJB
    SFSB1 sfsb1;

    private static int THREE_SECONDS = 3;


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String msg = req.getParameter("input");

        Writer writer = resp.getWriter();

        int count = 0;
        try {
            userTransaction.setTransactionTimeout(THREE_SECONDS);
            userTransaction.begin();
            boolean notRolledBackException = false;
            while (!notRolledBackException) {
                try {
                    System.out.println("repeating invocation, count=" + count);
                    count++;
                    System.out.println("about to create entity " + count);
                    sfsb1.createEmployee("name" + count, "address" + count, count);
                    // sfsb1.getEmployeeUntilTxTimeout();
                } catch (Exception exception) {
                    if (exception instanceof EJBTransactionRolledbackException) {
                        if (sfsb1.getEmployee(count) != null) {
                            writer.write("failed as entity " + count +", was not rolled back");
                            return;
                        }
                        System.out.println("ignoring expected RollbackException by repeating invocation, count=" + count);
                    } else {
                        System.out.println("caught exception that we didn't expect: " + exception.getClass().getName() + ", " + exception.getMessage() +
                                ", count=" + count);

                        notRolledBackException = true;
                    }
                    // try again until its not RollbackException:
                }
            }
        } catch (NotSupportedException e) {
            e.printStackTrace();
        } catch (SystemException e) {
            e.printStackTrace();
        } finally {
            try {
                userTransaction.rollback();
            } catch (SystemException e) {
                e.printStackTrace();
            }
        }
        if (sfsb1.getEmployee(count) != null) {
            writer.write("failed as entity " + count +", was not rolled back");
        }
        else {
            writer.write("success");
        }
    }
}
