package org.jboss.as.test.integration.jpa.hhh18901;

import java.util.Calendar;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceUnit;
import jakarta.persistence.EntityManagerFactory;

/**
 * CdiTestBean
 *
 * @author Scott Marlow
 */
public class CdiTestBean {

    @PersistenceUnit(unitName = "mainNonTXPu")
    EntityManagerFactory emf;


    public void createData() {
        createTestData();
    }

    public void logTrace(String message) {
        System.out.println(message);
    }

    java.util.Date dateId = getPKDate(2006, 04, 15);

    public java.util.Date getPKDate(final int yy, final int mm, final int dd) {
        Calendar newCal = Calendar.getInstance();
        newCal.clear();
        newCal.set(yy, mm, dd);
        logTrace("getPKDate: returning date:" + newCal.getTime());
        return newCal.getTime();
    }


    public void createTestData() {

        logTrace("createTestData");

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        Character[] cArray = {'a'};
        Byte[] bArray = {(byte) 100};
        DataTypes d1 = new DataTypes(1, true, 'a', (short) 100, 500, 300L, 50D, 1.0F,
                cArray, bArray);

        DataTypes2 d2 = new DataTypes2(dateId);

        em.persist(d1);
        em.persist(d2);

        em.flush();

    }

}

