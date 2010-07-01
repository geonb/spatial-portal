package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.settings.SettingsSupplementary;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.io.WKTWriter;
import com.vividsolutions.jts.io.gml2.GMLWriter;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.lang.String;
import java.net.URL;
import java.net.URLConnection;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import org.xml.sax.SAXException;
import org.zkoss.zhtml.Messagebox;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.HtmlMacroComponent;


import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.SuspendNotAllowedException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Clients;

import org.zkoss.zul.Button;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;

import org.zkoss.zul.Listbox;


import org.zkoss.zul.Popup;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

/**
 *
 * @author Angus
 */
public class SelectionController extends UtilityComposer {

    private static final String SAT_URL = "sat_url";
    private static final String GEOSERVER_URL = "geoserver_url";
    private Textbox selectionGeom;
    private Textbox boxGeom;
    private Textbox displayGeom;
    private Div polygonInfo;
    private Div envelopeInfo;
    private Label instructions;
    public Button download;
    public Listbox popup_listbox_results;
    public Popup popup_results;
    public Button results_prev;
    public Button results_next;
    public Label results_label;
    public Label popup_label;
    public Radio rdoEnvironmentalEnvelope;
    private Radiogroup rgAreaSelection;
    HtmlMacroComponent envelopeWindow;
    private SettingsSupplementary settingsSupplementary = null;
    private String geoServer;// = "http://spatial.ala.org.au"; // http://localhost:8080
    String satServer;
    String[] results = null;
    int results_pos;
    SortedSet speciesSet;

    public String getGeom() {
        if (rdoEnvironmentalEnvelope.isChecked()) {
            //get PID and return as ENVELOPE(PID)
            String envPid = ((FilteringWCController)envelopeWindow.getFellow("filteringwindow")).getPid();
            if (envPid.length() > 0) {
                return "ENVELOPE(" + envPid + ")";
            } else {
                return "";
            }
        //work around for null polygons to be reported as absence of polygon
        } else if (!displayGeom.getText().contains("NaN NaN")){
            return displayGeom.getText();
        }
        return "";
    }

    @Override
    public void afterCompose() {
        super.afterCompose();

        if (settingsSupplementary != null) {
            geoServer = settingsSupplementary.getValue(GEOSERVER_URL);
            satServer = settingsSupplementary.getValue(SAT_URL);
        }
    }

    public void onClick$btnClearSelection(Event event) {
        hideAllInfo();
        //rgAreaSelection.getSelectedItem().setSelected(false);
        MapComposer mc = getThisMapComposer();
      //  mc.getOpenLayersJavascript().removeAreaSelection();
        displayGeom.setValue("");

        String script = removeCurrentSelection();
        
        mc.getOpenLayersJavascript().execute(mc.getOpenLayersJavascript().iFrameReferences + script);
        mc.removeFromList(mc.getMapLayer("Area Selection"));
    }

    /**
     * Activate the polygon selection tool on the map
     * @param event
     */
    public void onCheck$rdoPolygonSelection(Event event) {
        instructions.setValue("Zoom and pan to the area of interest. Using the mouse, position the cursor at the first point to be digitized and click the left mouse button. Move the cursor to the second vertext of the polygon and click the mouse button. Repeat as required to define the area. On the last vertex, double click to finalise the polygon. ");
        showPolygonInfo();
        MapComposer mc = getThisMapComposer();
        String script = removeCurrentSelection();
        script += mc.getOpenLayersJavascript().addPolygonDrawingTool();
        mc.getOpenLayersJavascript().execute(mc.getOpenLayersJavascript().iFrameReferences + script);

        mc.removeFromList(mc.getMapLayer("Area Selection"));

    }

    /**
     * Activate the box selection tool on the map
     * @param event
     */
    public void onCheck$rdoBoxSelection(Event event) {
        instructions.setValue("Zoom and pan to the area of interest. Using the mouse, position the cursor over the area of interest and hold down the left mouse button and drag a rectangle to the required shape and size. Release the mouse button. ");
        showPolygonInfo();
        String script = removeCurrentSelection();
        MapComposer mc = getThisMapComposer();
        //mc.getOpenLayersJavascript().addBoxDrawingTool();
        script += mc.getOpenLayersJavascript().addBoxDrawingTool();
        mc.getOpenLayersJavascript().execute(mc.getOpenLayersJavascript().iFrameReferences + script);
        mc.removeFromList(mc.getMapLayer("Area Selection"));
    }

