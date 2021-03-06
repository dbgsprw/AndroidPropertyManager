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

import lib.Java2sAutoComboBox;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;

/**
 * Created by dbgsprw on 16. 1. 11.
 */
public class PropNameComboBox extends Java2sAutoComboBox {


    public PropNameComboBox(List list) {
        super(list);
        setStrict(false);
        addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                    PropNameComboBox propNameComboBox = (PropNameComboBox) itemEvent.getSource();
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
