package nz.co.kehrbusch.pentaho.connections.manage;

import nz.co.kehrbusch.pentaho.connections.util.FileRepository;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.repository.*;
import org.pentaho.di.ui.spoon.Spoon;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static nz.co.kehrbusch.pentaho.connections.ui.dialog.ConnectionDelegate.refreshMenu;

public class MS365ConnectionManager {
    private static final Logger log = Logger.getLogger(MS365ConnectionManager.class.getName());

    private static MS365ConnectionManager connectionManager = null;

    private final List<ConnectionTypeInterface> connectionTypeInterfaces;
    private final List<ConnectionDetailsInterface> liveConnectionDetailsInterfaces;

    private List<ConnectionDetailsInterface> existingConnectionDetailsInterfaces;
    private String repoFilename = "";

    private MS365ConnectionManager(){
        this.connectionTypeInterfaces = new ArrayList<>();
        this.liveConnectionDetailsInterfaces = new ArrayList<>();
        this.existingConnectionDetailsInterfaces = new ArrayList<>();
        this.connectionTypeInterfaces.add(GraphConnectionType.getInstance());
    }

    public void initConnections(){
        Spoon spoon = Spoon.getInstance();
        Repository repository = spoon.getRepository();
        try {
            repoFilename = FileRepository.calcDirectoryName(repository.getUserHomeDirectory(), repository.getRepositoryMeta()) + "ms365-connections.xml";
            log.info("Repo filename is: " + repoFilename);
            FileRepository.readFileXMLConnections(this, repoFilename, new FileRepository.ResponseHandler() {
                @Override
                public void onResponse(List<ConnectionDetailsInterface> detailsInterfaces) {
                    MS365ConnectionManager.this.liveConnectionDetailsInterfaces.addAll(detailsInterfaces);
                    refreshMenu();
                }
            });
        } catch (KettleException e) {
            e.printStackTrace();
        }
    }

    public static MS365ConnectionManager getInstance(){
        if (connectionManager == null){
            connectionManager = new MS365ConnectionManager();
        }
        return connectionManager;
    }

    public List<ConnectionDetailsInterface> getConnections(){
        return this.liveConnectionDetailsInterfaces;
    }

    public List<ConnectionTypeInterface> provideTypes(){
        return this.connectionTypeInterfaces;
    }

    public boolean hasConnectionName(String connectionName){
        List<String> connectionNames = this.existingConnectionDetailsInterfaces.stream()
                .map(ConnectionDetailsInterface::getConnectionName)
                .collect(Collectors.toList());
        return connectionNames.contains(connectionName);
    }

    public void createCopyOfDetailsForComparison(){
        this.existingConnectionDetailsInterfaces = new ArrayList<>();
        this.liveConnectionDetailsInterfaces.forEach(detail -> {
            ConnectionDetailsInterface cloneOfDetail = detail.clone();
            MS365ConnectionManager.this.existingConnectionDetailsInterfaces.add(cloneOfDetail);
        });
    }

    public ConnectionDetailsInterface provideDetailsByConnectionName(String connectionName){
        return liveConnectionDetailsInterfaces.stream()
                .filter(details -> details.getConnectionName().equals(connectionName))
                .findFirst()
                .orElse(null);
    }

    public void saveConnectionDetailsInterface(ConnectionDetailsInterface connectionDetailsInterface){
        ConnectionDetailsInterface savedDetails = provideDetailsByConnectionName(connectionDetailsInterface.getConnectionName());
        if (savedDetails == null){
            this.liveConnectionDetailsInterfaces.add(connectionDetailsInterface);
        }
        FileRepository.writeFileXMLConnection(this.repoFilename, connectionDetailsInterface);
        if (this.existingConnectionDetailsInterfaces.size() == this.liveConnectionDetailsInterfaces.size()){
            ConnectionDetailsInterface renamedElement = identifyRenamedElement();
            if (renamedElement != null){
                log.info("Identified element: " + renamedElement.getConnectionName());
                FileRepository.deleteFileXMLConnection(this.repoFilename, renamedElement);
            }
        }
    }

    private ConnectionDetailsInterface identifyRenamedElement(){
        List<String> connectionNames = this.liveConnectionDetailsInterfaces.stream()
                .map(ConnectionDetailsInterface::getConnectionName).collect(Collectors.toList());
        return this.existingConnectionDetailsInterfaces.stream()
                .filter(detail -> !connectionNames.contains(detail.getConnectionName()))
                .findFirst().orElse(null);
    }

    public void delete(String label){
        ConnectionDetailsInterface savedDetails = provideDetailsByConnectionName(label);
        FileRepository.deleteFileXMLConnection(repoFilename, savedDetails);
        this.liveConnectionDetailsInterfaces.remove(savedDetails);
    }
}
