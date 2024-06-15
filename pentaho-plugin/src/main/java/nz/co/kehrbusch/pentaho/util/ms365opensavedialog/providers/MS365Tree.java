package nz.co.kehrbusch.pentaho.util.ms365opensavedialog.providers;

import org.pentaho.di.plugins.fileopensave.api.providers.Tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class MS365Tree implements Tree<BaseEntity> {
    private static final Logger log = Logger.getLogger(MS365Tree.class.getName());

    private final String name;
    private List<BaseEntity> children = new ArrayList<>();

    public MS365Tree(String name){
        log.info("init tree ");
        MS365Directory obj4 = new MS365Directory();
        obj4.setId("name1");
        obj4.setName("name1");
        MS365Directory obj0 = new MS365Directory();
        obj0.setId("name2");
        obj0.setName("name2");
        obj4.addChild(obj0);
        obj0.setParent(obj4);
        MS36File obj1 = new MS36File();
        obj1.setName("name3");
        obj1.setId("name3");
        obj0.addChild(obj1);
        obj1.setParent(obj0);
        MS36File obj2 = new MS36File();
        obj2.setName("name4");
        obj2.setId("name4");
        MS36File obj3 = new MS36File();
        obj3.setName("name5");
        obj3.setId("name5");
        this.children.addAll(Arrays.asList(obj4, obj2, obj3));
        this.name = name;
    }

    @Override
    public String getName() {
        log.info("get tree name");
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
        log.info("Get order");
        return 4;
    }

    @Override
    public String getProvider() {
        return MS365FileProvider.TYPE;
    }

    public void deleteChild(BaseEntity ms365Object){
        this.children.remove(ms365Object);
    }

    public boolean hasChild(BaseEntity ms36File){
        BaseEntity searchResult = children.stream()
                .filter(child -> child.getName().equals(ms36File.getName())).findAny().orElse(null);
        //null means no child
        return searchResult != null;
    }
}
