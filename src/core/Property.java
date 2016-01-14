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
        return "Property{" +
                "Name='" + mName + '\'' +
                ", Value='" + mValue + '\'' +
                '}';
    }
}
