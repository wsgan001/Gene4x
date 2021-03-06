package org.geworkbench.util.associationdiscovery.PSSM;

import org.geworkbench.bison.datastructure.biocollections.microarrays.DSMicroarraySet;
import org.geworkbench.bison.datastructure.bioobjects.microarray.DSGenotypicMarkerValue;
import org.geworkbench.bison.datastructure.bioobjects.microarray.DSMarkerValue;
import org.geworkbench.bison.datastructure.bioobjects.microarray.DSMicroarray;
import org.geworkbench.bison.datastructure.complex.pattern.DSMatchedPattern;
import org.geworkbench.bison.datastructure.complex.pattern.DSPattern;
import org.geworkbench.bison.datastructure.complex.pattern.DSPatternMatch;
import org.geworkbench.bison.datastructure.complex.pattern.matrix.CSPValued;
import org.geworkbench.bison.util.DSPValue;
import org.geworkbench.util.associationdiscovery.cluster.CSMatrixPattern;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * <p>Title: Bioworks</p>
 * <p/>
 * <p>Description: Modular Application Framework for Gene Expession, Sequence and Genotype Analysis</p>
 * <p/>
 * <p>Copyright: Copyright (c) 2003 -2004</p>
 * <p/>
 * <p>Company: Columbia University</p>
 *
 * @author not attributable
 * @version 1.0
 */
class CSPSSMMatrixPattern extends CSMatrixPattern implements DSPattern<DSMicroarray, DSPValue> {
    boolean isTrained = false;
    HashMap[] pssm = null;
    double[] pseudoNo = null;
    double pseudoCount = 0.0;
    boolean isAllele = false;
    public double mean = 0;
    public double sigma = 0;
    static Double nullKey = new Double(-1);

    class GeneValue {
        double count = 0;

        public GeneValue(double count) {
            this.count = count;
        }
    }

    public CSPSSMMatrixPattern() {
        pssm = new HashMap[markers().length];
        pseudoNo = new double[markers.length];
        // Allocate space to store the PSSM and the pseudo counts
        //trainPSSM();
    }

    public DSPValue match(DSMicroarray object) {
        /** @todo to be implemented */
        CSPValued r = new CSPValued();
        r.setPValue(1.0);
        return r;
    }

    public List<DSPatternMatch<DSMicroarray, DSPValue>> match(DSMicroarray object, double p) {
        /** @todo to be implemented */
        return null;
    }

    public String toString(DSMicroarray array, DSPValue registration) {
        /** @todo to be implemented with logo */
        return "";
    }

    protected void trainPSSM(DSMicroarraySet<DSMicroarray> set, DSMatchedPattern<DSMicroarray, DSPValue> matchedPattern) {
        // Compute the statistics for the whole set and for the pattern
        // The PSSM is generated by computing the observed counts for each individual allele n(i)
        // a pseudo-count Nb(i) = 5 * ac. This is estimated according to the optimal method in:
        // "Using substitution probabilities to improve position-specific scoring matrices"
        // by Jorja G. Henikoff and Steven Henikoff*
        // where ac is the number of allele counts in the column.
        mean = 0.0;
        sigma = 0.0;
        pssm = new HashMap[markers.length];
        HashMap PSSMFG = new HashMap();
        HashMap PSSMBG = new HashMap();
        // First generate the counts for the background population
        for (int i = 0; i < markers().length; i++) {
            int geneId = markers[i].getSerial();
            pssm[i] = new HashMap();
            PSSMBG = new HashMap();
            //PSSMBG.put(nullKey, new GeneValue(1));
            for (int j = 0; j < set.size(); j++) {
                boolean valid = set.get(j).getMarkerValue(geneId).isValid();
                Double keyA = null;
                Double keyB = null;
                DSMarkerValue value = set.get(j).getMarkerValue(geneId);
                double v0 = 0;
                double v1 = 0;
                if (isAllele) {
                    DSGenotypicMarkerValue gt = (DSGenotypicMarkerValue) value;
                    v0 = (double) gt.getAllele(0);
                    v1 = (double) gt.getAllele(1);
                } else {
                    v0 = value.getValue();
                }
                if (valid) {
                    keyA = new Double(v0);
                    keyB = new Double(v1);
                    GeneValue gv = (GeneValue) PSSMBG.get(keyA);
                    if (gv != null) {
                        gv.count++;
                    } else {
                        PSSMBG.put(keyA, new GeneValue(1));
                    }
                    if (isAllele) {
                        gv = (GeneValue) PSSMBG.get(keyB);
                        if (gv != null) {
                            gv.count++;
                        } else {
                            PSSMBG.put(keyB, new GeneValue(1));
                        }
                    }
                }
            }
            // Now compute the distribution for the pattern population
            PSSMFG.clear();
            for (DSPatternMatch<DSMicroarray, ? extends DSPValue> match : matchedPattern.matches()) {
                DSMicroarray array = match.getObject();
                DSMarkerValue value = array.getMarkerValue(geneId);
                if (value.isValid()) {
                    double v0 = 0;
                    double v1 = 0;
                    if (isAllele) {
                        DSGenotypicMarkerValue gt = (DSGenotypicMarkerValue) value;
                        v0 = (double) gt.getAllele(0);
                        v1 = (double) gt.getAllele(1);
                    } else {
                        v0 = value.getValue();
                    }
                    Double keyA = new Double(v0);
                    Double keyB = new Double(v1);
                    GeneValue gv = (GeneValue) PSSMFG.get(keyA);
                    if (gv != null) {
                        gv.count++;
                    } else {
                        PSSMFG.put(keyA, new GeneValue(1));
                    }
                    if (isAllele) {
                        gv = (GeneValue) PSSMFG.get(keyB);
                        if (gv != null) {
                            gv.count++;
                        } else {
                            PSSMFG.put(keyB, new GeneValue(1));
                        }
                    }
                }
            }
            // Now compute the pseudo counts
            pseudoNo[i] = 5 * PSSMFG.size();
            // Now assign the PSSM scores as the log ration of the weighed averages of the counts and pseudo counts
            // and the background probability
            double mean = 0.0;
            double sigma = 0.0;
            double maNo = (double) (matchedPattern.matches().size());
            double pTot = 0.0;
            Set keys = PSSMBG.keySet();
            Iterator it = keys.iterator();
            while (it.hasNext()) {
                Double key = (Double) it.next();
                GeneValue gvBg = (GeneValue) PSSMBG.get(key);
                GeneValue gvFg = (GeneValue) PSSMFG.get(key);
                double countBg = gvBg.count / 2.0;
                double countFg = 0;
                if (gvFg != null) {
                    countFg = gvFg.count / 2.0;
                }
                double pBg = countBg / (double) set.size();
                double pMarker1 = (countFg + pseudoNo[i] * pBg) / (pseudoNo[i] + maNo);
                double pMarker2 = (countFg + 0.5 * pBg) / (0.5 + maNo);
                double score1 = Math.log(pMarker1 / pBg);
                double score2 = Math.log(pMarker2 / pBg);
                pssm[i].put(key, new GeneValue(score1));
                mean += score2 * pMarker2;
                sigma += score2 * score2 * pMarker2;
                pTot += pMarker2;
            }
            double pMarker = Math.log(pseudoNo[i] / (pseudoNo[i] + maNo));
            pssm[i].put(nullKey, new GeneValue(pMarker));
            mean += mean;
            sigma += sigma - mean * mean;
        }
        sigma = Math.sqrt(sigma);
    }
}
