#! /bin/sh

rm -Rf bin/jar
mkdir -p bin/jar

echo "Building hbdp-ohap-server.jar..."
rm -Rf bin/class
mkdir -p bin/class
(cd src/java &&
javac -d ../../bin/class com/henrikhedberg/ohap/HbdpOhapServer.java) &&
(cd bin/class &&
jar cfm ../jar/hbdp-ohap-server.jar ../../src/manifest/hbdp-ohap-server.manifest com)

echo "Building tcp-ohap-server.jar..."
rm -Rf bin/class
mkdir -p bin/class
(cd src/java &&
javac -d ../../bin/class com/henrikhedberg/ohap/TcpOhapServer.java) &&
(cd bin/class &&
jar cfm ../jar/tcp-ohap-server.jar ../../src/manifest/tcp-ohap-server.manifest com)

echo "Building hbdp-connection-example.jar..."
rm -Rf bin/class
mkdir -p bin/class
(cd src/java &&
javac -d ../../bin/class com/henrikhedberg/hbdp/client/HbdpConnectionExample.java) &&
(cd bin/class &&
jar cfm ../jar/hbdp-connection-example.jar ../../src/manifest/hbdp-connection-example.manifest com)

echo "Building echo-tcp-server.jar..."
rm -Rf bin/class
mkdir -p bin/class
(cd src/java &&
javac -d ../../bin/class com/henrikhedberg/util/EchoTcpServer.java) &&
(cd bin/class &&
jar cfm ../jar/echo-tcp-server.jar ../../src/manifest/echo-tcp-server.manifest com)


echo "Building documentation..."
rm -Rf doc
mkdir -p doc
(cd src/java &&
javadoc -quiet -version -author -d ../../doc -subpackages com)
