package nz.co.kehrbusch.pentaho.connections.ui.dialog;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.*;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.ui.core.PropsUI;
import org.pentaho.di.ui.core.widget.PasswordTextVar;

public class MS365DetailsCompositeHelper {
    private PropsUI props;
    private int margin;
    private int maxLabelWidth = 0;
    private final Class<?> pkg;

    public MS365DetailsCompositeHelper( Class<?> messageClass, PropsUI props ) {
        this.props = props;
        margin = Const.MARGIN;
        pkg = messageClass;
    }

    public PropsUI getProps() {
        return props;
    }

    public int getMaxLabelWidth() {
        return maxLabelWidth;
    }

    public int getMargin() {
        return margin;
    }

    public Label createLabel(Composite composite, int flags, String key, Control topWidget ) {
        return createLabel(composite, flags, key, topWidget, 0);
    }

    public Label createLabel(Composite composite, int flags, String key, Control topWidget, int leftPad ) {
        Label label = new Label( composite, flags );
        getProps().setLook( label );
        label.setText( BaseMessages.getString( pkg, key ) );
        maxLabelWidth = Math.max( maxLabelWidth, label.computeSize( SWT.DEFAULT, SWT.DEFAULT ).x );
        label.setLayoutData( getFormDataLabel( topWidget, leftPad) );
        return label;
    }

    public Label createTitle(Display display, Composite composite, int flags, String key, Control topWidget){
        Label label = new Label(composite, flags);
        getProps().setLook(label);
        label.setText(BaseMessages.getString(pkg, key));

        FontData[] fD = label.getFont().getFontData();
        fD[0].setHeight(16);
        label.setFont( new Font(display, fD[0]));

        FormData formData = new FormData();
        formData.left = new FormAttachment(0);
        formData.right = new FormAttachment(100);
        formData.top = new FormAttachment(0, margin * 2);
        label.setLayoutData(formData);
        return label;
    }

    public Text createText( Composite composite, int flags, Control topWidget, int width ) {
        return createText(composite, flags, topWidget, width, 0);
    }

    public Text createText( Composite composite, int flags, Control topWidget, int width, int leftPad ) {
        Text text = new Text( composite, flags );
        getProps().setLook( text );
        text.setLayoutData( getFormDataField( topWidget, width, leftPad ) );
        return text;
    }

    public FormData getFormDataLabel(Control topWidget, int leftPad ) {
        FormData formData = new FormData();
        if ( topWidget == null ) {
            formData.top = new FormAttachment( 0, margin * 2 ); // First Item
        } else {
            formData.top = new FormAttachment( topWidget, margin * 2 ); // Following Items
        }
        formData.left = new FormAttachment( 0, leftPad); // First one in the left top corner
        return formData;
    }

    public FormData getFormDataField( Control topWidget, int width, int lefPad ) {
        FormData formData = new FormData();
        if ( topWidget == null ) {
            formData.top = new FormAttachment( 0, margin * 2 );  //First Item
        } else {
            formData.top = new FormAttachment( topWidget, margin * 2 ); //Following Items
        }
        if ( width == 0 ) {
            formData.right = new FormAttachment( 100, -margin - 9 ); //Fill but make sure scrollbar won't overlap hence subtracting 9 pixels
        }
        formData.left = new FormAttachment(0, lefPad );
        return formData;
    }

    public CCombo createCCombo( Composite composite, int flags, Control topWidget, int width ) {
        CCombo cCombo = new CCombo( composite, flags );
        getProps().setLook( cCombo );
        cCombo.setLayoutData( getFormDataField( topWidget, width, 0 ) );
        return cCombo;
    }

    public PasswordTextVar createPasswordTextVar(VariableSpace variableSpace, Composite composite, int flags,
                                                 Control topWidget, int width ) {
        return createPasswordTextVar(variableSpace, composite, flags, topWidget, width, 0);
    }

    public PasswordTextVar createPasswordTextVar(VariableSpace variableSpace, Composite composite, int flags,
                                                 Control topWidget, int width, int leftPad) {
        PasswordTextVar passwordTextVar = new PasswordTextVar( variableSpace, composite, flags );
        getProps().setLook( passwordTextVar );
        passwordTextVar.setLayoutData( getFormDataField( topWidget, width, leftPad) );
        return passwordTextVar;
    }
}