    public void onCheck$rdoPointRadiusSelection(Event event) {
        instructions.setValue("Zoom and pan to the area of interest. With the mouse, place the cursor over the centre point of the area of interest. Hold down the (left) mouse button and drag the radius to define the area of interest. Release the mouse button. ");
        showPolygonInfo();
        String script = removeCurrentSelection();
        MapComposer mc = getThisMapComposer();
        script += mc.getOpenLayersJavascript().addRadiusDrawingTool();
        mc.getOpenLayersJavascript().execute(mc.getOpenLayersJavascript().iFrameReferences + script);
        mc.removeFromList(mc.getMapLayer("Area Selection"));
    }

    public void onCheck$rdoExistingFeatureSelection(Event event) {
        instructions.setValue("Zoom and pan to the area of interest. Identify the polygon of interest by a (left) mouse click within that polygon. (The area will be reported in the Area box). ");
        showPolygonInfo();
        String script = removeCurrentSelection();
        MapComposer mc = getThisMapComposer();
        script += mc.getOpenLayersJavascript().addFeatureSelectionTool();
        mc.getOpenLayersJavascript().execute(mc.getOpenLayersJavascript().iFrameReferences + script);
        mc.removeFromList(mc.getMapLayer("Area Selection"));
    }

    public void onCheck$rdoEnvironmentalEnvelope(Event event) {
        instructions.setValue("");
       showEnvelopeInfo();
    }

    public void onClick$rgAreaSelection(Event event) {
        try {
            Messagebox.show(rgAreaSelection.getSelectedItem().getLabel() + " selected");
        } catch (Exception e) {
            System.out.println("Error onClick$rgAreaSelection");
            e.printStackTrace(System.out);
        }
    }

    void showEnvelopeInfo(){
        onClick$btnClearSelection(null);
        envelopeInfo.setVisible(true);
        polygonInfo.setVisible(false);
    }

    void showPolygonInfo(){
        //onClick$btnClearSelection(null);
        envelopeInfo.setVisible(false);
        polygonInfo.setVisible(true);
    }

    void hideAllInfo() {
        //called by onClick$btnClearSelection();
        //rgAreaSelection.getSelectedItem().setChecked(false);
        envelopeInfo.setVisible(false);
        polygonInfo.setVisible(false);
    }



    private String removeCurrentSelection() {
        MapComposer mc = getThisMapComposer();
        MapLayer selectionLayer = mc.getMapLayer("Area Selection");

        if (mc.safeToPerformMapAction()) {
            if ((selectionLayer != null)) {
                //  selectionLayer.setDisplayed(false);
                //selectionLayer.
                System.out.println("removing Area selection layer");
                //mc.deactiveLayer(selectionLayer, true,false);
                return mc.getOpenLayersJavascript().removeMapLayer(selectionLayer);
                //mc.getOpenLayersJavascript().execute(script);
                //mc.removeLayer("Area Selection");
            }
            else {
                return "";
            }
        }
        else {
            try{
                Messagebox.show("Not Safe?");
            }
            catch (Exception e) {}
            return "Failed";
        }
        
    }

    /**
     * 
     * @param event
     */
    public void onChange$selectionGeom(Event event) {
        try {

//            displayGeom.setValue(selectionGeom.getValue());
//            wfsQueryPolygon(selectionGeom.getValue());
//            Clients.showBusy("Filtering species, please wait...", true);
//            Events.echoEvent("onDoInit", this, selectionGeom.getValue());


            displayGeom.setValue(selectionGeom.getValue());

            //get the current MapComposer instance
            MapComposer mc = getThisMapComposer();

            //add feature to the map as a new layer
//            mc.removeLayer("Area Selection");
         MapLayer mapLayer = mc.addWKTLayer(selectionGeom.getValue(),"Area Selection");
            rgAreaSelection.getSelectedItem().setChecked(false);
            instructions.setValue("");
            //wfsQueryBBox(selectionGeom.getValue());
        } catch (Exception e) {//FIXME
        }

    }

