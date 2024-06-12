package nz.co.kehrbusch.pentaho.connections.manage;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public interface ConnectionDetailsInterface {
    ConnectionTypeInterface provideType();
    String getConnectionName();
    String getDescription();
    ConnectionDetailsInterface copyToNewDetailsInterface(ConnectionTypeInterface typeInterface);
    UITypeCallback provideCallback();
    String computeValidateMessage();
    void setConnectionName(String connectionName);
    void setDescription(String description);
    void setHasDuplicateConnectionName(boolean flag);
    boolean hasDuplicateConnectionName();
    //Needs to serialize a type element
    void writeFileXMLConnection(Document doc, Element element);
    void readFileXMLProperties(Element element);
    ConnectionDetailsInterface clone();
    boolean test();
}
