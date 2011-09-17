package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.MapLayerMetadata;
import au.org.emii.portal.settings.SettingsSupplementary;
import au.org.emii.portal.util.LayerUtilities;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ala.spatial.data.Query;
import org.ala.spatial.data.QueryUtil;
import org.ala.spatial.util.CommonData;
import org.ala.spatial.data.SolrQuery;
import org.ala.spatial.util.Util;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.SuspendNotAllowedException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

/**
 *
 * @author ajay
 */
public class AddSpeciesInArea extends UtilityComposer {

    SettingsSupplementary settingsSupplementary;
    String selectedMethod = "";
    String pid = "";
    Radiogroup rgArea;
    Radio rAreaWorld, rAreaCustom, rAreaSelected, rAreaAustralia;
    Button btnCancel, btnOk;
    Textbox tToolName;
    boolean setCustomArea = false;
    boolean hasCustomArea = false;
    String lsid;
    MapLayer prevTopArea = null;

    String rank;
    String taxon;
    private String name;
    private String s;
    private int type;
    private int featureCount;

    boolean filterGrid = false;
    boolean filter = false;
    boolean byLsid = false;
    private String metadata;
    private boolean allSpecies = false;

    @Override
    public void afterCompose() {
        super.afterCompose();

        //loadAreaLayers();
    }