    public void onChange$boxGeom(Event event) {
        try {
//            Clients.showBusy("Filtering species, please wait...", true);
//            Events.echoEvent("onDoInit", this, boxGeom.getValue());
            
            displayGeom.setValue(boxGeom.getValue());

            //get the current MapComposer instance
            MapComposer mc = getThisMapComposer();

            //add feature to the map as a new layer
            //mc.removeLayer("Area Selection");
            //mc.deactiveLayer(mc.getMapLayer("Area Selection"), true,true);
            MapLayer mapLayer = mc.addWKTLayer(boxGeom.getValue(),"Area Selection");

            rgAreaSelection.getSelectedItem().setChecked(false);
            instructions.setValue("");

            //wfsQueryBBox(boxGeom.getValue());

        } catch (Exception e) {//FIXME
        }

    }

    public void onClick$btnShowSpecies() {
        openResults();
        /*
        if (selectionGeom.getValue() != ""
                && !selectionGeom.getValue().contains("NaN NaN")) {
            Clients.showBusy("Filtering species, please wait...", true);
            Events.echoEvent("showSpeciesPoly", this, displayGeom.getValue());
        }
        else if(boxGeom.getValue() != ""
                && !boxGeom.getValue().contains("NaN NaN")) {
            Clients.showBusy("Filtering species, please wait...", true);
            Events.echoEvent("showSpecies", this, displayGeom.getValue());
        }*/
    }

    public void showSpecies(Event event) throws Exception {
        String geomData = (String) event.getData();
        wfsQueryBBox(geomData);
        Clients.showBusy("", false);
    }

     public void showSpeciesPoly(Event event) throws Exception {
        String geomData = (String) event.getData();
        wfsQueryPolygon(geomData);
        Clients.showBusy("", false);
    }

    /**
     * Constructs a wfs 'species within bounding box query' for the given geometry
     * @param selectionWKT geometry of the box
     */
    public void wfsQueryPolygon(String selectionWKT) {
        WKTReader wkt_reader = new WKTReader();
        GMLWriter gml_writer = new GMLWriter();
        WKTWriter wkt_writer = new WKTWriter();
         String baseQueryXML = "<wfs:GetFeature service=\"WFS\" version=\"1.1.0\" xmlns:topp=\"http://www.openplans.org/topp\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.1.0/wfs.xsd\"> <wfs:Query typeName=\"ALA:occurrences\"> <wfs:PropertyName>ALA:scientificname</wfs:PropertyName><ogc:Filter><ogc:BBOX><ogc:PropertyName>the_geom</ogc:PropertyName><gml:Envelope srsName=\"http://www.opengis.net/gml/srs/epsg.xml#4326\"><gml:lowerCorner>LOWERCORNER</gml:lowerCorner><gml:upperCorner>UPPERCORNER</gml:upperCorner></gml:Envelope></ogc:BBOX></ogc:Filter></wfs:Query></wfs:GetFeature>";
       if (selectionWKT.contains("MULTIPOLYGON")) {
           selectionWKT.replace("MULTI","");
           selectionWKT.replace("(((","((");
           selectionWKT.replace(")))","))");
       }
       try {
            Geometry selection = wkt_reader.read(selectionWKT);
            //Envelope selectionBounds = selection.getEnvelope().getEnvelopeInternal();
            Geometry selectionBounds = selection.getEnvelope();
            String selectionBoundsWKT = wkt_writer.write(selectionBounds);
            selectionBoundsWKT = selectionBoundsWKT.replace("(", "");
            selectionBoundsWKT = selectionBoundsWKT.replace(")", "");
            String upperCorner = selectionBoundsWKT.replace("POLYGON", "").split(",")[3].toString();
            String lowerCorner = selectionBoundsWKT.replace("POLYGON", "").split(",")[1].toString();
            baseQueryXML = baseQueryXML.replace("UPPERCORNER", upperCorner);
            baseQueryXML = baseQueryXML.replace("LOWERCORNER", lowerCorner);
            //String selectionBoundsGML = gml_writer.toString(selectionBounds.)
           // Messagebox.show(baseQueryXML);
            String response = POSTRequest(baseQueryXML);
            String speciesList = parseIntersection(response,selection);
//            baseQueryXML = baseQueryXML.replace("ENVELOPE",selectionBoundsGML);
//            String response = POSTRequest(baseQueryXML);
        }
        catch (Exception e) {}
//        String baseQueryXML = "<wfs:GetFeature service=\"WFS\" version=\"1.1.0\" xmlns:topp=\"http://www.openplans.org/topp\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.1.0/wfs.xsd\"> <wfs:Query typeName=\"ALA:occurrences\"> <wfs:PropertyName>ALA:scientificname</wfs:PropertyName>";
//        String filterPart = "<ogc:Filter><Intersects><PropertyName>the_geom</PropertyName><gml:Polygon srsName=\"EPSG:4326\"><gml:outerBoundaryIs><gml:LinearRing><gml:coordinates cs=\" \" decimal=\".\" ts=\",\">COORDINATES</gml:coordinates></gml:LinearRing></gml:outerBoundaryIs></gml:Polygon></Intersects></ogc:Filter></wfs:Query></wfs:GetFeature>";
//        String coordinateString = selectionGeom.replace("POLYGON", "").replace(")", "").replace("(", "");
//        String request = baseQueryXML + filterPart.replace("COORDINATES", coordinateString);
//        try {
//            Messagebox.show(request);
//            String response = POSTRequest(request);
//            // Messagebox.show(response);
//
//        } catch (Exception e) { //FIXME
//        }
    }

