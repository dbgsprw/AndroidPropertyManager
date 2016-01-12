package myToolWindow;

import com.intellij.ide.util.gotoByName.ChooseByNameBase;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBRadioButton;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.CheckBox;
import com.intellij.util.ui.ComboBoxCellEditor;
import com.intellij.util.ui.RadioButtonEnumModel;
import com.sun.org.apache.xerces.internal.impl.PropertyManager;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;
import temp.Java2sAutoComboBox;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;

public class MyToolWindowFactory implements ToolWindowFactory {

    /*
    View Components
     */
    private JPanel myToolWindowContent;
    private JButton button1;
    private JButton addBtn;
    private JButton resetButton;
    private JButton button4;
    private JTable propTable;
    private ToolWindow myToolWindow;
    private JComboBox comboBox1;

    private boolean isInitDone;

    private Java2sAutoComboBox propNameComboBox;

    private myToolWindow.PropertyManager propertyManager;

    private void  showAllProperty() {
        ArrayList<String> propertyNames = propertyManager.getPropertyNames();
        int length = propertyNames.size();
        for(int i = 0; i < length; i++) {
            String propertyName = propertyNames.get(i);
            propTable.setValueAt(propertyName, i, 0);
            propTable.setValueAt(propertyManager.getProperty(propertyName).getValue(), i, 1);
        }
    }

    public MyToolWindowFactory() {

        propertyManager = new myToolWindow.PropertyManager();
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


        /*
        hideToolWindowButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                myToolWindow.hide(null);
            }
        });*/

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

    // Create the tool window content.
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        myToolWindow = toolWindow;
        //this.currentDateTime();
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(myToolWindowContent, "", false);
        toolWindow.getContentManager().addContent(content);
    }
}
