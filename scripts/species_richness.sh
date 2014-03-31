#!/bin/bash

# This script generates an occurrence density layer using data generated by the biocache and loads it into the spatial portal

export DATE=$(date +built_%y%m%d)
export PTH="/data/ala/data/layers"
export JAVA_CLASSPATH="$PTH/process/layer-ingestion-1.0-SNAPSHOT/layer-ingestion-1.0-SNAPSHOT.jar:$PTH/process/layer-ingestion-1.0-SNAPSHOT/lib/*"
export GEOSERVER_USRPWD="user:password"
export GEOSERVER_URL="http://localhost:8082/geoserver"
export OUTPUTDIR="$PTH/process/species_richness/$DATE"
export GDAL_TRANSLATE="/data/ala/utils/gdal-1.9.0/apps/gdal_translate"

# Create directory to store output files:
mkdir $OUTPUTDIR 

echo $DATE > $OUTPUTDIR/build.log

# download generated data from the biocache
echo "downloading data from biocache" >> $OUTPUTDIR/build.log 
wget http://biocache.ala.org.au/archives/exports/cell-species-lists-0.1-degree.txt 1>> $OUTPUTDIR/build.log 2>&1
mv *.txt $OUTPUTDIR

# generate ASCII grid and DIVA grid
echo "generating grid files" 1>> $OUTPUTDIR/build.log
java -Xmx8G -cp ${JAVA_CLASSPATH} org.ala.spatial.analysis.layers.SpeciesRichnessLayerGenerator 0.1 $OUTPUTDIR/cell-species-lists-0.1-degree.txt $OUTPUTDIR srichness 1>> $OUTPUTDIR/build.log 2>&1


cp $OUTPUTDIR/srichness.gr* $PTH/ready/diva 1>> $OUTPUTDIR/build.log 2>&1

#Fix mode and ownership of diva grids
chmod 777 $PTH/ready/diva/* 1>> $OUTPUTDIR/build.log 2>&1
chown tomcat:wheel $PTH/ready/diva/* 1>> $OUTPUTDIR/build.log 2>&1

# Generate geotiff
echo "generating geotiff" >> $OUTPUTDIR/build.log
$GDAL_TRANSLATE -of GTiff -ot Float32 -a_srs EPSG:4326 $OUTPUTDIR/srichness.asc $OUTPUTDIR/srichness.tif 1>> $OUTPUTDIR/build.log 2>&1
cp $OUTPUTDIR/srichness.tif $PTH/ready/geotiff 1>> $OUTPUTDIR/build.log 2>&1

#Fix mode and ownership of geotiffs
chmod 777 $PTH/ready/geotiff/* 1>> $OUTPUTDIR/build.log 2>&1
chown tomcat:wheel $PTH/ready/geotiff/* 1>> $OUTPUTDIR/build.log 2>&1

# Generate legend
echo "generating legend" >> $OUTPUTDIR/build.log
java -Xmx8G -cp ${JAVA_CLASSPATH} org.ala.layers.legend.GridLegend $PTH/ready/diva/srichness $PTH/test/srichness 8 1 1>> $OUTPUTDIR/build.log 2>&1

# Upload to geoserver
echo "uploading to geoserver" 1>> $OUTPUTDIR/build.log
curl -u $GEOSERVER_USRPWD -XPUT -H "Content-type: text/plain"  -d "file://$PTH/ready/geotiff/srichness.tif" $GEOSERVER_URL/rest/workspaces/ALA/coveragestores/srichness/external.geotiff 1>> $OUTPUTDIR/build.log
curl -u $GEOSERVER_USRPWD -XPOST -H "Content-type: text/xml"  -d "<style><name>srichness_style</name><filename>srichness.sld</filename></style>"  $GEOSERVER_URL/rest/styles 1>> $OUTPUTDIR/build.log
curl -u $GEOSERVER_USRPWD -XPUT -H "Content-type: application/vnd.ogc.sld+xml"  -d @$PTH/test/srichness.sld $GEOSERVER_URL/rest/styles/srichness_style 1>> $OUTPUTDIR/build.log
curl -u $GEOSERVER_USRPWD -XPUT -H "Content-type: text/xml"   -d "<layer><enabled>true</enabled><defaultStyle><name>srichness_style</name></defaultStyle></layer>" $GEOSERVER_URL/rest/layers/ALA:srichness 1>> $OUTPUTDIR/build.log

# Remove pre-existing geoserver gwc cached tiles for species richness layer
rm -r /data2/ala/data/geoserver_data_dir/gwc/ALA_srichness/*

# Regenerate layer analysis and distances
echo "regenerating layer analysis and distances" 1>> $OUTPUTDIR/build.log
rm -f /data/ala/data/layers/analysis/0.5/el899.gr* 1>> $OUTPUTDIR/build.log 2>&1
rm -f /data/ala/data/layers/analysis/0.01/el899.gr* 1>> $OUTPUTDIR/build.log 2>&1
rm -f /data/ala/data/layers/analysis/0.0025/el899.gr* 1>> $OUTPUTDIR/build.log 2>&1
cp /data/ala/data/alaspatial/layerDistances.properties /data/ala/data/alaspatial/layerDistances.properties_old 1>> $OUTPUTDIR/build.log 2>&1
sed -i '/el899\|el899/d' /data/ala/data/alaspatial/layerDistances.properties 1>> $OUTPUTDIR/build.log 2>&1

sh $PTH/process/layer-ingestion-1.0-SNAPSHOT/environmental_background_processing.sh 1>> $OUTPUTDIR/build.log 2>&1

echo "finished" 1>> $OUTPUTDIR/build.log

cp $OUTPUTDIR/build.log /data/ala/runtime/output/srichness.log
