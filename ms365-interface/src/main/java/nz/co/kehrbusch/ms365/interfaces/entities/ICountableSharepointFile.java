package nz.co.kehrbusch.ms365.interfaces.entities;

public interface ICountableSharepointFile extends ISharepointFile {
    Counter getFileCounter();
    void setFileCounter(Counter fileCounter);
    Counter getPartCounter();
    void setPartCounter(Counter partCounter);
}
