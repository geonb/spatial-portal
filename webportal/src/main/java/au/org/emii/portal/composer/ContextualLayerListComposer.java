package au.org.emii.portal.composer;

import au.org.emii.portal.settings.SettingsSupplementary;
import au.org.emii.portal.util.LayerUtilities;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.ala.spatial.analysis.web.AreaMapPolygon;
import org.ala.spatial.analysis.web.ContextualLayerSelection;
import org.ala.spatial.util.CommonData;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.Popup;
import org.zkoss.zul.SimpleTreeModel;
import org.zkoss.zul.SimpleTreeNode;
import org.zkoss.zul.Space;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Tree;
import org.zkoss.zul.Treecell;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.TreeitemRenderer;
import org.zkoss.zul.Treerow;

/**
 *
 * @author angus
 */
public class ContextualLayerListComposer extends UtilityComposer {

    public Tree tree;
    private Popup pupLayerAction;
    private Toolbarbutton llAdd;
    private Toolbarbutton llInfo;
    private ArrayList empty = new ArrayList();
    private MapComposer mc;
    ContextualLayerSelection contextualLayerSelection;
    SettingsSupplementary settingsSupplementary;

    @Override
    public void afterCompose() {
        super.afterCompose();

        if (tree == null) {
            System.out.println("tree is null");
        } else {
            System.out.println("tree is ready");
        }

        mc = getThisMapComposer();

        System.out.println("Loading the ContextualLayerListComposer");
        iterateAndLoad();
    }

    private MapComposer getThisMapComposer() {

        MapComposer mapComposer = null;
        Page page = getPage();
        mapComposer = (MapComposer) page.getFellow("mapPortalPage");

        return mapComposer;
    }

    public void iterateAndLoad() {
        try {

            ArrayList top = new ArrayList();

            TreeMap htCat1 = new TreeMap();
            TreeMap htCat2 = new TreeMap();
            //System.out.println("LAYERLIST>>>>>>>>>>" + (String) llist);

            JSONArray layerlist = CommonData.getLayerListJSONArray();//JSONArray.fromObject(llist);
            for (int i = 0; i < layerlist.size(); i++) {
                JSONObject jo = layerlist.getJSONObject(i);

                if (!jo.getBoolean("enabled")) {
                    continue;
                }

                List classNodes = new ArrayList();
                if (jo.getString("type").equalsIgnoreCase("Contextual")) {
                    classNodes = getContextualClasses(jo);

                    SimpleTreeNode stn;
                    if (classNodes.isEmpty()) {
                        stn = new SimpleTreeNode(jo, empty);
                    } else {
                        stn = new SimpleTreeNode(jo, classNodes);
                    }
                    addToMap(htCat1, htCat2, jo.getString("classification1"), jo.getString("classification2"), stn);
                }
            }

            Iterator it1 = htCat1.keySet().iterator();
            while (it1.hasNext()) {
                String catKey = (String) it1.next();
                JSONObject joCat = JSONObject.fromObject("{displayname:'" + catKey + "',type:'node',subtype:" + LayerUtilities.CONTEXTUAL + "}");

                //sort 2nd level branches
                ArrayList sorted = (ArrayList) htCat1.get(catKey);
                java.util.Collections.sort(sorted, new Comparator() {

                    @Override
                    public int compare(Object a, Object b) {
                        SimpleTreeNode sa = (SimpleTreeNode) a;
                        SimpleTreeNode sb = (SimpleTreeNode) b;
                        JSONObject ja = JSONObject.fromObject(sa.getData());
                        JSONObject jb = JSONObject.fromObject(sb.getData());
                        String na = ja.getString("displayname");
                        String nb = jb.getString("displayname");
                        na = (na.contains(">")) ? na.split(">")[1] : na;
                        nb = (nb.contains(">")) ? nb.split(">")[1] : nb;

                        return na.compareToIgnoreCase(nb);
                    }
                });
                SimpleTreeNode cat = new SimpleTreeNode(joCat, sorted);
                top.add(cat);
            }


            SimpleTreeNode root = new SimpleTreeNode("ROOT", top);
            SimpleTreeModel stm = new SimpleTreeModel(root);
            tree.setModel(stm);

            renderTree();
        } catch (Exception e) {
            //FIXME:
            e.printStackTrace();
        }

    }

    private List getContextualClasses(JSONObject joLayer) {
        return CommonData.getContextualClasses(joLayer);
    }