    public void loadAreaLayers() {
        try {

            Radiogroup rgArea = (Radiogroup) getFellowIfAny("rgArea");
            Radio rAreaCurrent = (Radio) getFellowIfAny("rAreaCurrent");

            List<MapLayer> layers = getMapComposer().getPolygonLayers();
            for (int i = 0; i < layers.size(); i++) {
                MapLayer lyr = layers.get(i);
                Radio rAr = new Radio(lyr.getDisplayName());
                rAr.setId(lyr.getDisplayName().replaceAll(" ", ""));
                rAr.setValue(lyr.getWKT());
                rAr.setParent(rgArea);
                rgArea.insertBefore(rAr, rAreaCurrent);
            }

            if(!allSpecies) {
                rgArea.setSelectedItem(rAreaWorld);
                rAreaSelected = rAreaWorld;
            } else {
                rAreaWorld.setVisible(false);
                rAreaAustralia.setVisible(false);

                for (int i = 0; i < rgArea.getItemCount(); i++) {
                    if (rgArea.getItemAtIndex(i).isVisible()) {
                        rgArea.getItemAtIndex(i).setSelected(true);
                        rAreaSelected = rgArea.getItemAtIndex(i);
                        Clients.evalJavaScript("jq('#" + rAreaSelected.getUuid() + "-real').attr('checked', true);");
                        break;
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("Unable to load active area layers:");
            e.printStackTrace(System.out);
        }
    }

    public void onCheck$rgArea(Event event) {
        setCustomArea = false;
        hasCustomArea = false;
        if (rgArea.getSelectedItem() == rAreaCustom) {
            setCustomArea = true;
            hasCustomArea = false;
        }
        rAreaSelected = rgArea.getSelectedItem(); 
    }

    public void onClick$btnCancel(Event event) {
        this.detach();
    }

    public void resetWindow(String selectedArea) {
        try {
            boolean ok = false;
            if (hasCustomArea) {
                MapLayer curTopArea = null;
                List<MapLayer> layers = getMapComposer().getPolygonLayers();
                if (layers != null && layers.size() > 0) {
                    curTopArea = layers.get(0);
                } else {
                    curTopArea = null;
                }

                if (curTopArea != prevTopArea) {
                    Radio rAr = new Radio(curTopArea.getDisplayName());
                    rAr.setId(curTopArea.getDisplayName().replaceAll(" ", ""));
                    rAr.setValue(curTopArea.getWKT());
                    rAr.setParent(rgArea);
                    rgArea.insertBefore(rAr, rgArea.getItemAtIndex(0));
                    rgArea.setSelectedIndex(0);
                    rgArea.setSelectedItem(rAr);

                    rAreaSelected = rAr; 

                    ok = true;
                }
            }
            //this.setTop(winTop);
            //this.setLeft(winLeft);

            this.doModal();

            if (ok) {
                onClick$btnOk(null);
            }
        } catch (InterruptedException ex) {
            System.out.println("InterruptedException when resetting analysis window");
            ex.printStackTrace(System.out);
        } catch (SuspendNotAllowedException ex) {
            System.out.println("Exception when resetting analysis window");
            ex.printStackTrace(System.out);
        }
    }

    public void onClick$btnOk(Event event) {

        try {
            if (setCustomArea && !hasCustomArea) {
                Map<String, Object> winProps = new HashMap<String, Object>();
                winProps.put("parent", this);
                winProps.put("parentname", "AddSpeciesInArea");
                winProps.put("lsid", lsid);
                winProps.put("rank", rank);
                winProps.put("taxon", taxon);
                winProps.put("name", name);
                winProps.put("s", s);
                winProps.put("featureCount", featureCount);
                winProps.put("filter", filter);
                winProps.put("filterGrid", filterGrid);
                winProps.put("byLsid", byLsid);
                winProps.put("metadata", metadata);
                winProps.put("type", type);


                List<MapLayer> layers = getMapComposer().getPolygonLayers();
                if (layers != null && layers.size() > 0) {
                    prevTopArea = layers.get(0);
                } else {
                    prevTopArea = null;
                }

                Window window = (Window) Executions.createComponents("WEB-INF/zul/AddArea.zul", getMapComposer(), winProps);
                window.setAttribute("winProps", winProps, true);
                window.doModal();
            } else {
                onFinish();
            }
        } catch (Exception ex) {
            Logger.getLogger(AddSpeciesInArea.class.getName()).log(Level.SEVERE, null, ex);
        }

        this.detach();
    }

    public void onFinish() {
        try {            
            String wkt = getSelectedArea();

            String spname = name + ";" + lsid;
            boolean setupMetadata = true;

            MapLayer ml = null;

            Query q = QueryUtil.get(lsid, getMapComposer()).newWkt(wkt);
            if (byLsid) {
                ml = getMapComposer().mapSpecies(q, name, s, featureCount, type, wkt, -1);
            } else if(filter) {
                ml = getMapComposer().mapSpecies(q, name, s, featureCount, type, wkt, -1);
            } else if(filterGrid) {
                ml = getMapComposer().mapSpecies(q, name, s, featureCount, type, wkt, -1);
            } else if (rank != null && taxon != null && lsid != null) {
                String sptaxon = taxon+";"+lsid;
                ml = getMapComposer().mapSpecies(q, taxon, rank, -1, LayerUtilities.SPECIES, wkt, -1);
                setupMetadata = false;
            } else {  
                int results_count_occurrences = q.getOccurrenceCount();

                //test limit
                if (results_count_occurrences > 0 
                        && results_count_occurrences <= settingsSupplementary.getValueAsInt("max_record_count_map")) {
                    
                    String activeAreaLayerName = getSelectedAreaDisplayName();
                    ml = getMapComposer().mapSpecies(q
                            , "Occurrences in " + activeAreaLayerName
                            , "species"
                            , results_count_occurrences
                            , LayerUtilities.SPECIES
                            , wkt, -1);

                    //getMapComposer().updateUserLogAnalysis("Sampling", sbProcessUrl.toString(), "", CommonData.satServer + "/" + sbProcessUrl.toString(), pid, "map species in area");
                } else {
                    getMapComposer().showMessage(results_count_occurrences
                            + " occurrences in this area.\r\nSelect an area with fewer than "
                            + settingsSupplementary.getValueAsInt("max_record_count_map")
                            + " occurrences");
                }

                setupMetadata = false;
            }

            if (setupMetadata) {
                MapLayerMetadata md = ml.getMapLayerMetadata();
                if (md == null) {
                    md = new MapLayerMetadata();
                    ml.setMapLayerMetadata(md);
                }
                md.setMoreInfo(metadata);
                //md.setSpeciesDisplayLsid(lsid);
                //md.setSpeciesRank(rank);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getSelectedArea() {
        //String area = rgArea.getSelectedItem().getValue();
        String area = rAreaSelected.getValue(); 

        try {
            if (area.equals("current")) {
                area = getMapComposer().getViewArea();
            } else if (area.equals("australia")) {
                area = CommonData.AUSTRALIA_WKT;
            } else if (area.equals("world")) {
                area = CommonData.WORLD_WKT;
            } else {
                List<MapLayer> layers = getMapComposer().getPolygonLayers();
                for (MapLayer ml : layers) {
                    if (area.equals(ml.getDisplayName())) {
                        area = ml.getWKT();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Unable to retrieve selected area");
            e.printStackTrace(System.out);
        }

        return area;
    }

    public String getSelectedAreaName() {
        String area = rAreaSelected.getLabel();
        List<MapLayer> layers = getMapComposer().getPolygonLayers();
        for (MapLayer ml : layers) {
            if (area.equals(ml.getDisplayName())) {
                area = ml.getName();
                break;
            }
        }

        return area;
    }

    public String getSelectedAreaDisplayName() {
        String areaName = rAreaSelected.getLabel();

        return areaName;
    }

    void setSpeciesParams(String lsid, String rank, String taxon) {
        this.lsid = lsid;
        this.rank = rank;
        this.taxon = taxon;
    }

    void setSpeciesFilterGridParams(String lsid, String name, String s, int featureCount, int type, String metadata, String rank) {
        this.lsid = lsid;
        this.name = name;
        this.s = s;
        this.featureCount = featureCount;
        this.type = type;
        this.filterGrid = true;
        this.metadata = metadata;
        this.rank = rank;
    }
    void setSpeciesFilterParams(String lsid, String name, String s, int featureCount, int type, String metadata, String rank) {
        this.lsid = lsid;
        this.name = name;
        this.s = s;
        this.featureCount = featureCount;
        this.type = type;
        this.filter = true;
        this.metadata = metadata;
        this.rank = rank;
    }

    void setSpeciesByLsidParams(String lsid, String name, String s, int featureCount, int type, String metadata) {
        this.lsid = lsid;
        this.name = name;
        this.s = s;
        this.featureCount = featureCount;
        this.type = type;
        this.byLsid = true;
        this.metadata = metadata;
    }

    void setAllSpecies(boolean isAllSpecies) {
        this.allSpecies = isAllSpecies;
    }
}
