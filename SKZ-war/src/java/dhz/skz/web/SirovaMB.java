/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package dhz.skz.web;

import dhz.skz.aqdb.facades.PodatakSiroviFacade;
import dhz.skz.aqdb.facades.PostajaFacade;
import dhz.skz.aqdb.entity.PodatakSirovi;
import dhz.skz.aqdb.entity.Postaja;
import dhz.skz.aqdb.entity.ProgramMjerenja;
import dhz.skz.aqdb.facades.ProgramMjerenjaFacade;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.model.chart.AxisType;
import org.primefaces.model.chart.DateAxis;
import org.primefaces.model.chart.LineChartModel;
import org.primefaces.model.chart.LineChartSeries;

/**
 *
 * @author kraljevic
 */
@Named(value = "sirovaMB")
@ManagedBean
@ViewScoped
public class SirovaMB implements Serializable{
    @Inject
    private  transient Logger log;
    /**
     * Creates a new instance of SirovaMB
     */
    public SirovaMB() {
    }
    
    @EJB
    private PodatakSiroviFacade podatakFacade;

    @EJB
    private PostajaFacade postajaFacade;
    
    @EJB
    private ProgramMjerenjaFacade programMjerenjaFacade;

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private Postaja postaja;
    private Integer postajaId;
    private Collection<Postaja> postaje;
//    private TreeMap<String, Postaja> postaje;

    private List<ProgramMjerenja> selektiraniProgram;
    private Map<ProgramMjerenja, List<PodatakSirovi>> selektiraniPodaci;

    private Collection<ProgramMjerenja> programiSvi;

    private LineChartModel dateModel;
    
    private boolean novi = false;

    public LineChartModel getDateModel() {
        return dateModel;
    }

    public Date getD1() {
        return d1;
    }

    public void setD1(Date d1) {
        this.d1 = d1;
    }

    public Date getD2() {
        return d2;
    }

    public void setD2(Date d2) {
        this.d2 = d2;
    }
    private Date d1, d2;

    @PostConstruct
    public void init() {
        postaja = null;
        postaje = postajaFacade.findAll();
        programiSvi = programMjerenjaFacade.findByPostaja(postaja);
        dateModel = new LineChartModel();
        LineChartSeries series1 = new LineChartSeries();
        series1.set(d1, 0);
        dateModel.addSeries(series1);
    }

    public Integer getPostajaId() {
        return postajaId;
    }

    public void setPostajaId(Integer postajaId) {
        this.postajaId = postajaId;
    }

    public Postaja getPostaja() {
        return postaja;
    }

    public void setPostaja(Postaja postaja) {
        this.postaja = postaja;
    }

    public Collection<Postaja> getPostaje() {
        return postaje;
    }

    public List<ProgramMjerenja> getSelektiraniProgram() {
        return selektiraniProgram;
    }

    public void setSelektiraniProgram(List<ProgramMjerenja> selektiraniProgram) {
        this.selektiraniProgram = selektiraniProgram;
    }

    public Collection<ProgramMjerenja> getProgramiSvi() {
        return programiSvi;
    }

    public void setProgramiSvi(List<ProgramMjerenja> programiSvi) {
        this.programiSvi = programiSvi;
    }

    public void onPostajaChange() {
        novi = false;
        if (postaja != null) {
            programiSvi = programMjerenjaFacade.findByPostaja(postaja);
        } else {
            programiSvi = new ArrayList<>();
        }
    }

    public void onProgramChange() {
        novi = false;
    }

    public void pokupiPodatke() {
        if (!novi) {
            selektiraniPodaci = new HashMap<>();
            if (selektiraniProgram != null) {
                for (ProgramMjerenja pm : selektiraniProgram) {
                    selektiraniPodaci.put(pm, podatakFacade.getPodaci(pm, d1, d2, false, true));
                }
            }
            novi = true;
        }
    }

    public void displayLocation() {
        pokupiPodatke();
        dateModel = new LineChartModel();

        dateModel.setExtender("chartExtender");
        dateModel.setZoom(true);
        dateModel.getAxis(AxisType.Y).setLabel("Values");

        for (ProgramMjerenja pm : selektiraniPodaci.keySet()) {
            List<PodatakSirovi> podaci = selektiraniPodaci.get(pm);

            LineChartSeries series1 = new LineChartSeries();
            series1.setLabel(pm.getKomponentaId().getNaziv());
            for (PodatakSirovi p : podaci) {
                if ( p.getVrijednost() > -999.) {
                    series1.set(sdf.format(p.getVrijeme()), p.getVrijednost());
                }
            }
//            series1.setMarkerStyle("circle");
            dateModel.addSeries(series1);
        }
        DateAxis axis = new DateAxis("Dates");
        axis.setTickAngle(-50);
        axis.setMin(sdf.format(d1));
        axis.setMax(sdf.format(d2));
        axis.setTickFormat("%m. %d. %H:%M");
        dateModel.getAxes().put(AxisType.X, axis);
        dateModel.setLegendPosition("ne");

    }

}
