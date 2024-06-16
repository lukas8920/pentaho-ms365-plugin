package nz.co.kehrbusch.pentaho.util.ms365opensavedialog.providers;

import org.pentaho.di.plugins.fileopensave.api.providers.Tree;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class MS365Tree implements Tree<BaseEntity> {
    private static final Logger log = Logger.getLogger(MS365Tree.class.getName());

    private final String name;
    private List<BaseEntity> children = new ArrayList<>();

    public MS365Tree(String name){
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public List<BaseEntity> getChildren() {
        return this.children;
    }

    @Override
    public void addChild(BaseEntity child) {
        this.children.add(child);
    }

    @Override
    public boolean isCanAddChildren() {
        return false;
    }

    @Override
    public int getOrder() {
        return 4;
    }

    @Override
    public String getProvider() {
        return MS365FileProvider.TYPE;
    }
}
