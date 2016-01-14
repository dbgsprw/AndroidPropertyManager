package core;

import com.android.ddmlib.AndroidDebugBridge;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.locks.Lock;

public class PluginViewFactory implements ToolWindowFactory {

    private ProjectManager projectManager;
    private Project project;
    /*
    View Components
     */
    private JPanel PluginViewContent;
    private JButton saveCustomTableButton;
    private JButton resetButton;
    private JButton saveToIntellijButton;
    private JTable propTable;
    private ToolWindow myToolWindow;
    private JComboBox tableViewComboBox;
    private PropertiesComponent propertiesComponent;
    private JLabel changeTableLabel;

    private boolean isUpdateDone;

    private PropNameComboBox propNameComboBox;
    private ArrayList<String> tableViewList;

    private core.PropertyManager propertyManager;

    private static PluginViewFactory pluginViewFactory;

    private final static int COLUMN_PROPERTY_NAME = 0;
    private final static int COLUMN_PROPERTY_VALUE = 1;
    private final static String TABLE_VIEW_LIST_PROPERTY_NAME = "TABLE_VIEW_LIST";
    private final static String ALL_TABLE_VIEW_PROPERTY_NAME = "ALL";

    public void updateTable() {
        String selectedTableView = tableViewComboBox.getSelectedItem().toString();
        updateTable(selectedTableView);
    }

    public void updateTable(String selectedTableView) {
        propNameComboBox.setDataList(propertyManager.getPropertyNames());

        isUpdateDone = false;
        if (ALL_TABLE_VIEW_PROPERTY_NAME.equals(selectedTableView)) {
            showAllProperty();
        } else {
            showSelectedTableViewProperty(selectedTableView);
        }
        isUpdateDone = true;
    }

    public static PluginViewFactory getInstance() {
        return pluginViewFactory;
    }


    public PluginViewFactory() {
        projectManager = ProjectManager.getInstance();
        project = projectManager.getDefaultProject();
        propertiesComponent = PropertiesComponent.getInstance();
//        propertiesComponent.unsetValue(TABLE_VIEW_LIST_PROPERTY_NAME);

        pluginViewFactory = this;
        propertyManager = PropertyManager.getInstance();
        tableViewComboBox.setEditable(true);

        getTableView();
        tableViewComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                if(itemEvent.getStateChange() == ItemEvent.DESELECTED) return;
                String tableView = itemEvent.getItem().toString();
                if(TABLE_VIEW_LIST_PROPERTY_NAME.equals(tableView)) {
                    Messages.showMessageDialog(project, "Cannot use that table view name", "Error", Messages.getInformationIcon());
                    return;
                }
                updateTable(tableView);
            }
        });

        saveCustomTableButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                String currentTableViewName = tableViewComboBox.getSelectedItem().toString();
                if(ALL_TABLE_VIEW_PROPERTY_NAME.equals(tableViewComboBox.getSelectedItem())) {
                    Messages.showMessageDialog(project, "Cannot save at ALL table, please use custom table view", "Error", Messages.getInformationIcon());
                }
                ArrayList<String> propertyNameList = new ArrayList<String>();
                int tableRowLength = propTable.getRowCount();
                for (int i = 0; i < tableRowLength; i++) {
                    propertyNameList.add(propTable.getValueAt(i, COLUMN_PROPERTY_NAME).toString());
                }
                propertiesComponent.setValues(currentTableViewName, propertyNameList.toArray(new String[propertyNameList.size()]));
            }
        });

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

    private void getTableView() {
        tableViewComboBox.addItem(ALL_TABLE_VIEW_PROPERTY_NAME);
        String[] tableViews = propertiesComponent.getValues(TABLE_VIEW_LIST_PROPERTY_NAME);
        if (tableViews == null) {
            tableViewList = new ArrayList<String>();
            return;
        }
        tableViewList = new ArrayList<String>(Arrays.asList(tableViews));
        for (String tableView : tableViewList) {
            tableViewComboBox.addItem(tableView);
        }
    }

    private void showProperty(ArrayList<String> propertyNames) {
        int propertyLength = propertyNames.size();
        int tableRowLength = propTable.getRowCount();
        for (int i = 0; i < propertyLength; i++) {
            String propertyName = propertyNames.get(i);

            propTable.setValueAt(propertyName, i, COLUMN_PROPERTY_NAME);

            propTable.setValueAt(propertyManager.getProperty(propertyName).getValue(), i, COLUMN_PROPERTY_VALUE);
        }
        for (int i = propertyLength; i < tableRowLength; i++) {
            propTable.setValueAt("", i, COLUMN_PROPERTY_NAME);
            propTable.setValueAt("", i, COLUMN_PROPERTY_VALUE);
        }

    }

    private void showAllProperty() {
        ArrayList<String> propertyNames = propertyManager.getPropertyNames();
        showProperty(propertyNames);
    }

    private void showSelectedTableViewProperty(String tableViewName) {
        String[] values = propertiesComponent.getValues(tableViewName);
        if (values == null) {
            if(!tableViewList.contains(tableViewName)) {
                tableViewList.add(tableViewName);
                propertiesComponent.setValues(TABLE_VIEW_LIST_PROPERTY_NAME, tableViewList.toArray(new String[tableViewList.size()]));
                tableViewComboBox.addItem(tableViewName);
            }
            values = new String[0];
        }

        ArrayList<String> propertyNames = new ArrayList<String>(Arrays.asList(values));
        showProperty(propertyNames);
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
