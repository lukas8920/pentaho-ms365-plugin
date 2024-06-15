package nz.co.kehrbusch.pentaho.util.config;

public enum ProviderFilterType {
    ALL_PROVIDERS, DEFAULT, CLUSTERS, LOCAL, REPOSITORY, VFS, SHAREPOINT, RECENTS;

    @Override
    public String toString() {
        return name().toLowerCase();
    }

    public static String[] getDefaults() {
        return new String[] { LOCAL.toString(), VFS.toString(), CLUSTERS.toString() };
    }
}
