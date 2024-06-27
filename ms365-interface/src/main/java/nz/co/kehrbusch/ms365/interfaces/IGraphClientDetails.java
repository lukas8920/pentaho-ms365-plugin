package nz.co.kehrbusch.ms365.interfaces;

public interface IGraphClientDetails extends ILoggable {
    String getScope();
    String getTenantId();
    String getClientId();
    String getPassword();
    String getProxyHost();
    String getProxyPort();
    String getProxyUser();
    String getProxyPassword();
}
