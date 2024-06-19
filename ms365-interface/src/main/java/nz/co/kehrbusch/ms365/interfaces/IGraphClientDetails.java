package nz.co.kehrbusch.ms365.interfaces;

public interface IGraphClientDetails extends ILoggable {
    String getScope();
    String getTenantId();
    String getClientId();
    String getPassword();
}
