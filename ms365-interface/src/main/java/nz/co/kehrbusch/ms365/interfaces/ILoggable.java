package nz.co.kehrbusch.ms365.interfaces;

public interface ILoggable {
    void logBasic(String message);
    void logError(String message);
    void logDebug(String message);
}
