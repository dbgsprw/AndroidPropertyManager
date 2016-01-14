package core;

import java.util.ArrayList;

/**
 * Created by dbgsprw on 16. 1. 12.
 */
public class Property {
    private final String name;
    ArrayList<String> valueHistory = new ArrayList<String>();
    private String value;

    public Property(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        valueHistory.add(value);
        this.value = value;
    }


    @Override
    public String toString() {
        return "Property{" +
                "name='" + name + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
