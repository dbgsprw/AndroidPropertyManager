package core;

import javax.swing.table.DefaultTableModel;

/**
 *
 * Created by myoo on 16. 1. 11.
 */
public class PropertyTableModel extends DefaultTableModel {



    @Override
    public String getColumnName(int col) {
        if(col == 0) {
            return "Name";
        }
        else {
            return "Value";
        }
    }




    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public int getRowCount() {
        return 500;
    }
}