    /**
     * Constructs a wfs 'species within bounding box query' for the given geometry
     * @param selectionGeom geometry of the box
     */
    public void wfsQueryBBox(String selectionGeom) {
        String baseQueryXML = "<wfs:GetFeature  service=\"WFS\" version=\"1.1.0\" xmlns:topp=\"http://www.openplans.org/topp\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.1.0/wfs.xsd\"> <wfs:Query typeName=\"ALA:occurrences\"> <wfs:PropertyName>ALA:scientificname</wfs:PropertyName><ogc:Filter><ogc:BBOX><ogc:PropertyName>the_geom</ogc:PropertyName><gml:Envelope srsName=\"http://www.opengis.net/gml/srs/epsg.xml#4326\"><gml:lowerCorner>LOWERCORNER</gml:lowerCorner><gml:upperCorner>UPPERCORNER</gml:upperCorner></gml:Envelope></ogc:BBOX></ogc:Filter></wfs:Query></wfs:GetFeature>";
        selectionGeom = selectionGeom.replace("(", "");
        selectionGeom = selectionGeom.replace(")", "");
        String upperCorner = selectionGeom.replace("POLYGON", "").split(",")[3].toString();
        String lowerCorner = selectionGeom.replace("POLYGON", "").split(",")[1].toString();
        baseQueryXML = baseQueryXML.replace("UPPERCORNER", upperCorner);
        baseQueryXML = baseQueryXML.replace("LOWERCORNER", lowerCorner);

        //       String completeQuery = baseQuery + selection + ")";
        try {
            
            String response = POSTRequest(baseQueryXML);
            String speciesList = parse(response);
            //Messagebox.show(response);

        } catch (Exception e) { //FIXME
        }
    }

    /**
     * Sends and POST to the wfs
     * @param query
     * @return
     */
    public String POSTRequest(String query) {
        try {
            // Construct data
            //String data = URLEncoder.encode(query, "UTF-8");// + "=" + URLEncoder.encode(query, "UTF-8");

            // Send data 
            URL url = new URL(geoServer + "/geoserver/wfs?");
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setRequestProperty("Content-Type", "application/xml");

            DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
            wr.writeBytes(query);
            wr.flush();
            // Get the response
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            String response = "";
            while ((line = rd.readLine()) != null) {
                response += line;
            }
            //Parse the response
         //   Messagebox.show(response);
           // String speciesList = parse(response);
            wr.close();
            // rd.close();
            return response;
        } catch (Exception e) {
            return e.getMessage();
        }

    }

