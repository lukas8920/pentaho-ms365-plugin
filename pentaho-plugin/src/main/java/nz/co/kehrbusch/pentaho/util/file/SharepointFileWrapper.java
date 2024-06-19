package nz.co.kehrbusch.pentaho.util.file;

import nz.co.kehrbusch.ms365.interfaces.ISharepointConnection;
import nz.co.kehrbusch.ms365.interfaces.entities.ISharepointFile;
import nz.co.kehrbusch.ms365.interfaces.entities.IStreamProvider;
import nz.co.kehrbusch.pentaho.trans.csvinput.MS365CsvInputMeta;
import nz.co.kehrbusch.pentaho.util.ms365opensavedialog.providers.BaseEntity;
import nz.co.kehrbusch.pentaho.util.ms365opensavedialog.providers.MS365Directory;
import nz.co.kehrbusch.pentaho.util.ms365opensavedialog.providers.MS365Site;
import nz.co.kehrbusch.pentaho.util.ms365opensavedialog.providers.MS365File;
import org.pentaho.di.core.logging.LogChannel;
import org.pentaho.di.core.logging.LogChannelInterface;

import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.List;

public class SharepointFileWrapper {
    private final List<IStreamProvider> streamProviders;
    private final ISharepointConnection iSharepointConnection;
    private final LogChannelInterface channel;

    public SharepointFileWrapper(MS365CsvInputMeta ms365CsvInputMeta, ISharepointConnection iSharepointConnection, LogChannelInterface channel){
        this.streamProviders = new ArrayList<>();
        this.iSharepointConnection = iSharepointConnection;
        this.channel = channel;

        if (iSharepointConnection != null){
            IStreamProvider iStreamProvider = map(iSharepointConnection.inflateTreeByPath(ms365CsvInputMeta.getFilename()));
            this.streamProviders.add(iStreamProvider);
            channel.logDebug("Retrieved base file from sharepoint");
            channel.logDebug("Base file name: " + iStreamProvider.getName());
            channel.logDebug("Base file path: " + iStreamProvider.getPath());
            channel.logDebug("Base file id: " + iStreamProvider.getId());
        }
    }

    public List<IStreamProvider> getStreamProviders(){
        return this.streamProviders;
    }

    public IStreamProvider getIStreamProviderByPath(String filename) throws InvalidPathException, NullPointerException {
        if (this.iSharepointConnection == null) throw new NullPointerException("No MS365 connection provided.");
        IStreamProvider iStreamProvider = this.streamProviders.stream()
                .filter(streamProvider -> {
                    channel.logDebug("Compare files: ");
                    channel.logDebug("Path of base file from sharepoint: " +streamProvider.getPath() + streamProvider.getName());
                    channel.logDebug("Path of requested base file: " + filename);
                    return (streamProvider.getPath() + streamProvider.getName()).equals(filename);
                })
                .findFirst().orElse(null);
        if (iStreamProvider == null){
            IStreamProvider streamProvider = map(this.iSharepointConnection.inflateTreeByPath(filename));
            this.streamProviders.add(streamProvider);
            return streamProvider;
        }
        return iStreamProvider;
    }

    //map all parent objects
    private IStreamProvider map(ISharepointFile iSharepointFile){
        MS365File ms365File = new MS365File();
        ms365File.setName(iSharepointFile.getName());
        ms365File.setId(iSharepointFile.getId());
        ms365File.setSize(iSharepointFile.getSize());
        BaseEntity previousFile = ms365File;
        while (iSharepointFile.getParentObject() != null){
            iSharepointFile = iSharepointFile.getParentObject();
            if (iSharepointFile.getParentObject() == null || iSharepointFile.getParentObject().getParentObject() == null){
                MS365Site ms365Site = new MS365Site();
                ms365Site.setName(iSharepointFile.getName());
                ms365Site.setId(iSharepointFile.getId());
                ms365Site.addChild(previousFile);
                previousFile.setParent(ms365Site);
                previousFile = ms365Site;
            } else {
                MS365Directory ms365Directory = new MS365Directory();
                ms365Directory.setName(iSharepointFile.getName());
                ms365Directory.setId(iSharepointFile.getId());
                ms365Directory.addChild(previousFile);
                previousFile.setParent(ms365Directory);
                previousFile = ms365Directory;
            }
        }

        return ms365File;
    }
}
