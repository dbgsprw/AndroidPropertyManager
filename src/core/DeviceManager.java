package core;

import com.android.ddmlib.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.containers.HashMap;
import com.intellij.util.text.StringTokenizer;
import exception.NullValueException;
import view.PluginViewFactory;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


/**
 * Created by dbgsprw on 16. 1. 11.
 */
public class DeviceManager {
    private static DeviceManager sDeviceManager;
    private HashMap<String, Device> mDevices;
    private AndroidDebugBridge mADB;
    private Device mDevice;
    private ProjectManager mProjectManager;
    private Project mProject;


    private DeviceManager() {
        mDevices = new HashMap<String, Device>();

        mProjectManager = ProjectManager.getInstance();
        mProject = mProjectManager.getDefaultProject();


        adbInit();
    }

    synchronized public static DeviceManager getInstance() {
        if (sDeviceManager == null) {
            sDeviceManager = new DeviceManager();
        }
        return sDeviceManager;
    }

    private void adbInit() {
        String androidHome = System.getenv("ANDROID_HOME");
        if (androidHome == null) {
            Messages.showMessageDialog(mProject, "Cannot Find $ANDROID_HOME\nPlease set $ANDROID_HOME and restart", "Error", Messages.getInformationIcon());
            System.out.println("Cannot Find ANDROID_HOME");
        }

        AndroidDebugBridge.init(true);

        File adbPath = new File(androidHome, "platform-tools" + File.separator + "adb");
        try {
            mADB = AndroidDebugBridge.createBridge(adbPath.getCanonicalPath(), true);

        } catch (IOException e) {
            e.printStackTrace();
        }

        AndroidDebugBridge.addDeviceChangeListener(new AndroidDebugBridge.IDeviceChangeListener() {
            @Override
            public void deviceConnected(IDevice iDevice) {
                Device device = new Device(iDevice);
                PluginViewFactory.getInstance().setHint("Device Connected");
                String deviceName = device.getSerialNumber();
                if (!mDevices.containsKey(deviceName)) {
                    mDevices.put(deviceName, device);
                }
                System.out.println("Device Connected : " + deviceName);
                PluginViewFactory.getInstance().updateDeviceListComboBox();
            }

            @Override
            public void deviceDisconnected(IDevice iDevice) {
                //  mCurrentDevice = null;
                System.out.println("DeviceDisconnected");
                PluginViewFactory.getInstance().updateDeviceListComboBox();
            }

            @Override
            public void deviceChanged(IDevice iDevice, int i) {

                System.out.println("DeviceChanged : " + iDevice.getName());
                //deviceConnected(iDevice);
            }
        });
    }

    public Property getProperty(String name) {
        return mDevice.getProperty(name);
    }

    public void setPropertyValue(String name, String value) throws NullValueException {
        Property property = getProperty(name);
        if ("".equals(value)) {
            Messages.showMessageDialog(mProject, "Cannot set property : value cannot be null.", "Error", Messages.getInformationIcon());
            throw new NullValueException();
        } else {
            mDevice.executeShellCommand("setprop " + name + " " + value, new MultiLineReceiver() {
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

            mDevice.executeShellCommand("getprop " + name, new MultiLineReceiver() {
                @Override
                public void processNewLines(String[] strings) {
                    if ("".equals(strings[0])) {
                        return;
                    }
                    if (!strings[0].trim().equals(finalValue)) {
                        PluginViewFactory.getInstance().setHint("Cannot set property now. Please save prop file and reboot device.");
                    }
                }

                @Override
                public boolean isCancelled() {
                    return false;
                }
            });
        }
        property.setValue(value);
    }

    public void putProperty(String name, Property property) {
        HashMap<String, Property> properties = mDevice.getProperties();
        if (!properties.containsKey(name)) {
            mDevice.getPropertyNames().add(name);
            properties.put(name, property);
        } else {
            properties.put(name, property);
            // Message : Already Contain
        }
    }

    public ArrayList<String> getConnectedDeviceNameList() {
        ArrayList<String> deviceNameList = new ArrayList<String>();
        IDevice[] devices = mADB.getDevices();
        for (IDevice device : devices) {
            deviceNameList.add(device.getSerialNumber());
        }
        return deviceNameList;
    }

    public void changeDevice(String deviceName) {
        mDevice = mDevices.get(deviceName);
    }

    public void updatePropFromDevice() {

        mDevice.executeShellCommand("getprop", new MultiLineReceiver() {
            @Override
            public void processNewLines(String[] strings) {
                for (String line : strings) {
                    if ("".equals(line)) {
                        PluginViewFactory pluginViewFactory = PluginViewFactory.getInstance();
                        pluginViewFactory.updateTable();
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

    public ArrayList<String> getPropertyNames() {
        if (mDevice == null) {
            return new ArrayList<>();
        }
        return mDevice.getPropertyNames();
    }

    public void restartRuntime() throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException {
        mDevice.executeShellCommand("stop", new NullOutputReceiver());
        mDevice.executeShellCommand("start", new NullOutputReceiver());
    }

    public void savePropFile(String path) {
        SimpleDateFormat simpleDataFormat = new SimpleDateFormat("yy-MM-dd.HHmmss.SSS");
        String currentTime = simpleDataFormat.format(new Date());
        try {
            mDevice.pullFile("system/build.prop", path + File.separator + "build.prop(" + currentTime + ")");
        } catch (AdbCommandRejectedException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (SyncException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        PropFileMaker propFileMaker = new PropFileMaker(this);
        propFileMaker.makePropFileToPath(path + File.separator + "createdBuild.prop");
        mDevice.executeAdbCommand("remount");
        try {
            mDevice.pushFile(path + File.separator + "createdBuild.prop", "/system/build.prop");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (AdbCommandRejectedException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (SyncException e) {
            e.printStackTrace();
        }

    }

    public void rebootDevice() throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException {
        mDevice.reboot(null);
    }
}
