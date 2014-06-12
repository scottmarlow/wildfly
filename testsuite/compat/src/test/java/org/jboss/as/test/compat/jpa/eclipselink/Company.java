package org.jboss.as.test.compat.jpa.eclipselink;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

/**
 * Company
 *
 * @author Scott Marlow
 */
@Entity
public class Company {
    @Id
    private int id;

    @OneToMany(mappedBy="company", fetch= FetchType.EAGER, cascade= CascadeType.ALL)
    private Set<Employee> employees = new HashSet<Employee>();

    public int getId() {

        return id;
    }
    public void setId(int id) {
        this.id = id;
    }


}
