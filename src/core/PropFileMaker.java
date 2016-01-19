package core;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by dbgsprw on 16. 1. 14.
 */
public class PropFileMaker {

    private DeviceManager mDeviceManager;

    public PropFileMaker(DeviceManager deviceManager) {
        this.mDeviceManager = deviceManager;
    }

    public void makePropFileToPath(String path) {
        File file = new File(path);
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        ArrayList<String> propertyNameList = mDeviceManager.getPropertyNames();
        for (String propertyName : propertyNameList) {
            Property property = mDeviceManager.getProperty(propertyName);
            try {
                fileWriter.append(property.toString() + "\n");

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
