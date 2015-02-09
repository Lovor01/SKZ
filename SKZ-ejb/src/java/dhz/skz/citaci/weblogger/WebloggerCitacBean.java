/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dhz.skz.citaci.weblogger;

import dhz.skz.aqdb.entity.IzvorPodataka;
import dhz.skz.aqdb.entity.NivoValidacije;
import dhz.skz.aqdb.entity.Podatak;
import dhz.skz.aqdb.entity.PodatakSirovi;
import dhz.skz.aqdb.entity.Postaja;
import dhz.skz.aqdb.entity.ProgramMjerenja;
import dhz.skz.aqdb.entity.ZeroSpan;
import dhz.skz.aqdb.facades.PodatakFacade;
import dhz.skz.aqdb.facades.PostajaFacade;
import dhz.skz.aqdb.facades.ProgramMjerenjaFacadeLocal;
import dhz.skz.aqdb.facades.ZeroSpanFacade;
import dhz.skz.citaci.CitacIzvora;
import dhz.skz.citaci.FtpKlijent;
import dhz.skz.citaci.weblogger.exceptions.FtpKlijentException;
import dhz.skz.citaci.weblogger.exceptions.WlFileException;
import dhz.skz.citaci.weblogger.util.NizOcitanja;
import dhz.skz.citaci.weblogger.util.AgregiraniPodatak;
import dhz.skz.citaci.weblogger.util.SatniAgregator;
import dhz.skz.citaci.weblogger.validatori.ValidatorFactory;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.EJBContext;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.apache.commons.net.ftp.FTPFile;

/**
 *
 * @author kraljevic
 */
@Stateless
@LocalBean
public class WebloggerCitacBean implements CitacIzvora {

    private static final Logger log = Logger.getLogger(WebloggerCitacBean.class.getName());
    private final TimeZone timeZone = TimeZone.getTimeZone("UTC");
    private final SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
    
    @PersistenceContext(unitName = "LIKZ-ejbPU")
    private EntityManager em;


    @EJB
    private ProgramMjerenjaFacadeLocal programMjerenjaFacade;

    @EJB
    private PodatakFacade podatakFacade;

    @EJB
    private ZeroSpanFacade zeroSpanFacade;

    @EJB
    private PostajaFacade posajaFacade;

    @EJB
    private ValidatorFactory validatorFac;

    private IzvorPodataka izvor;
    private Collection<ProgramMjerenja> programNaPostaji;
    private Postaja aktivnaPostaja;
    private Date vrijemeZadnjegMjerenja, vrijemeZadnjegZeroSpan;

    enum Vrsta {MJERENJE, KALIBRACIJA}
    
    public WebloggerCitacBean() {
        formatter.setTimeZone(timeZone);
    }

    @Override
    public Map<ProgramMjerenja, NizOcitanja> procitaj(IzvorPodataka izvor) {
        log.log(Level.INFO, "POCETAK CITANJA");
        this.izvor = izvor;
        for (Iterator<Postaja> it = posajaFacade.getPostajeZaIzvor(izvor).iterator(); it.hasNext();) {
            aktivnaPostaja = it.next();
            log.log(Level.INFO, "Citam: {0}", aktivnaPostaja.getNazivPostaje());

            programNaPostaji = programMjerenjaFacade.find(aktivnaPostaja, izvor);

            odrediVrijemeZadnjegPodatka(aktivnaPostaja);

            pokupiMjerenjaSaPostaje();

        }
        log.log(Level.INFO, "KRAJ CITANJA");
        return null;
    }

    private Map<ProgramMjerenja, NizOcitanja> getMapaNizovaOcitanja() throws URISyntaxException {

        Map<ProgramMjerenja, NizOcitanja> tmp = new HashMap<>();

        for (ProgramMjerenja pm : programNaPostaji) {
            log.log(Level.FINEST, "Program: {0}: {1}", new Object[]{pm.getPostajaId().getNazivPostaje(), pm.getKomponentaId().getFormula()});
            NizOcitanja np = new NizOcitanja();
            np.setKljuc(pm);
            np.setValidatori(validatorFac.getValidatori(pm));
            tmp.put(pm, np);
        }
        return tmp;
    }

