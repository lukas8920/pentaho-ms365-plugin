package nz.co.kehrbusch.ms365.interfaces;

public interface IGraphClientDetails extends Loggable {
    String getScope();
    String getTenantId();
    String getClientId();
    String getPassword();
}
