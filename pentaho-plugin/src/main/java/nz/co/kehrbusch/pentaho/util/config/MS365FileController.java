package nz.co.kehrbusch.pentaho.util.config;

import org.pentaho.di.plugins.fileopensave.api.providers.Tree;
import org.pentaho.di.plugins.fileopensave.api.providers.exception.InvalidFileProviderException;
import org.pentaho.di.plugins.fileopensave.cache.FileCache;
import org.pentaho.di.plugins.fileopensave.controllers.FileController;
import org.pentaho.di.plugins.fileopensave.providers.ProviderService;

import java.util.ArrayList;
import java.util.List;

public class MS365FileController extends FileController {
    private final ProviderService providerService;

    public MS365FileController(FileCache fileCache, ProviderService providerService) {
        super(fileCache, providerService);
        this.providerService = providerService;
    }

    @Override
    public List<Tree> load(String filter){
        return this.load(filter, new ArrayList<>());
    }

    @Override
    public List<Tree> load(String filter, List<String> connectionTypes){
        List<Tree> trees = new ArrayList<>();
        if (filter.equalsIgnoreCase(nz.co.kehrbusch.pentaho.util.config.ProviderFilterType.SHAREPOINT.toString())) {
            try {
                trees.add(providerService.get(nz.co.kehrbusch.pentaho.util.config.ProviderFilterType.SHAREPOINT.toString()).getTree());
            } catch (InvalidFileProviderException e) {
                e.printStackTrace();
            }
        }
        return trees;
    }
}
