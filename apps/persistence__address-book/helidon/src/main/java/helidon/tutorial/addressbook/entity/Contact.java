package helidon.tutorial.addressbook.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity
public class Contact implements Serializable {
    private static final long serialVersionUID = -825634229676522580L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String mobilePhone;
    private String homePhone;
    @Temporal(TemporalType.DATE)
    private Date birthday;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String s) { this.firstName = s; }
    public String getLastName() { return lastName; }
    public void setLastName(String s) { this.lastName = s; }
    public String getEmail() { return email; }
    public void setEmail(String s) { this.email = s; }
    public String getMobilePhone() { return mobilePhone; }
    public void setMobilePhone(String s) { this.mobilePhone = s; }
    public String getHomePhone() { return homePhone; }
    public void setHomePhone(String s) { this.homePhone = s; }
    public Date getBirthday() { return birthday; }
    public void setBirthday(Date d) { this.birthday = d; }
}