    private void addToMap(TreeMap htCat1, TreeMap htCat2, String cat1, String cat2, SimpleTreeNode treeNode) {

        if (cat1.trim().equals("") || cat1.trim().equals("?") || cat1.trim().equals("null")) {
            cat1 = "Other";
        }
        if (cat2.trim().equals("") || cat2.trim().equals("?") || cat2.trim().equals("null")) {
            //cat2 = "Other";
            //System.out.println("Adding layer to cat1.other as cat2=" + cat2);
            ArrayList alCat1 = (ArrayList) htCat1.get(cat1);
            if (alCat1 == null) {
                alCat1 = new ArrayList();
            }
            alCat1.add(treeNode);
            htCat1.put(cat1, alCat1);

        } else {

            // first check if cat1 already exists
            // if yes, grab the cat2 list and add add to its AL
            // else, create a new one and add it to cat1.list

            String cat2_full = cat1 + ">" + cat2;
            ArrayList alCat2 = (ArrayList) htCat2.get(cat2_full);
            if (alCat2 == null) {
                alCat2 = new ArrayList();
            }
            //System.out.println("add new stn to: " + cat1 + " > " + cat2);
            alCat2.add(treeNode);
            if (!htCat2.containsKey(cat2_full)) {
                htCat2.put(cat2_full, alCat2);
            }

            ArrayList alCat1 = (ArrayList) htCat1.get(cat1);
            if (alCat1 == null) {
                alCat1 = new ArrayList();
            }
            //System.out.println("\tAdding new cat2");
            String subtype = ((JSONObject)treeNode.getData()).getString("type");
            JSONObject joCat2 = JSONObject.fromObject("{displayname:'" + cat2_full + "',type:'node',subtype:" 
                    +((subtype.equalsIgnoreCase("environmental"))?LayerUtilities.GRID:LayerUtilities.CONTEXTUAL)
                    + "}");
            SimpleTreeNode stnCat2 = new SimpleTreeNode(joCat2, alCat2);
            //System.out.println("\tadding cat2.stn (" + cat2 + ") to " + cat1 + " :: " + alCat1.contains(stnCat2) + " ::: " + alCat1.indexOf(stnCat2));
            //System.out.println("\t=======================" + stnCat2);
            boolean found = false;
            for (int i = 0; i < alCat1.size(); i++) {
                // System.out.print("\t\t " + alCat1.get(i));
                if (stnCat2.toString().equals(alCat1.get(i).toString())) {
                    //      System.out.println(": found");
                    found = true;
                    break;
                } else {
                    //      System.out.println(": not this");
                }
            }
            if (!found) {
                alCat1.add(stnCat2);
            }
            //System.out.println("\t=======================");

            if (!htCat1.containsKey(cat1)) {
                htCat1.put(cat1, alCat1);
            } else {
                //  System.out.println("\thad existing");
            }

        }

    }

