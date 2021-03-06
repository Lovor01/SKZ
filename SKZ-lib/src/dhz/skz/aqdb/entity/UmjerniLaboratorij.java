/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dhz.skz.aqdb.entity;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 *
 * @author kraljevic
 */
@Entity
@Table(name = "umjerni_laboratorij")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "UmjerniLaboratorij.findAll", query = "SELECT u FROM UmjerniLaboratorij u"),
    @NamedQuery(name = "UmjerniLaboratorij.findById", query = "SELECT u FROM UmjerniLaboratorij u WHERE u.id = :id"),
    @NamedQuery(name = "UmjerniLaboratorij.findByNaziv", query = "SELECT u FROM UmjerniLaboratorij u WHERE u.naziv = :naziv"),
    @NamedQuery(name = "UmjerniLaboratorij.findByAdresa", query = "SELECT u FROM UmjerniLaboratorij u WHERE u.adresa = :adresa")})
public class UmjerniLaboratorij implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 2147483647)
    @Column(name = "naziv")
    private String naziv;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 400)
    @Column(name = "adresa")
    private String adresa;
    @OneToMany(mappedBy = "umjerniLaboratorijId")
    private Collection<Umjeravanje> umjeravanjeCollection;

    public UmjerniLaboratorij() {
    }

    public UmjerniLaboratorij(Integer id) {
        this.id = id;
    }

    public UmjerniLaboratorij(Integer id, String naziv, String adresa) {
        this.id = id;
        this.naziv = naziv;
        this.adresa = adresa;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNaziv() {
        return naziv;
    }

    public void setNaziv(String naziv) {
        this.naziv = naziv;
    }

    public String getAdresa() {
        return adresa;
    }

    public void setAdresa(String adresa) {
        this.adresa = adresa;
    }

    @XmlTransient
    public Collection<Umjeravanje> getUmjeravanjeCollection() {
        return umjeravanjeCollection;
    }

    public void setUmjeravanjeCollection(Collection<Umjeravanje> umjeravanjeCollection) {
        this.umjeravanjeCollection = umjeravanjeCollection;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof UmjerniLaboratorij)) {
            return false;
        }
        UmjerniLaboratorij other = (UmjerniLaboratorij) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "dhz.skz.aqdb.entity.UmjerniLaboratorij[ id=" + id + " ]";
    }
    
}
