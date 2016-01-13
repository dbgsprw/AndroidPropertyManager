package core;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import temp.Java2sAutoComboBox;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.*;
import java.util.ArrayList;

public class PluginViewFactory implements ToolWindowFactory {

    /*
    View Components
     */
    private JPanel PluginViewContent;
    private JButton saveToDeviceButton;
    private JButton addBtn;
    private JButton resetButton;
    private JButton saveToIntellijButton;
    private JTable propTable;
    private ToolWindow myToolWindow;
    private JComboBox comboBox1;

    private boolean isInitDone;

    private Java2sAutoComboBox propNameComboBox;

    private core.PropertyManager propertyManager;

    private void  showAllProperty() {
        ArrayList<String> propertyNames = propertyManager.getPropertyNames();
        int length = propertyNames.size();
        for(int i = 0; i < length; i++) {
            String propertyName = propertyNames.get(i);
            propTable.setValueAt(propertyName, i, 0);
            propTable.setValueAt(propertyManager.getProperty(propertyName).getValue(), i, 1);
        }
    }

    public PluginViewFactory() {

        propertyManager = new core.PropertyManager();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        PropNameComboBox propNameComboBox = new PropNameComboBox(propertyManager.getPropertyNames());



        TableCellEditor editor = new DefaultCellEditor(propNameComboBox);
        propTable.setRowHeight(30);
        propTable.setModel(new PropertyTableModel());
        propTable.setAutoscrolls(true);
        propTable.getColumnModel().getColumn(0).setCellEditor(editor);


        
        propTable.getModel().addTableModelListener(new TableModelListener() {
            /*
            property name at col=0
            property value at col=1
                    */
            @Override
            public void tableChanged(TableModelEvent tableModelEvent) {

                if(isInitDone == false) return;
                int row = tableModelEvent.getFirstRow();
                int col = tableModelEvent.getColumn();
                String name = propTable.getValueAt(row,0).toString();
                if(col == 0) {
                    cellChangeToCurrentValueAtRow(row);
                }
                else if(col == 1){
                    Property property = propertyManager.getProperty(name);
                    if(property == null) {
                        // do nothing
                    }
                    else {
                        String value = propTable.getValueAt(row,1).toString();
                        propertyManager.setProperty(name, value);
                    }
                   }
                else {
                    System.out.println("Error : Col Number Over");
                }

            }
        });
        showAllProperty();
        isInitDone = true;
    }

    private void cellChangeToCurrentValueAtRow(int row) {
        String name = propTable.getValueAt(row,0).toString();
        Property property = propertyManager.getProperty(name);
        if(property == null) {
            propertyManager.putProperty(name, new Property(name, null));
        }
        String value;
        value = propertyManager.getProperty(name).getValue();
        if(value != null) {
            propTable.setValueAt(propertyManager.getProperty(name).getValue(),row,1);
        }
    }

    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        myToolWindow = toolWindow;
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(PluginViewContent, "", false);
        toolWindow.getContentManager().addContent(content);
    }
}
