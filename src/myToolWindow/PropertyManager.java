package myToolWindow;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.Log;
import com.android.ddmlib.MultiLineReceiver;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.containers.HashMap;
import com.intellij.util.text.StringTokenizer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;


/**
 * Created by myoo on 16. 1. 11.
 */
public class PropertyManager {
    private HashMap<String, Property> properties;
    private String mAndroidHome;
    private AndroidDebugBridge adb;
    private ArrayList<String> propertyNames;
    private IDevice currentDevice;
    private ProjectManager projectManager;
    private Project project;

    public PropertyManager() {
        projectManager = ProjectManager.getInstance();
        projectManager.getDefaultProject();
        properties = new HashMap<String, Property>();
        propertyNames = new ArrayList<String>();
        mAndroidHome = System.getenv("ANDROID_HOME");
        if (mAndroidHome == null) {

            Messages.showMessageDialog(project, "Cannot Find ANDROID_HOME","Error", Messages.getInformationIcon());
            System.out.println("Cannot Find ANDROID_HOME");
        }



        AndroidDebugBridge.init(true);
        File adbPath = new File(mAndroidHome, "platform-tools" + File.separator + "adb");
        ;
        try {
            AndroidDebugBridge adb = AndroidDebugBridge.createBridge(adbPath.getCanonicalPath(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        adb.addDeviceChangeListener(new AndroidDebugBridge.IDeviceChangeListener() {
            @Override

            public void deviceConnected(IDevice iDevice) {
                currentDevice = iDevice;
                try {

                    iDevice.executeShellCommand("getprop", new MultiLineReceiver() {
                        @Override
                        public void processNewLines(String[] strings) {
                            for (String line : strings) {
                                // 다 받았을 때
                                /*
                                출력이 다 끝난후 string == "" 하나가 옴
                                 */
                                if (line.equals("")) {
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

            }

            @Override
            public void deviceChanged(IDevice iDevice, int i) {
            }
        });
    }


    public PropertyManager(AndroidDebugBridge adb) {
        this.adb = adb;
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
        if(properties.containsKey(name)) {
            // Message : Already Contain
        }
        else {
            propertyNames.add(name);
            properties.put(name, property);
        }
    }

    /*
    * return result code
    * false : error
    * true : done
    * */
    public boolean setProperty(String name, String value) {
        Property property = getProperty(name);
        if(property == null) {
            return false;
            // Message : Not Contain
        }
        else {
            try {
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
                        if(strings[0].equals("")) {
                            return;
                        }
                        if(!strings[0].trim().equals(finalValue)) {
                            Messages.showMessageDialog(project, "Cannot set property. Returned to old value","Error", Messages.getInformationIcon());

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
            property.setValue(value);
            return true;
        }
    }

    public Property getProperty(String name) {
        return properties.get(name);
    }

    public ArrayList<String> getPropertyNames() {
        return propertyNames;
    }
}