    /**
     * Takes a WFS response and returns species list
     * @param responseXML The response of an WFS query.
     * @return
     */
    public String parse(String responseXML) throws ParserConfigurationException, XPathExpressionException, SAXException, IOException, InterruptedException {
        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
        domFactory.setNamespaceAware(false);
        DocumentBuilder builder = domFactory.newDocumentBuilder();
        InputStream is = new java.io.ByteArrayInputStream(responseXML.getBytes());
        Document resultDoc = builder.parse(is);
        //Get a list of names
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        //Ugly xpath expression - using local-name() so that I can ignore namespaces
        XPathExpression speciesExpr = xpath.compile("//*[local-name()='FeatureCollection']//*[local-name()='featureMembers']//*[local-name()='occurrences']//*[local-name()='scientificname']/text()");

        NodeList species = (NodeList) speciesExpr.evaluate(resultDoc, XPathConstants.NODESET);
        speciesSet = new TreeSet();
        for (int i = 0; i < species.getLength(); i++) {
            speciesSet.add((String) species.item(i).getNodeValue());
        }
  

        results = (String[]) speciesSet.toArray(new String[speciesSet.size()]);
        popup_listbox_results.setModel(new SimpleListModel(speciesSet.toArray()));


        popup_label.setValue("Number of species: " + speciesSet.size());

        //popup_results.open(40, 150);
        openResults();
        
        return String.valueOf(speciesSet.size());
    }

    public String parseIntersection(String responseXML, Geometry selection) throws ParserConfigurationException, XPathExpressionException, SAXException, IOException, InterruptedException {
        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
        domFactory.setNamespaceAware(false);
        DocumentBuilder builder = domFactory.newDocumentBuilder();
        InputStream is = new java.io.ByteArrayInputStream(responseXML.getBytes());
        Document resultDoc = builder.parse(is);
//        try {
//        Messagebox.show(responseXML);
//        } catch (Exception e) {}
        //Get a list of names
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        //Ugly xpath expression - using local-name() so that I can ignore namespaces
        XPathExpression speciesExpr = xpath.compile("//*[local-name()='FeatureCollection']//*[local-name()='featureMembers']//*[local-name()='occurrences']//*[local-name()='scientificname']/text()");

        NodeList species = (NodeList) speciesExpr.evaluate(resultDoc, XPathConstants.NODESET);
        speciesSet = new TreeSet();
        for (int i = 0; i < species.getLength(); i++) {
            speciesSet.add((String) species.item(i).getNodeValue());
        }


       results = (String[]) speciesSet.toArray(new String[speciesSet.size()]);
        popup_listbox_results.setModel(new SimpleListModel(speciesSet.toArray()));


        popup_label.setValue("Number of species: " + speciesSet.size());

        //popup_results.open(40, 150);
        openResults();

        return String.valueOf(speciesSet.size());
    }
    void openResults() {
        java.util.Map args = new java.util.HashMap();
        args.put("pid", "none");
        args.put("shape", getGeom());
        //args.put("manual","true");
        FilteringResultsWCController win = (FilteringResultsWCController) Executions.createComponents(
                "/WEB-INF/zul/AnalysisFilteringResults.zul", null, args);
        try {
            /* TODO: fix species listing for polygons service so this
             * is not required
             */
            //win.results = results;
            //win.populateList();
            win.doModal();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Gets the main pages controller so we can add a
     * drawing tool to the map
     * @return MapComposer = map controller class
     */
    private MapComposer getThisMapComposer() {

        MapComposer mapComposer = null;
        Page page = getPage();
        mapComposer = (MapComposer) page.getFellow("mapPortalPage");

        return mapComposer;
    }

    public void onClick$download() {
        StringBuffer sb = new StringBuffer();
        for (String s : results) {
            sb.append(s);
            sb.append("\r\n");
        }
        org.zkoss.zhtml.Filedownload.save(sb.toString(), "text/plain", "species.csv");
//		}else{

    }

     private String getInfo(String urlPart) {
        try {
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(satServer + "/alaspatial/ws" + urlPart); // testurl
            get.addRequestHeader("Accept", "application/json, text/javascript, */*");

            int result = client.executeMethod(get);

            //TODO: test results
            String slist = get.getResponseBodyAsString();

            return slist;
        } catch (Exception ex) {
            //TODO: error message
            //Logger.getLogger(FilteringWCController.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("getInfo.error:");
            ex.printStackTrace(System.out);
        }
        return null;
    }
}