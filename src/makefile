JC = javac
LIBS = -cp .:Exception/gson-2.8.2.jar --module-path .:Exception/javafx-linux/lib --add-modules javafx.controls,javafx.fxml
JE = java
WLIBS = --add-opens java.base/java.util=ALL-UNNAMED
MACL = -cp .:Exception/gson-2.8.2.jar --module-path .:Exception/javafx-macOS/lib --add-modules javafx.controls,javafx.fxml

.PHONY: server client compMac serverMac clientMac clean

default:
		$(JC) $(LIBS) ./Exception/*.java ./Client/*.java ./Server/*.java ./Utils/*.java

server:
		$(JE) $(LIBS) $(WLIBS) Server.Server

client:
		$(JE) $(LIBS) $(WLIBS) Client.ClientGui

compMac:
		$(JC) $(MACL) ./Exception/*.java ./Client/*.java ./Server/*.java ./Utils/*.java

serverMac: 
		$(JE) $(MACL) $(WLIBS) Server.Server

clientMac:
		$(JE) $(MACL) $(WLIBS) Client.ClientGui

clean:
		-rm -f Client/*.class Exception/*.class Server/*.class Utils/*.class
