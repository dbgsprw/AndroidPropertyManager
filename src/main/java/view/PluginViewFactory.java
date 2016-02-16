/*Copyright 2016 dbgsprw / dbgsprw@gmail.com

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.*/

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
import exception.NullAndroidHomeException;
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
import java.io.File;
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
    private JButton mLoadPropFileButton;
    private JButton mPushPropFileButton;
    private PropNameComboBox mPropNameComboBox;

    private boolean mIsChosenFileWritable;
    private boolean mIsChosenFileExist;


    private ArrayList<String> mTableViewList;

    private boolean mIsUpdateDone;
    private final Color DEFAULT_TABLE_BACKGROUND_COLOR;


    public PluginViewFactory() {
        sPluginViewFactory = this;
        mDeviceManager = DeviceManager.getInstance();
        DEFAULT_TABLE_BACKGROUND_COLOR = mPropTable.getBackground();
    }

    public static PluginViewFactory getInstance() {
        return sPluginViewFactory;
    }


    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        try {
            mDeviceManager.adbInit();
        } catch (NullAndroidHomeException e) {
            e.printStackTrace();
            return;
        }
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

    public void SelectedDeviceChanged() {
        mDeviceManager.changeDevice(mDeviceListComboBox.getSelectedItem().toString());
        mDeviceManager.setRootMode();

        switch (mDeviceManager.getCurrentDeviceState()) {
            case DeviceManager.DeviceState.PROPERTY_EDITABLE:
                mPropTable.setEnabled(true);
                mPropTable.setBackground(DEFAULT_TABLE_BACKGROUND_COLOR);
                setHint("Device is root-mode");
                break;
            case DeviceManager.DeviceState.PROPERTY_VISIBLE:
                mPropTable.setEnabled(false);
                mPropTable.setBackground(new Color(230, 230, 230));
                setHint("Device is user-mode : You can't edit value");
                break;
            case DeviceManager.DeviceState.PROPERTY_INVISIBLE:
                mPropTable.setEnabled(false);
                mPropTable.setBackground(new Color(230, 230, 230));
                clearTable();
                setHint("Device is unauthorized or offline");
                break;
        }

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
                    Object valueObject = mPropTable.getValueAt(row, COLUMN_PROPERTY_VALUE);
                    if (valueObject == null) {
                        return;
                    } else if("".equals(name)) {
                        mPropTable.setValueAt(null, row, COLUMN_PROPERTY_VALUE);
                        return;
                    }
                    String value = valueObject.toString();
                    if (property != null) {
                        if (!value.equals(mDeviceManager.getProperty(name))) {
                            try {
                                mDeviceManager.setPropertyValue(name, value);
                                saveTable();
                            } catch (NullValueException e) {
                                cellChangeToCurrentValueAtRow(row);
                                e.printStackTrace();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    } else if (value != null) {
                        mDeviceManager.putProperty(name, new Property(name, value));
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
                if (ItemEvent.SELECTED == itemEvent.getStateChange()) {
                    SelectedDeviceChanged();
                }

            }
        });


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


        final JFileChooser jFileChooser = new JFileChooser();
        jFileChooser.setCurrentDirectory(new File(mProject.getBasePath()));
        jFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        jFileChooser.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                File selectedFile = jFileChooser.getSelectedFile();
                if (selectedFile == null) return;
                mIsChosenFileExist = selectedFile.exists();
                mIsChosenFileWritable = selectedFile.getParentFile().canWrite();
            }
        });

        mSavePropFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (jFileChooser.showSaveDialog(mPluginViewContent) == JFileChooser.CANCEL_OPTION) return;
                if (!mIsChosenFileWritable) {
                    showMessage("Please choose writable path.", "Android Property Manager");
                    return;
                } else if (mIsChosenFileExist) {
                    if (Messages.showOkCancelDialog("Overwrite file?", "Android Property Manager",
                            Messages.getInformationIcon()) != Messages.OK) {
                        return;
                    }
                }
                String[] values = mPropertiesComponent.getValues(mTableViewListComboBox.getSelectedItem().toString());
                ArrayList<String> propertyNames;
                if (ALL_TABLE_VIEW_PROPERTY_NAME == mTableViewListComboBox.getSelectedItem().toString()) {
                    propertyNames = mDeviceManager.getPropertyNames();
                } else {
                    propertyNames = new ArrayList<>(Arrays.asList(values));
                }
                mDeviceManager.savePropFile(jFileChooser.getSelectedFile().getPath(), propertyNames);
            }
        });

        mLoadPropFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (jFileChooser.showDialog(mPluginViewContent, "Choose") == JFileChooser.CANCEL_OPTION) return;

                if (mIsChosenFileExist) {
                    mDeviceManager.loadPropFile(jFileChooser.getSelectedFile().getPath());
                    mDeviceManager.updatePropFromDevice();
                } else {
                    showMessage("Please select exist file.", "Android Property Manager");
                }
            }
        });

        mPushPropFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (jFileChooser.showDialog(mPluginViewContent, "Choose") == JFileChooser.CANCEL_OPTION) return;
                if (mIsChosenFileExist) {
                    String oldPropDirPath = mProject.getBasePath();
                    mDeviceManager.pushPropFile(jFileChooser.getSelectedFile().getPath(), oldPropDirPath);
                    PluginViewFactory.getInstance().
                            setHint("Property file is saved at system/build.prop of device file system");
                } else {
                    showMessage("Please select exist file.", "Android Property Manager");
                }
            }
        });

        mRestartRuntimeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    PluginViewFactory.getInstance().setHint("Device is restarting runtime...");
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
                    PluginViewFactory.getInstance().setHint("Device is rebooting..., so now it is offline state");
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

    public void saveTable() {
        String currentTableViewName = mTableViewListComboBox.getSelectedItem().toString();
        if (ALL_TABLE_VIEW_PROPERTY_NAME.equals(mTableViewListComboBox.getSelectedItem())) {
            return;
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

    public void clearTable() {
        int tableRowLength = mPropTable.getRowCount();
        mIsUpdateDone = false;
        for (int i = 0; i < tableRowLength; i++) {
            mPropTable.setValueAt("", i, COLUMN_PROPERTY_NAME);
            mPropTable.setValueAt("", i, COLUMN_PROPERTY_VALUE);
        }
        mIsUpdateDone = true;
    }

    public void updateDeviceListComboBox() {
        ArrayList<String> deviceNameList = mDeviceManager.getConnectedDeviceNameList();
        if (deviceNameList.size() == 0) {
            setHint("Can't find device. Maybe it is booting or not connected");
            mPropTable.setEnabled(false);
            mPropTable.setBackground(DEFAULT_TABLE_BACKGROUND_COLOR);
            clearTable();
        }
        mDeviceListComboBox.removeAllItems();
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
        ArrayList<String> propertyNames = new ArrayList<>(Arrays.asList(values));
        showProperty(propertyNames);
    }

    private void showProperty(ArrayList<String> propertyNames) {
        int propertyLength = propertyNames.size();
        int tableRowLength = mPropTable.getRowCount();
        for (int i = 0; i < propertyLength; i++) {
            String propertyName = propertyNames.get(i);

            mPropTable.setValueAt(propertyName, i, COLUMN_PROPERTY_NAME);
            Property property = mDeviceManager.getProperty(propertyName);
            if (property != null) {
                mPropTable.setValueAt(mDeviceManager.getProperty(propertyName).getValue(), i, COLUMN_PROPERTY_VALUE);
            } else {
                mPropTable.setValueAt("", i, COLUMN_PROPERTY_VALUE);
            }
        }
        for (int i = propertyLength; i < tableRowLength; i++) {
            mPropTable.setValueAt("", i, COLUMN_PROPERTY_NAME);
            mPropTable.setValueAt("", i, COLUMN_PROPERTY_VALUE);
        }

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
        mPropTable.setValueAt(value, row, COLUMN_PROPERTY_VALUE);
    }

    public void setHint(String text) {
        mHintLabel.setText(text);
    }

    public void showMessage(String message, String type) {
        Messages.showMessageDialog(mProject, message, type, Messages.getInformationIcon());
    }
}
