package myToolWindow;

import com.intellij.openapi.ui.ComboBox;
import temp.Java2sAutoComboBox;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by myoo on 16. 1. 11.
 */
public class PropNameComboBox extends Java2sAutoComboBox {


    public PropNameComboBox(List list) {
        super(list);
    //    setStrict(false);
        addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                // Cell Data changed
                if(itemEvent.getStateChange() == ItemEvent.SELECTED) {
                    PropNameComboBox propNameComboBox = (PropNameComboBox) itemEvent.getSource();
              //      propNameComboBox.table


                }
            }
        });

    }

    @Override
    public void addItem(Object item) {
        super.addItem(item);
        getDataList().add(item.toString());
    }

}
