FILES="../desc/Limosine/types/*.xml"

MAIN=org.apache.uima.tools.jcasgen.Jg
for f in $FILES
do
    "$UIMA_HOME/bin/runUimaClass.sh" $MAIN -jcasgeninput "$f" -jcasgenoutput "../src/main/java"
done
