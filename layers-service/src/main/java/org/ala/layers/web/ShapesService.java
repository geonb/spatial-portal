/**************************************************************************
 *  Copyright (C) 2010 Atlas of Living Australia
 *  All Rights Reserved.
 *
 *  The contents of this file are subject to the Mozilla Public
 *  License Version 1.1 (the "License"); you may not use this file
 *  except in compliance with the License. You may obtain a copy of
 *  the License at http://www.mozilla.org/MPL/
 *
 *  Software distributed under the License is distributed on an "AS
 *  IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 *  implied. See the License for the specific language governing
 *  rights and limitations under the License.
 ***************************************************************************/
package org.ala.layers.web;

import java.sql.ResultSet;
import java.sql.SQLException;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import org.ala.layers.dao.ObjectDAO;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author Adam
 */
@Controller
public class ShapesService {

    /**
     * Log4j instance
     */
    protected Logger logger = Logger.getLogger(this.getClass());
    
    @Resource(name="objectDao")
    private ObjectDAO objectDao;

    /*
     * return a shape as kml
     */
    @RequestMapping(value = "/shape/{type}/{id}", method = RequestMethod.GET)
    public
    @ResponseBody
    String findShape(@PathVariable("type") String type, @PathVariable("id") String id, HttpServletRequest req) {
        try {

            //validate object id
            id = cleanObjectId(id);

//            List<Objects> objects = objectDao.getObjectsById(id);
//            if (objects.size() > 0) {
//                Geometry geom = objects.get(0).getGeometry();
//                if (type.equalsIgnoreCase("wkt")) {
//                    WKTWriter wkt = new WKTWriter();
//                    return wkt.write(geom);
//                } else if (type.equalsIgnoreCase("kml")) {
//                    Encoder e = new Encoder(new KMLConfiguration());
//                    e.setIndenting(true);
//                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
//                    e.encode(geom, KML.Geometry, baos);
//                    String kmlGeometry = new String(baos.toByteArray());
//                    return kmlGeometry.substring(kmlGeometry.indexOf('\n'));
//                } else if (type.equalsIgnoreCase("geojson")) {
//                    return "Not supported yet.";
//                }
//
//            } else {
//                return "";
//            }

            if (type.equalsIgnoreCase("wkt") || type.equalsIgnoreCase("kml") || type.equalsIgnoreCase("geojson") || type.equalsIgnoreCase("shp")) {
                return objectDao.getObjectsGeometryById(id, type);
            } else {
                return "'" + type + "' type not supported yet.";
            }

         } catch (Exception e) {
            logger.error("An error has occurred retrieving '" + type + "' for object id " + id);
            logger.error(ExceptionUtils.getFullStackTrace(e));
        }
        return "";
    }

    private String cleanObjectId(String id) {
        return id.replaceAll("[^a-zA-Z0-9]:", "");
    }
}
