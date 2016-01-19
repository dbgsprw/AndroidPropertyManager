package view;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.ddmlib.TimeoutException;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import core.DeviceManager;
import core.Property;
import exception.NullValueException;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class PluginViewFactory implements ToolWindowFactory {

    private final static int COLUMN_PROPERTY_NAME = 0;
    private final static int COLUMN_PROPERTY_VALUE = 1;
    private final static String TABLE_VIEW_LIST_PROPERTY_NAME = "TABLE_VIEW_LIST";
    private final static String ALL_TABLE_VIEW_PROPERTY_NAME = "ALL";

    private static PluginViewFactory sPluginViewFactory;
    private Project mProject;
    private PropertiesComponent mPropertiesComponent;
    private DeviceManager mDeviceManager;

    private ToolWindow mToolWindow;
    private JPanel mPluginViewContent;
    private JTable mPropTable;
    private JButton mSaveCustomTableButton;
    private JButton mSavePropFileButton;
    private JLabel mChangeTableLabel;
    private JComboBox mTableViewListComboBox;
    private JButton mRestartRuntimeButton;
    private JButton mRebootDeviceButton;
    private JButton mRefreshButton;
    private JLabel mHintLabel;
    private JComboBox mDeviceListComboBox;
    private JLabel mDeviceListLabel;
    private PropNameComboBox mPropNameComboBox;

    private ArrayList<String> mTableViewList;

    private boolean mIsUpdateDone;
    private final Color DEFAULT_TABLE_BACKGROUND_COLOR ;


    public PluginViewFactory() {
        sPluginViewFactory = this;
        mDeviceManager = DeviceManager.getInstance();
        DEFAULT_TABLE_BACKGROUND_COLOR = mPropTable.getBackground();

    }

    public static PluginViewFactory getInstance() {
        return sPluginViewFactory;
    }

    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        mProject = project;
        mPropertiesComponent = PropertiesComponent.getInstance(mProject);
        mPropNameComboBox = new PropNameComboBox(mDeviceManager.getPropertyNames());
        updateTableViewList();
        viewComponentInit();
        updateTable();

        mToolWindow = toolWindow;
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(mPluginViewContent, "", false);
        toolWindow.getContentManager().addContent(content);
    }

    private void deviceStateChanged() {
        Object item = mDeviceListComboBox.getSelectedItem();
        if(item == null) {
            mPropTable.setEnabled(false);
            mPropTable.setBackground(Color.gray);
        }
        else {
            mPropTable.setEnabled(true);
            mPropTable.setBackground(DEFAULT_TABLE_BACKGROUND_COLOR);
        }
        mDeviceManager.changeDevice(mDeviceListComboBox.getSelectedItem().toString());
    }

    private void viewComponentInit() {
        TableCellEditor editor = new DefaultCellEditor(mPropNameComboBox);
        mPropTable.setRowHeight(30);
        mPropTable.setModel(new PropertyTableModel());
        mPropTable.setAutoscrolls(true);
        mPropTable.getColumnModel().getColumn(COLUMN_PROPERTY_NAME).setCellEditor(editor);
        mPropTable.getModel().addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent tableModelEvent) {

                if (mIsUpdateDone == false) return;
                int row = tableModelEvent.getFirstRow();
                int col = tableModelEvent.getColumn();
                String name = mPropTable.getValueAt(row, COLUMN_PROPERTY_NAME).toString();
                if (col == COLUMN_PROPERTY_NAME) {
                    cellChangeToCurrentValueAtRow(row);
                } else if (col == COLUMN_PROPERTY_VALUE) {
                    Property property = mDeviceManager.getProperty(name);
                    if (property != null) {
                        String value = mPropTable.getValueAt(row, COLUMN_PROPERTY_VALUE).toString();
                        if (!value.equals(mDeviceManager.getProperty(name))) {
                            try {
                                mDeviceManager.setPropertyValue(name, value);
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

        mDeviceListComboBox.setPrototypeDisplayValue("XXXXXXXXXXXXX");
        mDeviceListComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                if(ItemEvent.SELECTED == itemEvent.getStateChange()) {
                    deviceStateChanged();
                }

            }
        });
        deviceStateChanged();


        mTableViewListComboBox.setPrototypeDisplayValue("XXXXXXXXXXXXX");
        mTableViewListComboBox.setEditable(true);

        mTableViewListComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                if (itemEvent.getStateChange() == ItemEvent.DESELECTED) return;
                String tableView = itemEvent.getItem().toString();
                if (TABLE_VIEW_LIST_PROPERTY_NAME.equals(tableView)) {
                    showMessage("Cannot use that table view name", "Error");
                    return;
                }
                updateTable(tableView);
            }
        });


        mRefreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                mDeviceManager.updatePropFromDevice();
            }
        });

        mSaveCustomTableButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                String currentTableViewName = mTableViewListComboBox.getSelectedItem().toString();
                if (ALL_TABLE_VIEW_PROPERTY_NAME.equals(mTableViewListComboBox.getSelectedItem())) {
                    showMessage("Cannot save at ALL table, please use custom table view", "Error");
                }
                ArrayList<String> propertyNameList = new ArrayList<String>();
                int tableRowLength = mPropTable.getRowCount();
                for (int i = 0; i < tableRowLength; i++) {
                    String propName = mPropTable.getValueAt(i, COLUMN_PROPERTY_NAME).toString();
                    if (!"".equals(propName)) {
                        propertyNameList.add(propName);
                    }
                }
                mPropertiesComponent.setValues(currentTableViewName, propertyNameList.toArray(new String[propertyNameList.size()]));
            }
        });

        mSavePropFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                mDeviceManager.savePropFile(mProject.getBasePath());
            }
        });

        mRestartRuntimeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    mDeviceManager.restartRuntime();
                } catch (TimeoutException e) {
                    e.printStackTrace();
                } catch (AdbCommandRejectedException e) {
                    e.printStackTrace();
                } catch (ShellCommandUnresponsiveException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        mRebootDeviceButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    setHint("Please wait rebooting");
                    mDeviceManager.rebootDevice();
                } catch (TimeoutException e) {
                    e.printStackTrace();
                } catch (AdbCommandRejectedException e) {
                    e.printStackTrace();
                } catch (ShellCommandUnresponsiveException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void updateDeviceListComboBox() {
        mDeviceListComboBox.removeAllItems();
        ArrayList<String> deviceNameList = mDeviceManager.getConnectedDeviceNameList();
        for (String deviceName : deviceNameList) {
            mDeviceListComboBox.addItem(deviceName);
        }
    }

    public void updateTable() {
        String selectedTableView = mTableViewListComboBox.getSelectedItem().toString();
        updateTable(selectedTableView);
    }

    public void updateTable(String selectedTableView) {
        mPropNameComboBox.setDataList(mDeviceManager.getPropertyNames());

        mIsUpdateDone = false;
        if (ALL_TABLE_VIEW_PROPERTY_NAME.equals(selectedTableView)) {
            showAllProperty();
        } else {
            showSelectedTableViewProperty(selectedTableView);
        }
        mIsUpdateDone = true;
    }

    private void updateTableViewList() {
        mTableViewListComboBox.addItem(ALL_TABLE_VIEW_PROPERTY_NAME);
        String[] tableViews = mPropertiesComponent.getValues(TABLE_VIEW_LIST_PROPERTY_NAME);
        if (tableViews == null) {
            mTableViewList = new ArrayList<String>();
            return;
        }
        mTableViewList = new ArrayList<String>(Arrays.asList(tableViews));
        for (String tableView : mTableViewList) {
            mTableViewListComboBox.addItem(tableView);
        }
    }

    private void showProperty(ArrayList<String> propertyNames) {
        int propertyLength = propertyNames.size();
        int tableRowLength = mPropTable.getRowCount();
        for (int i = 0; i < propertyLength; i++) {
            String propertyName = propertyNames.get(i);

            mPropTable.setValueAt(propertyName, i, COLUMN_PROPERTY_NAME);

            mPropTable.setValueAt(mDeviceManager.getProperty(propertyName).getValue(), i, COLUMN_PROPERTY_VALUE);
        }
        for (int i = propertyLength; i < tableRowLength; i++) {
            mPropTable.setValueAt("", i, COLUMN_PROPERTY_NAME);
            mPropTable.setValueAt("", i, COLUMN_PROPERTY_VALUE);
        }

    }

    private void showAllProperty() {
        ArrayList<String> propertyNames = mDeviceManager.getPropertyNames();
        showProperty(propertyNames);
    }

    private void showSelectedTableViewProperty(String tableViewName) {
        String[] values = mPropertiesComponent.getValues(tableViewName);
        if (values == null) {
            if (!mTableViewList.contains(tableViewName)) {
                mTableViewList.add(tableViewName);
                mPropertiesComponent.setValues(TABLE_VIEW_LIST_PROPERTY_NAME, mTableViewList.toArray(new String[mTableViewList.size()]));
                mTableViewListComboBox.addItem(tableViewName);
            }
            values = new String[0];
        }

        ArrayList<String> propertyNames = new ArrayList<String>(Arrays.asList(values));
        showProperty(propertyNames);
    }

    private void cellChangeToCurrentValueAtRow(int row) {
        String name = mPropTable.getValueAt(row, COLUMN_PROPERTY_NAME).toString();
        Property property = mDeviceManager.getProperty(name);
        if ("".equals(name)) {
            return;
        }
        if (property == null) {
            mDeviceManager.putProperty(name, new Property(name, null));
        }
        String value;
        value = mDeviceManager.getProperty(name).getValue();
        if (value != null) {
            mPropTable.setValueAt(mDeviceManager.getProperty(name).getValue(), row, COLUMN_PROPERTY_VALUE);
        }
    }

    public void setHint(String text) {
        mHintLabel.setText(text);
    }

    public void showMessage(String message, String type) {
        Messages.showMessageDialog(mProject, message, type, Messages.getInformationIcon());
    }
}
