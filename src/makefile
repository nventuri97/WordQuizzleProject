JC = javac
LIBS = -cp .:Exception/gson-2.8.2.jar --module-path .:Exception/javafx/lib --add-modules javafx.controls,javafx.fxml
JE = java

default:
		$(JC) $(LIBS) ./Exception/*.java ./Client/*.java ./Server/*.java ./Utils/*.java

server:
		$(JE) $(LIBS) Server.Server

client:
		$(JE) $(LIBS) Client.ClientGui

clean:
		-rm -f Client/*.class Exception/*.class Server/*.class Utils/*.class
