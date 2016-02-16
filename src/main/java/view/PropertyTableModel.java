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