    private void pokupiMjerenjaSaPostaje() {
        FtpKlijent ftp = new FtpKlijent();

        try {
            Map<ProgramMjerenja, NizOcitanja> mapaMjernihNizova = getMapaNizovaOcitanja();
            ftp.connect(new URI(izvor.getUri()));

            String ptStr = "^(" + Pattern.quote(aktivnaPostaja.getNazivPostaje().toLowerCase()) + ")(_c)?-(\\d{8})(.?)\\.csv";
            Pattern pattern = Pattern.compile(ptStr);
            for (FTPFile file : ftp.getFileList()) {
                Matcher m = pattern.matcher(file.getName().toLowerCase());
                if (m.matches()) {
                    try {
                        Vrsta vrstaDatoteke = odrediVrstuDatoteke(m.group(2));
                        Date terminDatoteke = formatter.parse(m.group(3));
                        
                        if (isDobarTermin(terminDatoteke, vrstaDatoteke)) {

                            WlFileParser citac = napraviParser(vrstaDatoteke);

                            Collection<ProgramMjerenja> aktivniProgram = programMjerenjaFacade.findZaTermin(aktivnaPostaja, izvor, terminDatoteke);

                            citac.setNizKanala(mapaMjernihNizova, aktivniProgram);

                            log.log(Level.INFO, "Datoteka :{0}", file.getName());
                            try (InputStream ifs = ftp.getFileStream(file)) {
                                citac.parse(ifs);
                            } catch (WlFileException ex) {
                                log.log(Level.SEVERE, null, ex);
                            } catch (Exception ex) { // kakva god da se iznimka dogodi, nastavljamo
                                log.log(Level.SEVERE, null, ex);
                            } finally {
                                ftp.zavrsi();
                            }
                        }
                    } catch (ParseException ex) {
                        log.log(Level.SEVERE, null, ex);
                    }
                }
            }
            obradiISpremiNizove(mapaMjernihNizova);
        } catch (FtpKlijentException | URISyntaxException ex) {
            log.log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            log.log(Level.SEVERE, "XXXXXXXX", ex);
        } finally {
            ftp.disconnect();
        }
    }

    private void odrediVrijemeZadnjegPodatka(Postaja p) {
        Podatak zadnjiP = podatakFacade.getZadnji(izvor, p);
        if (zadnjiP != null) {
            vrijemeZadnjegMjerenja = zadnjiP.getVrijeme();
        } else {
            vrijemeZadnjegMjerenja = null;
        }
        vrijemeZadnjegZeroSpan = zeroSpanFacade.getVrijemeZadnjeg(izvor, p);
    }

    private void obradiISpremiNizove(Map<ProgramMjerenja, NizOcitanja> ulaz) {
        for (ProgramMjerenja p : ulaz.keySet()) {

            NizOcitanja niz = ulaz.get(p);
            try {
                SatniAgregator a = new SatniAgregator(niz.getPodaci(), niz.getValidatori());
                a.agregiraj();
                niz.setPodaci(a.getAgregiraniPodaci());
            } catch (Exception ex) {
                log.log(Level.SEVERE, "Postaja {0}, komponenta {1}", new Object[]{p.getPostajaId().getNazivPostaje(), p.getKomponentaId().getFormula()});
                throw ex;
            }
            log.log(Level.INFO, "Pospremam Postaja {0}, komponenta {1}", new Object[]{p.getPostajaId().getNazivPostaje(), p.getKomponentaId().getFormula()});
            pospremiNiz(niz);
        }
    }

    public void pospremiNiz(NizOcitanja niz) {

//            pospremiZsNiz(niz);
//            pospremiMjerenjaNiz(niz);

    }

    @TransactionAttribute(REQUIRES_NEW)
    public void pospremiMjerenjaNiz(NizOcitanja niz) {
        log.log(Level.INFO, "Postaja {0}, komponenta {1}, prvi {2}, zadnj {3}, ukupno {4}",
                    new Object[]{niz.getKljuc().getPostajaId().getNazivPostaje(),
                        niz.getKljuc().getKomponentaId().getFormula(),
                        niz.getPodaci().firstKey(),
                        niz.getPodaci().lastKey(),
                        niz.getPodaci().size()});
            NivoValidacije nv = new NivoValidacije((short) 0);
            for (Date d : niz.getPodaci().keySet()) {
                AgregiraniPodatak wlp = niz.getPodaci().get(d);
                Podatak p = new Podatak();
                p.setVrijeme(d);
                p.setVrijednost(wlp.getVrijednost());
                p.setObuhvat(wlp.getObuhvat());
                p.setProgramMjerenjaId(niz.getKljuc());
                p.setNivoValidacijeId(nv);
                p.setStatus(wlp.getStatus());
                podatakFacade.create(p);
            }
    }

    @TransactionAttribute(REQUIRES_NEW)
    public void pospremiZsNiz(NizOcitanja niz) {

        log.log(Level.INFO, "ZS postaja {0}, komponenta {1}, prvi {2}, zadnj {3}, ukupno {4}",
                new Object[]{niz.getKljuc().getPostajaId().getNazivPostaje(),
                    niz.getKljuc().getKomponentaId().getFormula(),
                    niz.getZeroSpan().firstKey(),
                    niz.getZeroSpan().lastKey(),
                    niz.getZeroSpan().size()});
        for (Date d : niz.getZeroSpan().keySet()) {
            ZeroSpan zs = niz.getZeroSpan().get(d);
            zeroSpanFacade.create(zs);
        }
    }

    private boolean isDobarTermin(Date t, Vrsta vrsta) {
        if (vrsta == Vrsta.MJERENJE) {
            return isDobarTerminMjerenje(t);
        } else {
            return isDobarTerminZeroSpan(t);
        }
    }

