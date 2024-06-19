package nz.co.kehrbusch.pentaho.connections.util;

import com.microsoft.kiota.TriConsumer;
import nz.co.kehrbusch.pentaho.connections.manage.ConnectionDetailsInterface;
import nz.co.kehrbusch.pentaho.connections.manage.ConnectionTypeInterface;
import nz.co.kehrbusch.pentaho.connections.manage.MS365ConnectionManager;
import org.pentaho.di.core.Const;
import org.pentaho.di.repository.RepositoryDirectoryInterface;
import org.pentaho.di.repository.RepositoryMeta;
import org.pentaho.di.repository.filerep.KettleFileRepositoryMeta;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class FileRepository {
    private static final Object lock = new Object();
    private static final Logger log = Logger.getLogger(FileRepository.class.getName());

    public static void deleteFileXMLConnection(String filename, ConnectionDetailsInterface connectionDetailsInterface){
        TriConsumer<Document, Node, ConnectionDetailsInterface> triConsumer = (mDoc, mConnectionsNode, mConnectionDetailsInterface) -> {};
        new Thread(() -> {
            synchronized (lock){
                handleFileUpdate(filename, connectionDetailsInterface, triConsumer);
            }
        }).start();
    }

    //deletes connection first and then re-adds with new values
    public static void writeFileXMLConnection(String filename, ConnectionDetailsInterface connectionDetailsInterface){
        TriConsumer<Document, Node, ConnectionDetailsInterface> triConsumer = (mDoc, mConnectionsNode, mConnectionDetailsInterface) -> {
            // Get the root <connections> element
            mConnectionsNode = mDoc.getElementsByTagName("connections").item(0);

            // Create a new <connection> element
            Element newConnection = mDoc.createElement("connection");
            mConnectionDetailsInterface.writeFileXMLConnection(mDoc, newConnection);

            // Append the new <connection> element to the <connections> node
            mConnectionsNode.appendChild(newConnection);
        };
        new Thread(() -> {
            synchronized (lock){
                handleFileUpdate(filename, connectionDetailsInterface, triConsumer);
            }
        }).start();
    }

    public static void readFileXMLConnections(MS365ConnectionManager ms365ConnectionManager, String filename, ResponseHandler handler){
        new Thread(() -> {
            synchronized (lock){
                List<ConnectionDetailsInterface> connectionDetailsInterfaces = readAllConnectionDetails(ms365ConnectionManager, filename);
                handler.onResponse(connectionDetailsInterfaces);
            }
        }).start();
    }

    public static List<ConnectionDetailsInterface> readAllConnectionDetails(MS365ConnectionManager ms365ConnectionManager, String filename){
        List<ConnectionDetailsInterface> connectionDetailsInterfaces = new ArrayList<>();
        try {
            // Create a DocumentBuilderFactory
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc;

            // Parse the existing XML file
            File xmlFile = new File(filename);
            if (xmlFile.exists()) {
                doc = dBuilder.parse(xmlFile);
                doc.getDocumentElement().normalize();

                // Get all <connection> elements
                NodeList connectionList = doc.getElementsByTagName("connection");
                for (int i = 0; i < connectionList.getLength(); i++) {
                    Node connectionNode = connectionList.item(i);
                    if (connectionNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element connectionElement = (Element) connectionNode;

                        String type = connectionElement.getElementsByTagName("type").item(0).getTextContent();
                        List<ConnectionTypeInterface> types = ms365ConnectionManager.provideTypes();
                        ConnectionTypeInterface typeInterface = types.stream()
                                .filter(t -> t.provideType().equals(type))
                                .findFirst()
                                .orElseThrow(() -> new IOException("Invalid type provided in ms365-connections.xml"));
                        ConnectionDetailsInterface connectionDetailsInterface = typeInterface.createInstanceOfDetails();
                        connectionDetailsInterface.readFileXMLProperties(connectionElement);

                        connectionDetailsInterfaces.add(connectionDetailsInterface);
                    }
                }
            }
        } catch (IOException | SAXException | ParserConfigurationException e){
            log.info(e.getMessage());
        }
        return connectionDetailsInterfaces;
    }

    public static void handleFileUpdate(String filename, ConnectionDetailsInterface connectionDetailsInterface, TriConsumer<Document, Node, ConnectionDetailsInterface> insertHandler){
        try {
            // Create a DocumentBuilderFactory
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc;

            // Check if the file exists
            File xmlFile = new File(filename);
            if (xmlFile.exists()) {
                // Parse the existing XML file
                doc = dBuilder.parse(xmlFile);
                doc.getDocumentElement().normalize();
            } else {
                // Create a new Document
                doc = dBuilder.newDocument();
                // Create root element <connections>
                Element rootElement = doc.createElement("connections");
                doc.appendChild(rootElement);
            }

            doc.getDocumentElement().normalize();

            // Get the root <connections> element
            Node connectionsNode = doc.getElementsByTagName("connections").item(0);
            if (connectionsNode != null && connectionsNode.getNodeType() == Node.ELEMENT_NODE) {
                Element connectionsElement = (Element) connectionsNode;

                // Get all <connection> elements
                NodeList connectionList = connectionsElement.getElementsByTagName("connection");
                for (int i = 0; i < connectionList.getLength(); i++) {
                    Node connectionNode = connectionList.item(i);
                    if (connectionNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element connectionElement = (Element) connectionNode;
                        // Check if the name attribute matches the specified name
                        if (connectionElement.getAttribute("name").equals(connectionDetailsInterface.getConnectionName())) {
                            // Remove the matching <connection> element
                            connectionsElement.removeChild(connectionElement);
                            break;
                        }
                    }
                }
            }

            insertHandler.accept(doc, connectionsNode, connectionDetailsInterface);

            // Remove empty text nodes
            removeEmptyTextNodes(doc.getDocumentElement());

            // Write the content back to the XML file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(filename));
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
            transformer.transform(source, result);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private static void removeEmptyTextNodes(Node node) {
        NodeList children = node.getChildNodes();
        for (int i = children.getLength() - 1; i >= 0; i--) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.TEXT_NODE && child.getNodeValue().trim().isEmpty()) {
                node.removeChild(child);
            } else if (child.hasChildNodes()) {
                removeEmptyTextNodes(child);
            }
        }
    }

    public static String calcDirectoryName(RepositoryDirectoryInterface dir, RepositoryMeta repositoryMeta) {
        StringBuilder directory = new StringBuilder();
        String baseDir = ((KettleFileRepositoryMeta) repositoryMeta).getBaseDirectory();
        baseDir = Const.replace(baseDir, "\\", "/");
        directory.append(baseDir);
        if (!baseDir.endsWith("/")) {
            directory.append("/");
        }

        if (dir != null) {
            String path = calcRelativeElementDirectory(dir);
            if (path.startsWith("/")) {
                directory.append(path.substring(1));
            } else {
                directory.append(path);
            }

            if (!path.endsWith("/")) {
                directory.append("/");
            }
        }

        return directory.toString();
    }

    private static String calcRelativeElementDirectory(RepositoryDirectoryInterface dir) {
        return dir != null ? dir.getPath() : "/";
    }

    public interface ResponseHandler {
        void onResponse(List<ConnectionDetailsInterface> detailsInterfaces);
    }
}