    void initALC() {
        if(contextualLayerSelection == null) {
            try {
                contextualLayerSelection = (ContextualLayerSelection) getMapComposer().getFellow("contextuallayerselectionwindow");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void renderTree() {

        tree.setTreeitemRenderer(new TreeitemRenderer() {

            @Override
            public void render(Treeitem item, Object data) throws Exception {

                SimpleTreeNode t = (SimpleTreeNode) data;

                JSONObject joLayer = JSONObject.fromObject(t.getData());

                Treerow tr = null;
                /*
                 * Since only one treerow is allowed, if treerow is not null,
                 * append treecells to it. If treerow is null, construct a new
                 * treerow and attach it to item.
                 */
                if (item.getTreerow() == null) {
                    tr = new Treerow();
                    tr.setParent(item);
                } else {
                    tr = item.getTreerow();
                    tr.getChildren().clear();
                }

                String displayname = joLayer.getString("displayname");
                displayname = (displayname.contains(">")) ? displayname.split(">")[1] : displayname;
                Treecell tcName = new Treecell();
                if (!joLayer.getString("type").equals("node")) {
                    Image img = new Image();
                    img.setSrc("/img/information.png");
                    img.addEventListener("onClick", new EventListener() {

                        @Override
                        public void onEvent(Event event) throws Exception {                            
                            JSONObject jo = JSONObject.fromObject(event.getTarget().getParent().getParent().getAttribute("lyr"));
                            String s = jo.getString("uid");
                            String metadata = CommonData.satServer + "/layers/" + s;
                            mc.activateLink(metadata, "Metadata", false);
                        }
                    });
                    img.setParent(tcName);

                    Space sp = new Space();
                    sp.setParent(tcName);
                }
                Label lbl = new Label(displayname);
                lbl.setParent(tcName);

                //Treecell  tcDesc = new Treecell(joLayer.getString("displayname"));

                Treecell tcAdd = new Treecell();
                Treecell tcInfo = new Treecell();

                //System.out.print("name: " + joLayer.getString("displayname:") + " --- ");
                //System.out.println("type: " + joLayer.getString("type"));

                if (!joLayer.getString("type").equals("node")) {

                    // add the "add" button
                    //tcAdd = new Treecell();
                    tcAdd.setImage("/img/add.png");

                    // add the "info" button
                    //tcInfo = new Treecell();
                    tcInfo.setImage("/img/information.png");

                    // set the layer data for the row
                    tr.setAttribute("lyr", joLayer);

                    //item.setPopup(pupLayerAction);


                } else {
                    //tcAdd.setLabel(displayname);
                    //tcInfo.setLabel("");
                    //tcName.setLabel("");
                }

                if (joLayer.getString("type").equals("class")) {
                    tcAdd.setImage("/img/add.png");
                }



                // Attach onclick events:
                if (!joLayer.getString("type").equals("node")) {

                    /*
                    tcName.addEventListener("onClick", new EventListener() {

                    @Override
                    public void onEvent(Event event) throws Exception {
                    Treecell tc = (Treecell) event.getTarget();
                    tc.setPopup(pupLayerAction);

                    }

                    });
                     * 
                     */

                    // tcAdd
                    tr.addEventListener("onClick", new EventListener() {

                        @Override
                        public void onEvent(Event event) throws Exception {
                            //Object o = event.getTarget().getId();
                            //Treecell tc = (Treecell) event.getTarget();
                            //JSONObject joLayer = JSONObject.fromObject(tc.getParent().getAttribute("lyr"));
                            JSONObject joLayer = JSONObject.fromObject(tree.getSelectedItem().getTreerow().getAttribute("lyr"));
                            if (!joLayer.getString("type").contentEquals("class")) {

                                String metadata = CommonData.satServer + "/layers/" + joLayer.getString("uid");

                                initALC();
                                contextualLayerSelection.setLayer(joLayer.getString("displayname"), joLayer.getString("displaypath"), metadata,
                                        joLayer.getString("type").equalsIgnoreCase("environmental")?LayerUtilities.GRID:LayerUtilities.CONTEXTUAL);

//                                mc.addWMSLayer(joLayer.getString("displayname"),
//                                        joLayer.getString("displaypath"),
//                                        (float) 0.75, metadata);
                            } else {
                                String classAttribute = joLayer.getString("classname");
                                String classValue = joLayer.getString("displayname");
                                String layer = joLayer.getString("layername");
                                String displaypath = joLayer.getString("displaypath") + "&cql_filter=(" + classAttribute + "='" + classValue + "');include";
                                //Filtered requests don't work on
                                displaypath = displaypath.replace("gwc/service/", "");
                                // Messagebox.show(displaypath);
                                String metadata = CommonData.satServer + "/layers/" + joLayer.getString("uid");
                                initALC();
                                contextualLayerSelection.setLayer(layer + " - " + classValue, displaypath, metadata, joLayer.getString("type").equalsIgnoreCase("environmental")?LayerUtilities.GRID:LayerUtilities.CONTEXTUAL);

//                                mc.addWMSLayer(layer + " - " + classValue,
//                                        displaypath,
//                                        (float) 0.75, metadata);
                            }

//                            mc.updateUserLogMapLayer("env - tree - add", joLayer.getString("uid")+"|"+joLayer.getString("displayname"));

                            //close parent if it is 'addlayerwindow'
//                            try {
//                                getRoot().getFellow("addlayerwindow").detach();
//                            } catch (Exception e) {}
                        }
                    });


                    tcInfo.addEventListener("onClick", new EventListener() {

                        @Override
                        public void onEvent(Event event) throws Exception {

                            Object o = event.getTarget().getId();
                            Treecell tc = (Treecell) event.getTarget();
                            JSONObject joLayer = JSONObject.fromObject(tc.getParent().getAttribute("lyr"));

                            String metadata = CommonData.satServer + "/layers/" + joLayer.getString("uid");

                            mc.activateLink(metadata, "Metadata", false);

                            mc.updateUserLogMapLayer("env - tree - info", joLayer.getString("uid")+"|"+joLayer.getString("displayname"));

                        }
                    });
                }

                tcName.setParent(tr);
                item.setOpen(false);
            }
        });

    }
}