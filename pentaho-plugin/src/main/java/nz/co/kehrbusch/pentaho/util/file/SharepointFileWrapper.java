package nz.co.kehrbusch.pentaho.util.file;

import nz.co.kehrbusch.ms365.interfaces.ISharepointConnection;
import nz.co.kehrbusch.ms365.interfaces.entities.ISharepointFile;
import nz.co.kehrbusch.ms365.interfaces.entities.IStreamProvider;
import nz.co.kehrbusch.pentaho.util.ms365opensavedialog.providers.BaseEntity;
import nz.co.kehrbusch.pentaho.util.ms365opensavedialog.providers.MS365Directory;
import nz.co.kehrbusch.pentaho.util.ms365opensavedialog.providers.MS365Site;
import nz.co.kehrbusch.pentaho.util.ms365opensavedialog.providers.MS365File;
import org.pentaho.di.core.logging.LogChannelInterface;
import org.pentaho.di.core.util.Utils;
import org.pentaho.di.trans.steps.csvinput.CsvInputMeta;
import org.pentaho.di.trans.steps.fileinput.text.TextFileInputMeta;

import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SharepointFileWrapper<T> {
    private final List<IStreamProvider> streamProviders;
    private final ISharepointConnection iSharepointConnection;
    private final LogChannelInterface channel;

    public SharepointFileWrapper(T t, ISharepointConnection iSharepointConnection, LogChannelInterface channel){
        this.streamProviders = new ArrayList<>();
        this.iSharepointConnection = iSharepointConnection;
        this.channel = channel;
        this.getProviderList(t);
    }

    private void getProviderList(T t){
        if (iSharepointConnection != null){
            if (t instanceof TextFileInputMeta){
                channel.logBasic("Try to identify files with patterns provided.");

                String[] filenames = ((TextFileInputMeta) t).inputFiles.fileName;
                String[] realmask = ((TextFileInputMeta) t).inputFiles.fileMask;
                String[] realExcludeMask = ((TextFileInputMeta) t).inputFiles.excludeFileMask;

                for (int i = 0; i < filenames.length; i++){
                    if (filenames[i] != null){
                        ISharepointFile iSharepointFile = iSharepointConnection.inflateTreeByPath(filenames[i]);
                        List<IStreamProvider> iStreamProviders = processBaseSharepointFile(iSharepointFile, realmask[i], realExcludeMask[i]);
                        this.streamProviders.addAll(iStreamProviders);
                    }
                }
            } else if (t instanceof CsvInputMeta){
                ISharepointFile iSharepointFile = iSharepointConnection.inflateTreeByPath(((CsvInputMeta) t).getFilename());
                List<IStreamProvider> streamProviders = processBaseSharepointFile(iSharepointFile);
                this.streamProviders.addAll(streamProviders);
            }
        }
    }

    public void addIStreamProvider(String filename){
        ISharepointFile iSharepointFile = iSharepointConnection.inflateTreeByPath(filename);
        List<IStreamProvider> streamProviders = processBaseSharepointFile(iSharepointFile);
        this.streamProviders.addAll(streamProviders);
    }

    private List<IStreamProvider> processBaseSharepointFile(ISharepointFile iSharepointFile){
        return this.processBaseSharepointFile(iSharepointFile, null, null);
    }

    private List<IStreamProvider> processBaseSharepointFile(ISharepointFile iSharepointFile, String onemask, String excludemask){
        if (iSharepointFile.getChildrenCount() == 0){
            IStreamProvider iStreamProvider = mapFile(iSharepointFile);
            channel.logDebug("Retrieved base file from sharepoint");
            channel.logDebug("Base file name: " + iStreamProvider.getName());
            channel.logDebug("Base file path: " + iStreamProvider.getPath());
            channel.logDebug("Base file id: " + iStreamProvider.getId());
            return Collections.singletonList(iStreamProvider);
        } else if (iSharepointFile.getChildrenCount() > 0){
            channel.logDebug("Retrieved directory from sharepoint");
            ISharepointFile parentDirectory = mapDirectory(iSharepointFile);
            List<ISharepointFile> iSharepointFiles = this.iSharepointConnection.getChildren(parentDirectory, 100);
            iSharepointFiles = iSharepointFiles.stream().filter(file -> file.getChildrenCount() == 0).collect(Collectors.toList());

            List<IStreamProvider> iStreamProviders = new ArrayList<>();
            if (!iSharepointFiles.isEmpty()){
                channel.logDebug("Check which files match the pattern.");
                iSharepointFiles = iSharepointFiles.stream().filter(file -> filter(file, onemask, excludemask)).collect(Collectors.toList());
                iSharepointFiles.forEach(file -> {
                    MS365File ms365File = new MS365File();
                    ms365File.setId(file.getId());
                    ms365File.setName(file.getName());
                    ms365File.setChildrenCount(file.getChildrenCount());
                    ms365File.setParent((BaseEntity) parentDirectory);
                    ((BaseEntity) parentDirectory).addChild(ms365File);
                    iStreamProviders.add(ms365File);
                });
            }
            return iStreamProviders;
        }
        return Collections.emptyList();
    }

    private boolean filter(ISharepointFile iSharepointFile, String onemask, String excludeonemask){
        boolean matches = true;
        if (!Utils.isEmpty(onemask)) {
            matches = Pattern.matches(onemask, iSharepointFile.getName());
        }

        boolean excludematches = false;
        if (!Utils.isEmpty(excludeonemask)) {
            excludematches = Pattern.matches(excludeonemask, iSharepointFile.getName());
        }
        channel.logDebug("File: " + iSharepointFile.getName());
        channel.logDebug("Matches pattern: " + matches + " , is in exclude filter: " + excludematches);

        return matches && !excludematches;
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
                    channel.logDebug("Parent file: " + streamProvider.getParentObject().getName());
                    return (streamProvider.getPath() + streamProvider.getName()).equals(filename);
                })
                .findFirst().orElse(null);
        if (iStreamProvider == null){
            IStreamProvider streamProvider = mapFile(this.iSharepointConnection.inflateTreeByPath(filename));
            this.streamProviders.add(streamProvider);
            return streamProvider;
        }
        return iStreamProvider;
    }

    //map all parent objects
    private IStreamProvider mapFile(ISharepointFile iSharepointFile){
        MS365File ms365File = new MS365File();
        ms365File.setName(iSharepointFile.getName());
        ms365File.setId(iSharepointFile.getId());
        ms365File.setSize(iSharepointFile.getSize());
        mapBase(iSharepointFile, ms365File);
        return ms365File;
    }

    //map all parent objects
    private ISharepointFile mapDirectory(ISharepointFile iSharepointFile){
        BaseEntity baseEntity;
        if (iSharepointFile.getParentObject() == null || iSharepointFile.getParentObject().getParentObject() == null){
            baseEntity = new MS365Site();
            baseEntity.setName(iSharepointFile.getName());
            baseEntity.setId(iSharepointFile.getId());
        } else {
            baseEntity = new MS365Directory();
            baseEntity.setName(iSharepointFile.getName());
            baseEntity.setId(iSharepointFile.getId());
        }
        mapBase(iSharepointFile, baseEntity);
        return baseEntity;
    }

    private void mapBase(ISharepointFile baseEntity, BaseEntity convertedFile){
        BaseEntity previousFile = convertedFile;
        while (baseEntity.getParentObject() != null){
            baseEntity = baseEntity.getParentObject();
            if (baseEntity.getParentObject() == null || baseEntity.getParentObject().getParentObject() == null){
                MS365Site ms365Site = new MS365Site();
                ms365Site.setName(baseEntity.getName());
                ms365Site.setId(baseEntity.getId());
                ms365Site.addChild(previousFile);
                previousFile.setParent(ms365Site);
                previousFile = ms365Site;
            } else {
                MS365Directory ms365Directory = new MS365Directory();
                ms365Directory.setName(baseEntity.getName());
                ms365Directory.setId(baseEntity.getId());
                ms365Directory.addChild(previousFile);
                previousFile.setParent(ms365Directory);
                previousFile = ms365Directory;
            }
        }
    }
}
