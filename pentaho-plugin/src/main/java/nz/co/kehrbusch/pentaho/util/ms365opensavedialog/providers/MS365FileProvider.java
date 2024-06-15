package nz.co.kehrbusch.pentaho.util.ms365opensavedialog.providers;

import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.plugins.fileopensave.api.providers.BaseFileProvider;
import org.pentaho.di.plugins.fileopensave.api.providers.File;
import org.pentaho.di.plugins.fileopensave.api.providers.Tree;
import org.pentaho.di.plugins.fileopensave.api.providers.exception.FileException;
import org.pentaho.di.plugins.fileopensave.api.providers.exception.FileExistsException;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class MS365FileProvider extends BaseFileProvider<BaseEntity> {
    private static final Logger log = Logger.getLogger(MS365FileProvider.class.getName());

    public static final String NAME = "MS365 Sharepoint";
    public static final String TYPE = "SHAREPOINT";

    private MS365Tree ms365Tree;

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
        log.info("Get MS365FileProvider Type");
        return TYPE;
    }

    @Override
    public boolean isAvailable() {
        //todo here i run a check whether there are details available in connections manager
        log.info("Check whether provider is available");
        return true;
    }

    @Override
    public Tree getTree() {
        return this.getTree(new ArrayList<>());
    }

    @Override
    public MS365Tree getTree(List<String> types){
        log.info("Create and get tree");
        //todo here I get the initial children - disks
        //todo find trigger to initiate getTree callback
        this.ms365Tree = new MS365Tree(NAME);
        return this.ms365Tree;
    }

    @Override
    public List<BaseEntity> getFiles(BaseEntity file, String filters, VariableSpace space) throws FileException {
        log.info("Get files");
        //todo run another get job in the background - show loading and getFiles Callback
        return file.getChildren();
    }

    @Override
    public List<BaseEntity> delete(List<BaseEntity> files, VariableSpace space) throws FileException {
        files.forEach(file -> {
            //todo foreach run server request to delete file
            MS365Directory parent = file.getParentObject();
            if (parent == null){
                this.ms365Tree.deleteChild(file);
            } else {
                parent.deleteChild(file);
            }
        });
        return files;
    }

    @Override
    public BaseEntity add(BaseEntity folder, VariableSpace space) throws FileException {
        MS365Directory parent = folder.getParentObject();
        boolean hasChild;
        if (parent == null){
            hasChild = this.ms365Tree.hasChild(folder);
        } else {
            hasChild = parent.hasChild(folder);
        }
        if (hasChild) throw new FileExistsException();
        //todo add to sharepoint
        if (parent == null){
            this.ms365Tree.addChild(folder);
        } else {
            parent.addChild(folder);
        }
        return folder;
    }

    @Override
    public BaseEntity getFile(BaseEntity file, VariableSpace space) {
        return null;
    }

    @Override
    public boolean fileExists(BaseEntity dir, String path, VariableSpace space) throws FileException {
        log.info("File exists: " + dir + ", " + path);
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