    private boolean isDobarTerminMjerenje(Date t) {
        boolean aktivna = false;
        boolean dobroVrijeme;
        for (ProgramMjerenja pm : programNaPostaji) {
            if (pm.getIzvorProgramKljuceviMap() != null) {
                boolean a = t.compareTo(pm.getPocetakMjerenja()) >= 0;
                boolean b = (pm.getZavrsetakMjerenja() == null) || (t.compareTo(pm.getZavrsetakMjerenja()) <= 0);
                aktivna |= (a && b);

//                aktivna |= t.compareTo(pm.getPocetakMjerenja()) >= 0
//                    && ((pm.getZavrsetakMjerenja() == null) || (t.compareTo(pm.getZavrsetakMjerenja()) <= 0));
            }
        }
        dobroVrijeme = (vrijemeZadnjegMjerenja == null) || (vrijemeZadnjegMjerenja.getTime() - t.getTime() < 24 * 3600 * 1000);
        return aktivna && dobroVrijeme;
    }

    private boolean isDobarTerminZeroSpan(Date t) {
        boolean aktivna = false;
        boolean dobroVrijeme;
        for (ProgramMjerenja pm : programNaPostaji) {
            if ((pm.getIzvorProgramKljuceviMap() != null) && (pm.getIzvorProgramKljuceviMap().getUKljuc() != null)) {
                boolean a = t.compareTo(pm.getPocetakMjerenja()) >= 0;
                boolean b = (pm.getZavrsetakMjerenja() == null) || (t.compareTo(pm.getZavrsetakMjerenja()) <= 0);
                aktivna |= (a && b);

//                aktivna |= t.compareTo(pm.getPocetakMjerenja()) >= 0
//                    && ((pm.getZavrsetakMjerenja() == null) || (t.compareTo(pm.getZavrsetakMjerenja()) <= 0));
            }
        }
        dobroVrijeme = (vrijemeZadnjegZeroSpan == null) || (vrijemeZadnjegZeroSpan.getTime() - t.getTime() < 24 * 3600 * 1000);
        return aktivna && dobroVrijeme;
    }

    private WlFileParser napraviParser(Vrsta vrstaDatoteke) {
        WlFileParser parser;
        if (vrstaDatoteke == Vrsta.MJERENJE) {
            parser = new WlMjerenjaParser(timeZone);
            parser.setZadnjiPodatak(vrijemeZadnjegMjerenja);
        } else {
            parser = new WlZeroSpanParser(timeZone);
            parser.setZadnjiPodatak(vrijemeZadnjegMjerenja);
        }

        return parser;
    }
    
    private Vrsta odrediVrstuDatoteke(String group) {
        if ( group == null || group.isEmpty()) {
            return Vrsta.MJERENJE;
        } else {
            return Vrsta.KALIBRACIJA;
        }
    }

    

    @Override
    public Collection<PodatakSirovi> dohvatiSirove(ProgramMjerenja program, Date pocetak, Date kraj, boolean p, boolean k) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Map<ProgramMjerenja, NizOcitanja> procitaj(IzvorPodataka izvor, Map<ProgramMjerenja, Podatak> pocetak) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

//    private Date getVrijemePocetka(Postaja p) {
//        Date zadnji;
//        Podatak zadnjiP = dao.getZadnji(izvor, p);
//        if (zadnjiP != null) {
//            zadnji = zadnjiP.getVrijeme();
//        } else {
//            zadnji = programMjerenjaFacade.getPocetakMjerenja(izvor, p);
//        }
//
////        Collection<ProgramMjerenja> programi = izvorPodatakaFacade.getProgram(p, izvor);
////        Date zadnji = new Date(0L);
////        for (ProgramMjerenja pm : programi) {
////            Date tmp;
////            if (pm.getZavrsetakMjerenja() == null) {
////                tmp = dao.getZadnjiPodatak(pm);
////
////            } else {
////                tmp = pm.getZavrsetakMjerenja();
////            }
////            if (zadnji.before(tmp)) {
////                zadnji = tmp;
////            }
////
////        }
//        log.log(Level.INFO, "Zadnji podatak na {0} u {1}", new Object[]{p.getNazivPostaje(), zadnji});
//        return zadnji;
//    }
//    private NavigableMap<Date, Uredjaj> getUredjajiMapa(ProgramMjerenja pm) {
//        NavigableMap<Date, Uredjaj> mapa = new TreeMap<>();
//        for (ProgramUredjajLink pul : pm.getProgramUredjajLinkCollection()) {
//            mapa.put(pul.getVrijemePostavljanja(), pul.getUredjajId());
//        }
//        return mapa;
//    }
//    private Map<ProgramMjerenja, NizZeroSpanPodataka> getMapaZeroSpanNizova() {
//        Map<ProgramMjerenja, NizZeroSpanPodataka> tmp = new HashMap<>();
//        for (ProgramMjerenja pm : programNaPostaji) {
//            NizZeroSpanPodataka np = new NizZeroSpanPodataka();
//            np.setKljuc(pm);
//            tmp.put(pm, np);
//        }
//        return tmp;
//    }
}
