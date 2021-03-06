/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dhz.skz.aqdb.entity;

import java.io.Serializable;
import java.util.Collection;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 *
 * @author kraljevic
 */
@Entity
@Table(name = "kategorije_granica")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "KategorijeGranica.findAll", query = "SELECT k FROM KategorijeGranica k"),
    @NamedQuery(name = "KategorijeGranica.findById", query = "SELECT k FROM KategorijeGranica k WHERE k.id = :id"),
    @NamedQuery(name = "KategorijeGranica.findByOznaka", query = "SELECT k FROM KategorijeGranica k WHERE k.oznaka = :oznaka"),
    @NamedQuery(name = "KategorijeGranica.findByOpis", query = "SELECT k FROM KategorijeGranica k WHERE k.opis = :opis")})
public class KategorijeGranica implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    private Integer id;
    @Size(max = 90)
    private String oznaka;
    @Size(max = 600)
    private String opis;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "kategorijeGranicaId")
    private Collection<Granice> graniceCollection;

    public KategorijeGranica() {
    }

    public KategorijeGranica(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getOznaka() {
        return oznaka;
    }

    public void setOznaka(String oznaka) {
        this.oznaka = oznaka;
    }

    public String getOpis() {
        return opis;
    }

    public void setOpis(String opis) {
        this.opis = opis;
    }

    @XmlTransient
    public Collection<Granice> getGraniceCollection() {
        return graniceCollection;
    }

    public void setGraniceCollection(Collection<Granice> graniceCollection) {
        this.graniceCollection = graniceCollection;
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
        if (!(object instanceof KategorijeGranica)) {
            return false;
        }
        KategorijeGranica other = (KategorijeGranica) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "dhz.skz.aqdb.entity.KategorijeGranica[ id=" + id + " ]";
    }
    
}
