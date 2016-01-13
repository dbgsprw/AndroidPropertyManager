package core;

import com.android.ddmlib.AndroidDebugBridge;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import exception.NullValueException;
import temp.Java2sAutoComboBox;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.*;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;

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

    private boolean isUpdateDone;

    private PropNameComboBox propNameComboBox;

    private core.PropertyManager propertyManager;

    private static PluginViewFactory pluginViewFactory;

    private final static int COLUMN_PROPERTY_NAME = 0;
    private final static int COLUMN_PROPERTY_VALUE = 1;

    /*
    static int sNumber;
    static {
        sNumber = 1;
        File adbPath = new File(System.getenv("ANDROID_HOME"), "platform-tools" + File.separator + "adb");


    }*/

    private void showAllProperty() {
        ArrayList<String> propertyNames = propertyManager.getPropertyNames();
        int length = propertyNames.size();
        for (int i = 0; i < length; i++) {
            String propertyName = propertyNames.get(i);

            propTable.setValueAt(propertyName, i, COLUMN_PROPERTY_NAME);

            propTable.setValueAt(propertyManager.getProperty(propertyName).getValue(), i, COLUMN_PROPERTY_VALUE);
        }
    }

    public void updateTable() {
        isUpdateDone = false;
        propNameComboBox.setDataList(propertyManager.getPropertyNames());
        showAllProperty();
        isUpdateDone = true;
    }

    public static PluginViewFactory getInstance() {
        return pluginViewFactory;
    }


    public PluginViewFactory() {
        pluginViewFactory = this;
        propertyManager = PropertyManager.getInstance();

        propNameComboBox = new PropNameComboBox(propertyManager.getPropertyNames());


        TableCellEditor editor = new DefaultCellEditor(propNameComboBox);
        propTable.setRowHeight(30);
        propTable.setModel(new PropertyTableModel());
        propTable.setAutoscrolls(true);
        propTable.getColumnModel().getColumn(COLUMN_PROPERTY_NAME).setCellEditor(editor);
        propTable.getModel().addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent tableModelEvent) {

                if (isUpdateDone == false) return;
                int row = tableModelEvent.getFirstRow();
                int col = tableModelEvent.getColumn();
                String name = propTable.getValueAt(row, COLUMN_PROPERTY_NAME).toString();
                if (col == 0) {
                    cellChangeToCurrentValueAtRow(row);
                } else if (col == 1) {
                    Property property = propertyManager.getProperty(name);
                    if (property != null) {
                        String value = propTable.getValueAt(row, COLUMN_PROPERTY_VALUE).toString();
                        if (!value.equals(propertyManager.getProperty(name))) {
                            try {
                                propertyManager.setProperty(name, value);
                            } catch (NullValueException e) {
                                cellChangeToCurrentValueAtRow(row);
                                e.printStackTrace();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } else {
                    System.out.println("Error : Col Number Over");
                }

            }
        });
    }

    private void cellChangeToCurrentValueAtRow(int row) {
        String name = propTable.getValueAt(row, COLUMN_PROPERTY_NAME).toString();
        Property property = propertyManager.getProperty(name);
        if ("".equals(name)) {
            return;
        }
        if (property == null) {
            propertyManager.putProperty(name, new Property(name, null));
        }
        String value;
        value = propertyManager.getProperty(name).getValue();
        if (value != null) {
            propTable.setValueAt(propertyManager.getProperty(name).getValue(), row, COLUMN_PROPERTY_VALUE);
        }
    }

    public void createToolWindowContent(Project project, ToolWindow toolWindow) {

        myToolWindow = toolWindow;
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(PluginViewContent, "", false);
        toolWindow.getContentManager().addContent(content);
    }
}
