package BiNGO.methods;

/* * Copyright (c) 2005 Flanders Interuniversitary Institute for Biotechnology (VIB)
 * *
 * * Authors : Steven Maere, Karel Heymans
 * *
 * * This program is free software; you can redistribute it and/or modify
 * * it under the terms of the GNU General Public License as published by
 * * the Free Software Foundation; either version 2 of the License, or
 * * (at your option) any later version.
 * *
 * * This program is distributed in the hope that it will be useful,
 * * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 * * The software and documentation provided hereunder is on an "as is" basis,
 * * and the Flanders Interuniversitary Institute for Biotechnology
 * * has no obligations to provide maintenance, support,
 * * updates, enhancements or modifications.  In no event shall the
 * * Flanders Interuniversitary Institute for Biotechnology
 * * be liable to any party for direct, indirect, special,
 * * incidental or consequential damages, including lost profits, arising
 * * out of the use of this software and its documentation, even if
 * * the Flanders Interuniversitary Institute for Biotechnology
 * * has been advised of the possibility of such damage. See the
 * * GNU General Public License for more details.
 * *
 * * You should have received a copy of the GNU General Public License
 * * along with this program; if not, write to the Free Software
 * * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * *
 * * Authors: Steven Maere, Karel Heymans
 * * Date: Nov.29.2007
 * * Description: class that counts the small n, big N, small x, big X which serve as input for the statistical tests.     
 **/
import BiNGO.interfaces.DistributionCount;
import cytoscape.data.annotation.Annotation;
import cytoscape.data.annotation.Ontology;
import cytoscape.task.TaskMonitor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * ************************************************************
 * ParentChildIntersectionCount.java Steven Maere (c) Nov 2007 ----------------------
 * <p/>
 * class that counts the small n, big N, small x, big X which serve as input for the parent-child intersection
 * conditional hypergeometric test (Grossmann et al. Bioinformatics 2007).
 * *************************************************************
 */
public class ParentChildIntersectionCount implements DistributionCount {

    /*--------------------------------------------------------------
     FIELDS.
     --------------------------------------------------------------*/
    /**
     * the annotation.
     */
    private Annotation annotation;
    /**
     * the ontology.
     */
    private Ontology ontology;
    private Map<String, Set<String>> alias;
    /**
     * HashSet of selected nodes
     */
    private Set selectedNodes;
    /**
     * HashSet of reference nodes
     */
    private Set refNodes;
    /**
     * hashmap with values of small n ; keys GO labels.
     */
    private Map mapSmallN;
    /**
     * hashmap with values of small x ; keys GO labels.
     */
    private Map mapSmallX;
    /**
     * int containing value for big N.
     */
    private Map mapBigN;
    /**
     * int containing value for big X.
     */
    private Map mapBigX;
    // Keep track of progress for monitoring:
    private int maxValue;
    private boolean interrupted = false;

    /*--------------------------------------------------------------
     CONSTRUCTOR.
     --------------------------------------------------------------*/
    public ParentChildIntersectionCount(Annotation annotation, Ontology ontology, Set selectedNodes,
            Set refNodes, Map alias) {

        this.annotation = annotation;
        this.ontology = ontology;
        this.alias = alias;
        annotation.setOntology(ontology);

        this.selectedNodes = selectedNodes;
        this.refNodes = refNodes;
    }

    /*--------------------------------------------------------------
     METHODS.
     --------------------------------------------------------------*/
    /**
     * method for compiling GO classifications for given node
     */
    @Override
    public Set getNodeClassifications(String node) {

        // HashSet for the classifications of a particular node
        Set classifications = new HashSet();
        Set identifiers = alias.get(node + "");
        if (identifiers != null) {
            Iterator it = identifiers.iterator();
            while (it.hasNext()) {
                int[] goID = annotation.getClassifications(it.next() + "");
                for (int t = 0; t < goID.length; t++) {
                    classifications.add(goID[t] + "");
//			omitted : all parent classes of GO class that node is assigned to are also explicitly included in classifications from the start
//			up(goID[t], classifications) ;	
                }
            }
        }
        return classifications;
    }

    /**
     * method for recursing through tree to root
     */

