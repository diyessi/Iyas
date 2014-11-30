tar -xzf APOSTLib.tgz

tar -xzf CRF++-0.58.tar.gz
cd CRF++-0.58/
./configure
make

# make sure that in CRF++-0.58/java/Makefile the jni.h file is correctly specified
cd java
make

cd ../../

cp CRF++-0.58/java/CRFPP.jar ../lib/CRFPP.jar
cp CRF++-0.58/java/libCRFPP.so ../resources/ArabicPOSTLib/data/libCRFPP.so
