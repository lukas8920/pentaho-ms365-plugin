package nz.co.kehrbusch.pentaho.util.file;

import nz.co.kehrbusch.ms365.interfaces.ISharepointConnection;
import nz.co.kehrbusch.ms365.interfaces.entities.ISharepointFile;
import nz.co.kehrbusch.ms365.interfaces.entities.IStreamProvider;
import nz.co.kehrbusch.pentaho.trans.csvinput.MS365CsvInputMeta;
import nz.co.kehrbusch.pentaho.util.ms365opensavedialog.providers.BaseEntity;
import nz.co.kehrbusch.pentaho.util.ms365opensavedialog.providers.MS365Directory;
import nz.co.kehrbusch.pentaho.util.ms365opensavedialog.providers.MS365Site;
import nz.co.kehrbusch.pentaho.util.ms365opensavedialog.providers.MS365File;

import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class SharepointFileWrapper {
    private static final Logger log = Logger.getLogger(SharepointFileWrapper.class.getName());
    private final List<IStreamProvider> streamProviders;
    private final ISharepointConnection iSharepointConnection;

    public SharepointFileWrapper(MS365CsvInputMeta ms365CsvInputMeta, ISharepointConnection iSharepointConnection){
        this.streamProviders = new ArrayList<>();
        this.iSharepointConnection = iSharepointConnection;

        if (iSharepointConnection != null){
            IStreamProvider iStreamProvider = map(iSharepointConnection.inflateTreeByPath(ms365CsvInputMeta.getFilename()));
            this.streamProviders.add(iStreamProvider);
            log.info("Retrieved File");
            log.info(iStreamProvider.getName());
            log.info(iStreamProvider.getId());
            log.info(iStreamProvider.getPath());
        }
    }

    public List<IStreamProvider> getStreamProviders(){
        return this.streamProviders;
    }

    public IStreamProvider getIStreamProviderByPath(String filename) throws InvalidPathException, NullPointerException {
        if (this.iSharepointConnection == null) throw new NullPointerException("No MS365 connection provided.");
        IStreamProvider iStreamProvider = this.streamProviders.stream()
                .filter(streamProvider -> {
                    log.info("Compare: ");
                    log.info(streamProvider.getPath() + streamProvider.getName());
                    log.info(filename);
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