    /*  public void up (int goID, HashSet classifications){	
     OntologyTerm child  = ontology.getTerm(goID);	
     int [] parents =  child.getParentsAndContainers ();	
     for(int t = 0; t < parents.length; t++){
     classifications.add(parents[t] + "");
     up(parents[t],classifications);
     }	
     }
     */
    /**
     * method for making the hashmap for small n.
     */
    @Override
    public void countSmallN() {

        mapSmallN = this.count(refNodes);
    }

    /**
     * method for making the hashmap for the small x.
     */
    @Override
    public void countSmallX() {

        mapSmallX = this.count(selectedNodes);
    }

    /**
     * method that counts for small n and small x.
     */
    @Override
    public Map count(Set nodes) {

        HashMap map = new HashMap();

        Iterator i = nodes.iterator();
        while (i.hasNext()) {
            Set classifications = getNodeClassifications(i.next().toString());
            Iterator iterator = classifications.iterator();
            Integer id;

            // puts the classification counts in a map
            while (iterator.hasNext()) {
                id = new Integer(iterator.next().toString());
                if (map.containsKey(id)) {
                    map.put(id, new Integer(new Integer(map.get(id).toString()).intValue() + 1));
                } else {
                    map.put(id, new Integer(1));
                }
            }

        }

        return map;
    }

    /**
     * counts big N.
     */
    @Override
    public void countBigN() {

        mapBigN = new HashMap();
        for (Object id : this.mapSmallX.keySet()) {
            int[] parents = this.ontology.getTerm(((Integer) id).intValue()).getParentsAndContainers();
            int bigN = 0;
            for (Object i : this.refNodes) {
                Set classifications = getNodeClassifications(i.toString());
                boolean ok = true;
                for (int j : parents) {
                    if (!classifications.contains(j + "")) {
                        ok = false;
                    }
                }
                if (ok == true) {
                    bigN++;
                }
            }
            mapBigN.put(id, new Integer(bigN));
        }
    }

    /**
     * counts big X.
     */
    @Override
    public void countBigX() {

        mapBigX = new HashMap();
        for (Object id : this.mapSmallX.keySet()) {
            int[] parents = this.ontology.getTerm(((Integer) id).intValue()).getParentsAndContainers();
            int bigX = 0;
            for (Object i : this.selectedNodes) {
                Set classifications = getNodeClassifications(i.toString());
                boolean ok = true;
                for (int j : parents) {
                    if (!classifications.contains(j + "")) {
                        ok = false;
                    }
                }
                if (ok == true) {
                    bigX++;
                }
            }
            mapBigX.put(id, new Integer(bigX));
        }
    }

    /*--------------------------------------------------------------
     GETTERS.
     --------------------------------------------------------------*/
    @Override
    public Map getTestMap() {

        return mapSmallX;
    }

    /**
     * returns small n hashmap.
     *
     * @return hashmap mapSmallN
     */
    @Override
    public Map getMapSmallN() {

        return mapSmallN;
    }

    /**
     * returns small x hashmap.
     *
     * @return hashmap mapSmallX
     */
    @Override
    public Map getMapSmallX() {

        return mapSmallX;
    }

    /**
     * returns the int for the big N.
     *
     * @return int bigN
     */
    @Override
    public Map getMapBigN() {

        return mapBigN;
    }

    /**
     * returns the int for the big X.
     *
     * @return int bigX.
     */
    @Override
    public Map getMapBigX() {

        return mapBigX;
    }

    @Override
    public void calculate() {

        countSmallX();
        countSmallN();
        countBigX();
        countBigN();
    }

    /**
     * Run the Task.
     */
    @Override
    public void run() {

        calculate();
    }

    /**
     * Non-blocking call to interrupt the task.
     */
    @Override
    public void halt() {

        this.interrupted = true;
    }

    @Override
    public String getTitle() {

        return "Counting genes in GO categories...";
    }

    @Override
    public Map<String, Double> getWeights() {

        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Map<Integer, Double> getMapWeights() {

        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setTaskMonitor(TaskMonitor tm) throws IllegalThreadStateException {
        //throw new UnsupportedOperationException("Not supported yet.");
    }
}
