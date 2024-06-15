package nz.co.kehrbusch.ms365;

import nz.co.kehrbusch.ms365.interfaces.IGraphClientDetails;
import nz.co.kehrbusch.ms365.interfaces.IGraphConnection;
import nz.co.kehrbusch.ms365.interfaces.ISharepointConnection;

public class GraphClientModule {
    public static IGraphConnection provideGraphConnection(IGraphClientDetails iGraphClientDetails){
        return new GraphConnection(iGraphClientDetails);
    }

    public static ISharepointConnection provideSharepointConnection(IGraphClientDetails iGraphClientDetails){
        return new SharepointConnection(iGraphClientDetails);
    }
}
