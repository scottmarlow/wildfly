#!/bin/bash
# 
# ee8artifact2ee9.sh
# convert specified maven artifact to EE 9 and add to your local maven repo under new GAV

# https://github.com/bjhargrave/transformer project folder

echo "ee8artifact2ee9 converts specified maven artifact to EE 9 and add to your local maven repo under new GAV"

transformer="${TRANSFORMER_SRC_FOLDER}"
groupid="${GROUPID}"
artifactid="${ARTIFACTID}"
version="${VERSION}"
outputversion="${OUTPUTVERSION}"
outputjarname="${OUTPUTJARNAME}"

function usage()
{
    echo "convert specified maven artifact to EE 9 and add to your local maven repo under new GAV"
    echo ""
    echo "./ee8artifact2ee9.sh"
    echo "\t-h --help"
    echo "\t--transformer=$TRANSFORMER_SRC_FOLDER"
    echo "\t--groupid=$GROUPID"
    echo "\t--artifactid=$ARTIFACTID"
    echo "\t--version=$VERSION"
    echo "\t--outputversion=$OUTPUTVERSION"
    echo "\t--outputjarname=$OUTPUTJARNAME"
    echo ""
}

while [ "$1" != "" ]; do
    PARAM=`echo $1 | awk -F= '{print $1}'`
    VALUE=`echo $1 | awk -F= '{print $2}'`
    case $PARAM in
        -h | --help)
            usage
            exit
            ;;
        --outputversion)
            outputversion=$VALUE
            ;;
        --outputjarname)
            outputjarname=$VALUE
            ;;
        --artifactid)
            artifactid=$VALUE
            ;;
        --version)
            version=$VALUE
            ;;
        --transformer)
            transformer=$VALUE
            ;;
        --groupid)
            groupid=$VALUE
            ;;
        *)
            echo "ERROR: unknown parameter \"$PARAM\""
            usage
            exit 1
            ;;
    esac
    shift
done

echo "inputs:"
echo "transformer=$transformer"
echo "groupid=$groupid"
echo "artifactid=$artifactid"
echo "version=$version"
echo "outputversion=$outputversion"
echo "outputjarname=$outputjarname"

echo "verifying inputs..."

if [ -z "$transformer" ]; then
  echo "TRANSFORMER_SRC_FOLDER needs to be exported, set to local copy of https://github.com/bjhargrave/transformer."
  exit 1
fi

if [ -z "$groupid" ]; then 
  echo "GROUPID of artifact to transform needs to be exported."
  exit 1
fi

if [ -z "$artifactid" ]; then 
  echo "ARTIFACTID of artifact to transform needs to be exported."
  exit 1;
fi

if [ -z "$version" ]; then 
  echo "VERSION of artifact to transform needs to be exported."
  exit 1
fi

echo "will do transformation in /tmp/ee82ee9 folder, any current jars in /tmp/ee82ee9 will be removed."
mkdir -p /tmp/ee82ee9
cd /tmp/ee82ee9
rm -f *.jar

mvn dependency:get -DrepoUrl=http://.../ -Dartifact=$groupid:$artifactid:$version:jar -Dtransitive=false
mvn dependency:copy -DrepoUrl=http://.../ -Dartifact=$groupid:$artifactid:$version:jar -Dtransitive=false -DoutputDirectory=/tmp/ee82ee9

echo "downloaded jar for $groupid:$artifactid:$version"
ls *.jar

if [ -z "outputjarname" ]; then 
  echo "OUTPUTJARNAME for transformer output, needs to be exported."
  exit 1
fi

if [ -z "outputversion" ]; then 
  echo "OUTPUTVERSION for transformer output, needs to be exported."
  exit 1
fi

# transform the jars just obtained from maven repo
count=$(ls -l /tmp/ee82ee9/*.jar 2>/dev/null | wc -l)

echo "will process $count jars"

if [ "$count" -eq "0" ]; then
  echo "error: could not obtain jar for $groupid:$artifactid:$version"
  exit 1
fi 

if [ "$count" -eq "0" ]; then
  echo "error: could not obtain jar for $groupid:$artifactid:$version"
  exit 1
fi 

if [ "$count" -gt "1" ]; then
  echo "error: multiple jars downloaded for $groupid:$artifactid:$version but can only transform one jar at a time."
  exit 2
fi 

cd /tmp/ee82ee9
# process the one jar
for i in *.jar
do     
     cd $transformer
     echo "transforming /tmp/ee82ee9/$i"
     
     ./gradlew --console=plain run --args="-j /tmp/ee82ee9/$i -o /tmp/ee82ee9/$outputjarname"
     mvn org.apache.maven.plugins:maven-install-plugin:3.0.0-M1:install-file -Dfile="/tmp/ee82ee9/$outputjarname" -DgroupId=$groupid -DartifactId=$artifactid -Dversion=$outputversion -Dpackaging=jar
done
