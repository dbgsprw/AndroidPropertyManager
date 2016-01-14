package core;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.MultiLineReceiver;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.containers.HashMap;
import com.intellij.util.text.StringTokenizer;
import exception.NullValueException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;


/**
 * Created by myoo on 16. 1. 11.
 */
public class PropertyManager {
    private static PropertyManager propertyManager;
    private HashMap<String, Property> properties;
    private ArrayList<String> propertyNames;
    private String mAndroidHome;
    private AndroidDebugBridge adb;
    private IDevice currentDevice;
    private ProjectManager projectManager;
    private Project project;


    private PropertyManager() {


        properties = new HashMap<String, Property>();
        propertyNames = new ArrayList<String>();

        projectManager = ProjectManager.getInstance();
        project = projectManager.getDefaultProject();
        try {
            Process process = Runtime.getRuntime().exec("adb root");
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }

        mAndroidHome = System.getenv("ANDROID_HOME");
        if (mAndroidHome == null) {

            Messages.showMessageDialog(project, "Cannot Find $ANDROID_HOME\nPlease set $ANDROID_HOME and restart", "Error", Messages.getInformationIcon());
            System.out.println("Cannot Find ANDROID_HOME");
        }

        AndroidDebugBridge.init(true);

        File adbPath = new File(mAndroidHome, "platform-tools" + File.separator + "adb");
        try {
            adb = AndroidDebugBridge.createBridge(adbPath.getCanonicalPath(), true);

        } catch (IOException e) {
            e.printStackTrace();
        }


        adb.addDeviceChangeListener(new AndroidDebugBridge.IDeviceChangeListener() {
            @Override
            public void deviceConnected(IDevice iDevice) {
                System.out.println("DeviceConnected");
                currentDevice = iDevice;
                try {
                    currentDevice.executeShellCommand("getprop", new MultiLineReceiver() {
                        @Override
                        public void processNewLines(String[] strings) {
                            for (String line : strings) {
                                // 다 받았을 때
                                /*
                                출력이 다 끝난후 string == "" 하나가 옴
                                 */
                                if ("".equals(line)) {
                                    PluginViewFactory.getInstance().updateTable();
                                    return;
                                }
                                Property property = lineToProperty(line);
                                putProperty(property.getName(), property);
                            }
                        }

                        @Override
                        public boolean isCancelled() {
                            return false;
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void deviceDisconnected(IDevice iDevice) {
                currentDevice = null;
                System.out.println("DeviceDisconnected");
            }

            @Override
            public void deviceChanged(IDevice iDevice, int i) {
                deviceConnected(iDevice);
            }
        });
    }

    public PropertyManager(AndroidDebugBridge adb) {
        this.adb = adb;
    }

    public static PropertyManager getInstance() {
        if (propertyManager == null) {
            propertyManager = new PropertyManager();
        }
        return propertyManager;
    }

    private Property lineToProperty(String line) {
        StringTokenizer stringTokenizer;
        String name, value;
        line = line.substring(1);
        stringTokenizer = new StringTokenizer(line);
        name = stringTokenizer.nextToken("]");
        value = stringTokenizer.nextToken().substring(3);

        return new Property(name, value);
    }

    public void putProperty(String name, Property property) {
        if (!properties.containsKey(name)) {
            propertyNames.add(name);
            properties.put(name, property);
        } else {
            // Message : Already Contain
        }
    }

    public void setProperty(String name, String value) throws Exception {
        Property property = getProperty(name);
        if ("".equals(value)) {
            Messages.showMessageDialog(project, "Cannot set property : value cannot be null", "Error", Messages.getInformationIcon());
            throw new NullValueException();
        } else if (property == null) {
            Messages.showMessageDialog(project, "Cannot set property : cannot find that property", "Error", Messages.getInformationIcon());
            throw new Exception();
        } else {
            currentDevice.executeShellCommand("setprop " + name + " " + value, new MultiLineReceiver() {
                @Override
                public void processNewLines(String[] strings) {
                    System.out.println("==== setprop result ====");
                    for (String line : strings) {
                        System.out.println(line);
                    }
                }

                @Override
                public boolean isCancelled() {
                    return false;
                }
            });
            // Test : is Done

            final String finalValue = value;
            currentDevice.executeShellCommand("getprop " + name, new MultiLineReceiver() {
                @Override
                public void processNewLines(String[] strings) {
                    if ("".equals(strings[0])) {
                        return;
                    }
                    if (!strings[0].trim().equals(finalValue)) {
                        Messages.showMessageDialog(project, "Cannot set property. Returned to old value\n Please check adb is in root", "Error", Messages.getInformationIcon());
                        try {
                            throw new Exception();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public boolean isCancelled() {
                    return false;
                }
            });
            property.setValue(value);
        }
    }

    public Property getProperty(String name) {
        return properties.get(name);
    }

    public ArrayList<String> getPropertyNames() {
        return propertyNames;
    }
}
