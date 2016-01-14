package core;

import javax.swing.table.DefaultTableModel;

/**
 * Created by dbgsprw on 16. 1. 11.
 */
public class PropertyTableModel extends DefaultTableModel {
    private final static int ROW_COUNT = 500;
    private final static int COLUMN_COUNT = 2;
    private final static int COLUMN_PROPERTY_NAME = 0;
    private final static int COLUMN_PROPERTY_VALUE = 1;

    @Override
    public String getColumnName(int col) {
        switch (col) {
            case COLUMN_PROPERTY_NAME:
                return "Name";
            default:
                return "Value";
        }
    }


    @Override
    public int getColumnCount() {
        return COLUMN_COUNT;
    }

    @Override
    public int getRowCount() {
        return ROW_COUNT;
    }
}
