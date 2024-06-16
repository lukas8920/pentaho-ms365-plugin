package nz.co.kehrbusch.pentaho.util.ms365opensavedialog.providers;

import nz.co.kehrbusch.ms365.interfaces.ISharepointConnection;
import nz.co.kehrbusch.ms365.interfaces.entities.ISharepointFile;
import org.eclipse.swt.widgets.Display;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.plugins.fileopensave.api.providers.BaseFileProvider;
import org.pentaho.di.plugins.fileopensave.api.providers.File;
import org.pentaho.di.plugins.fileopensave.api.providers.Tree;
import org.pentaho.di.plugins.fileopensave.api.providers.exception.FileException;
import org.pentaho.di.ui.spoon.Spoon;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class MS365FileProvider extends BaseFileProvider<BaseEntity> {
    private static final Logger log = Logger.getLogger(MS365FileProvider.class.getName());

    public static final String NAME = "MS365 Sharepoint";
    public static final String TYPE = "SHAREPOINT";
    private static final Object LOCK = new Object();

    private final ISharepointConnection iSharepointConnection;
    private final Runnable reloadCallback;
    private final Consumer<Boolean> loadingCallback;

    private MS365Tree ms365Tree;

    public MS365FileProvider(ISharepointConnection iSharepointConnection, Runnable reloadCallback, Consumer<Boolean> loadingCallback){
        this.iSharepointConnection = iSharepointConnection;
        this.reloadCallback = reloadCallback;
        this.loadingCallback = loadingCallback;
        this.initTree();
    }

    @Override
    public Class<BaseEntity> getFileClass() {
        return BaseEntity.class;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public boolean isAvailable() {
        //todo check if details are present with ISharepointConnection
        return true;
    }

    public void initTree(){
        this.ms365Tree = new MS365Tree(NAME);
        new Thread(() -> {
            synchronized (LOCK){
                this.loadingCallback.accept(true);
                List<ISharepointFile> sharepointFiles = iSharepointConnection.getSites(MS365Site.MAX_SITES_TO_FETCH);
                List<MS365Site> baseEntities = sharepointFiles.stream()
                        .map(sharepointFile -> {
                            MS365Site ms365Site = new MS365Site();
                            ms365Site.setName(sharepointFile.getName());
                            ms365Site.setId(sharepointFile.getId());
                            ms365Site.setChildrenCount(sharepointFile.getChildrenCount());
                            if (sharepointFile.getParentObject() != null){
                                ms365Site.setTmpParentId(sharepointFile.getParentObject().getId());
                            }
                            return ms365Site;
                        }).collect(Collectors.toList());
                baseEntities.stream().filter(base -> base.getTmpParentId() != null)
                                .forEach(base -> {
                                    MS365Site parent = getParent(baseEntities, base.getTmpParentId());
                                    parent.addChild(base);
                                    base.setParent(parent);
                                });
                filterSites(baseEntities).forEach(site -> this.ms365Tree.addChild(site));

                //refresh UI on Main Thread
                Display display = Spoon.getInstance().getDisplay();
                if (!display.isDisposed()){
                    display.asyncExec(this.reloadCallback::run);
                    this.loadingCallback.accept(false);
                }
            }
        }).start();
    }

    private MS365Site getParent(List<MS365Site> allFiles, String parentId){
        return allFiles.stream().filter(file -> file.getId().equals(parentId)).findFirst().orElse(null);
    }

    private List<BaseEntity> filterSites(List<MS365Site> baseEntities){
        return baseEntities.stream().filter(baseEntity -> baseEntity.getParentObject() == null).collect(Collectors.toList());
    }

    @Override
    public Tree getTree() {
        return this.getTree(new ArrayList<>());
    }

    @Override
    public MS365Tree getTree(List<String> types){
        return this.ms365Tree;
    }

    @Override
    public List<BaseEntity> getFiles(BaseEntity file, String filters, VariableSpace space) throws FileException {
        if (file.getParentObject() != null){
            new Thread(() -> {
                synchronized (LOCK){
                    if (file.hasInitChildren()) return;
                    this.loadingCallback.accept(true);
                    file.getRemoteChildren(this.iSharepointConnection);
                    file.setHasInitChildren(true);

                    //refresh UI on Main Thread
                    Display display = Spoon.getInstance().getDisplay();
                    display.asyncExec(() -> this.reloadCallback.run());
                    this.loadingCallback.accept(false);
                }
            }).start();
        }
        return file.getChildren();
    }

    @Override
    public List<BaseEntity> delete(List<BaseEntity> files, VariableSpace space) throws FileException {
        return files;
    }

    @Override
    public BaseEntity add(BaseEntity folder, VariableSpace space) throws FileException {
        return folder;
    }

    @Override
    public BaseEntity getFile(BaseEntity file, VariableSpace space) {
        return null;
    }

    @Override
    public boolean fileExists(BaseEntity dir, String path, VariableSpace space) throws FileException {
        return false;
    }

    @Override
    public String getNewName(BaseEntity destDir, String newPath, VariableSpace space) throws FileException {
        return null;
    }

    @Override
    public boolean isSame(File file1, File file2) {
        return file1.getProvider().equals(file2.getProvider());
    }

    @Override
    public BaseEntity rename(BaseEntity file, String newPath, boolean overwrite, VariableSpace space) throws FileException {
        return file;
    }

    @Override
    public BaseEntity copy(BaseEntity file, String toPath, boolean overwrite, VariableSpace space) throws FileException {
        return file;
    }

    @Override
    public BaseEntity move(BaseEntity file, String toPath, boolean overwrite, VariableSpace space) throws FileException {
        return file;
    }

    @Override
    public InputStream readFile(BaseEntity file, VariableSpace space) throws FileException {
        return null;
    }

    @Override
    public BaseEntity writeFile(InputStream inputStream, BaseEntity destDir, String path, boolean overwrite, VariableSpace space) throws FileException {
        return null;
    }

    @Override
    public BaseEntity getParent(BaseEntity file) {
        return file.getParentObject();
    }

    @Override
    public void clearProviderCache() {

    }
}
