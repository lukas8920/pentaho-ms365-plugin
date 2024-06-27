package nz.co.kehrbusch.pentaho.util.listeners;

import nz.co.kehrbusch.ms365.interfaces.ISharepointConnection;
import nz.co.kehrbusch.pentaho.connections.manage.GraphConnectionDetails;
import nz.co.kehrbusch.pentaho.connections.manage.MS365ConnectionManager;
import nz.co.kehrbusch.pentaho.trans.getfilenames.MS365GetFileNamesMeta;
import nz.co.kehrbusch.pentaho.util.file.SharepointFileWrapper;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.pentaho.di.core.logging.LogChannel;
import org.pentaho.di.trans.step.BaseStepMeta;

import java.util.function.BiConsumer;

public class Handler {
    public static <T extends BaseStepMeta> void handleAfterFilesAvailable(BiConsumer<ISharepointConnection, SharepointFileWrapper<T>> runnable,
                                                 HandlerInput<T> handlerInput){
        String connection = handlerInput.getwConnectionField().getText();
        T info = (T) handlerInput.getMeta();
        handlerInput.populateGenericMeta(info);

        new Thread(() -> {
            MS365ConnectionManager connectionManager = MS365ConnectionManager.getInstance(new LogChannel());
            GraphConnectionDetails graphConnectionDetails = (GraphConnectionDetails) connectionManager.provideDetailsByConnectionName(connection);
            ISharepointConnection iSharepointConnection = graphConnectionDetails.getISharepointConnection();

            SharepointFileWrapper<T> wrapper = new SharepointFileWrapper<>(info, iSharepointConnection, new LogChannel());
            if (info instanceof MS365GetFileNamesMeta){
                wrapper.addIStreamProvider(((MS365GetFileNamesMeta) info).getFileName(), ((MS365GetFileNamesMeta) info).getFileMask(), ((MS365GetFileNamesMeta) info).getExcludeFileMask());
            }

            if (!handlerInput.getShell().isDisposed()){
                handlerInput.getShell().getDisplay().asyncExec(() -> {
                    runnable.accept(iSharepointConnection, wrapper);
                    handlerInput.getLblLoadingInfo().setVisible(false);
                });
            }
        }).start();
    }

    public interface HandlerInput<T extends BaseStepMeta>{
        CCombo getwConnectionField();
        void populateGenericMeta(T t);
        Shell getShell();
        Label getLblLoadingInfo();
        T getMeta();
    }
}
