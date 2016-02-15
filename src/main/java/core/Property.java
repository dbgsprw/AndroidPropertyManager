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

package core;

import java.util.ArrayList;

/**
 * Created by dbgsprw on 16. 1. 12.
 */
public class Property {
    private final String mName;
    private ArrayList<String> mValueHistory = new ArrayList<String>();
    private String mValue;

    public Property(String name, String value) {
        this.mName = name;
        this.mValue = value;
    }

    public String getName() {
        return mName;
    }

    public String getValue() {
        return mValue;
    }

    public void setValue(String value) {
        mValueHistory.add(value);
        this.mValue = value;
    }


    @Override
    public String toString() {
        return mName + "=" + mValue;
    }
}
