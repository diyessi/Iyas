source classpath.sh

java -Djava.util.logging.config.file="resources/logging.properties" \
-Xss128m \
-cp ${CLASSPATH} qa.qcri.qf.pipeline.trec.TrecPipelineRunner \
-argumentsFilePath arguments/trec-pipeline-arguments.txt
