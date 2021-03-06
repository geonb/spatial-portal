package au.org.emii.portal.session;

import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.value.BoundingBox;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the state of the portal.
 * <p/>
 * On loading the web application a default PortalSession is built, when a user
 * accesses the portal, the default instance is COPIED into their HTTP session
 * where it can be manipulated through the ZK GUI without affecting the other
 * sessions
 *
 * @author geoff
 */
public class PortalSession implements Cloneable, Serializable {

    public static final int UNKNOWN = -1;
    public static final int LAYER_FACILITY_TAB = 0;
    /**
     * The current view we are displaying to the user
     */
    private int currentLayerTab = LAYER_FACILITY_TAB;
    /**
     * The current view we need for displaying the menu EG, we may be displaying
     * the regions panel but we want to display an invisible menu on the
     * facilities panel because the user hasn't selected a radio button yet
     */
    private int tabForCurrentMenu = LAYER_FACILITY_TAB;
    public static final int LAYER_REGION_TAB = 1;
    public static final int LAYER_USER_TAB = 2;
    public static final int LAYER_REALTIME_TAB = 3;
    public static final int LAYER_TAB = 0;
    /**
     * Default navigation tab
     */
    private int currentNavigationTab = LAYER_TAB;
    public static final int SEARCH_TAB = 1;
    public static final int LINK_TAB = 2;
    public static final int START_TAB = 3;
    public static final int AREA_TAB = 4;
    public static final int MAP_TAB = 5;
    private static final long serialVersionUID = 1L;

    /* Datasources - Discovery and Service both resolve to MapLayer instances,
     * static links are handled separately
     */
    private List<MapLayer> mapLayers = null;
    private List<MapLayer> activeLayers = null;
    private List<MapLayer> userDefinedLayers = null;
    private String onIframeMapFullyLoaded
            = "alert('onIframeMapFullyLoaded function has not been replaced"
            + " - possible race conditon'); ";
    private BoundingBox defaultBoundingbox = null;
    /**
     * Flag to indicate whether the map has been loaded successfully if false,
     * no openlayers javascript will be executed
     */
    private boolean mapLoaded = false;
    /**
     * Are we hiding the left menu?
     */
    private boolean maximised = false;
    /**
     * Baselayer name
     */
    private String baseLayer = "normal";

    public PortalSession() {
        reset();
    }

    public int getCurrentNavigationTab() {
        return currentNavigationTab;
    }

    public void setCurrentNavigationTab(int currentNavigationTab) {
        this.currentNavigationTab = currentNavigationTab;
    }

    public List<MapLayer> getMapLayers() {
        return mapLayers;
    }

    public void setMapLayers(List<MapLayer> mapLayers) {
        this.mapLayers = mapLayers;
    }

    public void addMapLayer(MapLayer mapLayer) {
        mapLayers.add(mapLayer);
    }

    public List<MapLayer> getActiveLayers() {
        return activeLayers;
    }

    public void setActiveLayers(List<MapLayer> activeLayers) {
        this.activeLayers = activeLayers;
    }

    public List<MapLayer> getUserDefinedLayers() {
        return userDefinedLayers;
    }

    public void setUserDefinedLayers(List<MapLayer> userDefined) {
        this.userDefinedLayers = userDefined;
    }

    public int getCurrentLayerTab() {
        return currentLayerTab;
    }

    public void setLayerTab(int layerTab) {
        this.currentLayerTab = layerTab;
    }

    /**
     * Check if the user defined view is displayable
     *
     * @return
     */
    public boolean isUserDefinedViewDisplayable() {
        return true;
    }

    public String getOnIframeMapFullyLoaded() {
        return onIframeMapFullyLoaded;
    }

    public void setOnIframeMapFullyLoaded(String onIframeMapFullyLoaded) {
        this.onIframeMapFullyLoaded = onIframeMapFullyLoaded;
    }

    public int getTabForCurrentMenu() {
        return tabForCurrentMenu;
    }

    public void setTabForCurrentMenu(int viewForCurrentMenu) {
        this.tabForCurrentMenu = viewForCurrentMenu;
    }

    public BoundingBox getDefaultBoundingBox() {
        return defaultBoundingbox;
    }

    public void setDefaultBoundingbox(BoundingBox defaultBoundingbox) {
        this.defaultBoundingbox = defaultBoundingbox;
    }

    public boolean isMapLoaded() {
        return mapLoaded;
    }

    public void setMapLoaded(boolean mapLoaded) {
        this.mapLoaded = mapLoaded;
    }

    public boolean isMaximised() {
        return maximised;
    }

    public void setMaximised(boolean maximised) {
        this.maximised = maximised;
    }

    public final void reset() {
        mapLayers = new ArrayList<MapLayer>();
        activeLayers = new ArrayList<MapLayer>();
        userDefinedLayers = new ArrayList<MapLayer>();
    }

    public String getBaseLayer() {
        return baseLayer;
    }

    public void setBaseLayer(String baseLayer) {
        this.baseLayer = baseLayer;
    }

    public Object copy() throws CloneNotSupportedException {
        return this.clone();
    }
}
