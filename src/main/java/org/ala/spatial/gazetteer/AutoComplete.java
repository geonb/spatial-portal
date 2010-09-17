package org.ala.spatial.gazetteer;

import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.settings.SettingsSupplementary;
import org.zkoss.zul.Combobox;
import org.zkoss.zk.ui.event.InputEvent;
import java.util.Iterator;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.zkoss.zul.Comboitem;
import org.apache.http.impl.client.*;
import org.apache.http.HttpHost;
import org.apache.http.protocol.BasicHttpContext;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import org.zkoss.zhtml.Messagebox;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;

public class AutoComplete extends Combobox {
    private static final String GAZ_URL = "geoserver_url";
    private String gazServer = null;//"http://spatial.ala.org.au";

    public AutoComplete() {
        refresh(""); //init the child comboitems
    }

    public AutoComplete(String value) {
        super(value); //it invokes setValue(), which inits the child comboitems
    }



    @Override
    public void setValue(String value) {
        super.setValue(value);
        refresh(value); //refresh the child comboitems
    }

    public void onSelect(Event event) {
        if(gazServer == null && this.getParent() != null){
                SettingsSupplementary settingsSupplementary = settingsSupplementary = this.getThisMapComposer().getSettingsSupplementary();
                System.out.println("AC got SS: " + settingsSupplementary);
                gazServer = settingsSupplementary.getValue(GAZ_URL);
        }        
        if(gazServer == null){
            return;
        }

          String label = null;
            String entity = null;
            MapLayer mapLayer = null;

            //get the entity value from the button id
          //  entity = event.getData().toString(); // getTarget().getId();
            Comboitem item = this.getSelectedItem();
           //Listcell lc  = (Listcell)(item.getFirstChild());
            
            //avoid error seen when occasionally changing tabs
            if(item == null || item.getValue() == null){
                return;
            }
          label = item.getValue().toString(); //lc.getLabel();
//         try {
//          Messagebox.show(label);
//         }
//         catch (Exception e) {}
//            Label ln = (Label) event.getTarget().getFellow("ln" + entity);
//            label = ln.getValue();

            //get the current MapComposer instance
            MapComposer mc = getThisMapComposer();

            //add feature to the map as a new layer
            mapLayer = mc.addGeoJSON(item.getLabel(), gazServer + label);
    }
    
     /**
     * Gets the main pages controller so we can add a
     * layer to the map
     * @return MapComposer = map controller class
     */
    private MapComposer getThisMapComposer() {

        MapComposer mapComposer = null;
        Page page = this.getPage();
        mapComposer = (MapComposer) page.getFellow("mapPortalPage");

        return mapComposer;
    }

    /** Listens for what a user is entering.
     * @param evt
     */
    public void onChanging(InputEvent evt) {
        refresh(evt.getValue());
    }

    /** Refresh comboitem based on the specified value.
     */
    private void refresh(String val) {
        if(gazServer == null && this.getParent() != null){
                SettingsSupplementary settingsSupplementary = settingsSupplementary = this.getThisMapComposer().getSettingsSupplementary();
                System.out.println("AC got SS: " + settingsSupplementary);
                gazServer = settingsSupplementary.getValue(GAZ_URL);
        }
        if(gazServer == null){
            return;
        }

        HttpHost targetHost = new HttpHost(gazServer, 80, "http"); // "ec2-175-41-187-11.ap-southeast-1.compute.amazonaws.com"
        DefaultHttpClient httpclient = new DefaultHttpClient();
        BasicHttpContext localcontext = new BasicHttpContext();
        String searchString = val.trim().replaceAll("\\s+", "+");

        // check if searchString is empty, if so, set it to "a"
        // this is a hack to make sure a valid xml is returned
        // rather than just a "1"
        // TODO: angus fix this?
        searchString = (searchString.equals(""))?"a":searchString; 

        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
        domFactory.setNamespaceAware(true); 

        try {
            
            //Read in the xml response
            DocumentBuilder builder = domFactory.newDocumentBuilder();
            //String uri = targetHost.toString() + "/geoserver/rest/gazetteer/result.xml?q=" + searchString;
            String uri = gazServer + "/geoserver/rest/gazetteer/result.xml?q=" + searchString;
            System.out.println("Looking gaz up on: " + uri); 
            //Messagebox.show(uri);
            Document resultDoc = builder.parse(uri);

            //Get a list of names from the xml
            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();
            XPathExpression expr = xpath.compile("//search/results/result/name/text()");
            XPathExpression descriptionsExpr = xpath.compile("//search/results/result/description/text()");
            XPathExpression linksExpr = xpath.compile("//search/results/result/@*");

            Object result = expr.evaluate(resultDoc, XPathConstants.NODESET);
            NodeList descriptions = (NodeList) descriptionsExpr.evaluate(resultDoc, XPathConstants.NODESET);
            NodeList links = (NodeList) linksExpr.evaluate(resultDoc, XPathConstants.NODESET);

            NodeList nodes = (NodeList) result;
          
            Iterator it = getItems().iterator();
            
            for(int i=0;i<nodes.getLength();i++) {
                
                String itemString = (String) nodes.item(i).getNodeValue();
                String link = (String) links.item(i).getNodeValue();
                String description = (String) descriptions.item(i).getNodeValue();
                // Messagebox.show(link);
                if (it != null && it.hasNext()) {
                    Comboitem ci = (Comboitem) it.next();
                    ci.setLabel(itemString);
                    ci.setValue(link);
                    ci.setDescription(description);
                } else {
                    it = null;
                    Comboitem ci = new Comboitem();
                    ci.setLabel(itemString);
                    ci.setValue(link);
                    ci.setDescription(description);
                    ci.setParent(this);
                    //new Comboitem(itemString).setParent(this);
                }

            }

            while (it != null && it.hasNext()) {
                it.next();
                it.remove();
            }

        } catch (Exception e) {

        }

        
    }

}