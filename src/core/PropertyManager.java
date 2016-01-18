package core;

import com.android.ddmlib.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.containers.HashMap;
import com.intellij.util.text.StringTokenizer;
import exception.NullValueException;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


/**
 * Created by dbgsprw on 16. 1. 11.
 */
public class PropertyManager {
    private static PropertyManager sPropertyManager;
    private HashMap<String, Property> mProperties;
    private HashMap<String, IDevice> mDevices;
    private ArrayList<String> mPropertyNames;
    private String mAndroidHome;
    private AndroidDebugBridge mADB;
    private IDevice mCurrentDevice;
    private ProjectManager mProjectManager;
    private Project mProject;
    private java.lang.ProcessBuilder mProcessBuilder;
    private String mSelectedDeviceName;


    private PropertyManager() {
        mProperties = new HashMap<String, Property>();

        mDevices = new HashMap<String, IDevice>();
        mPropertyNames = new ArrayList<String>();

        mProjectManager = ProjectManager.getInstance();
        mProject = mProjectManager.getDefaultProject();
        mProcessBuilder = new java.lang.ProcessBuilder();

        mAndroidHome = System.getenv("ANDROID_HOME");
        if (mAndroidHome == null) {

            Messages.showMessageDialog(mProject, "Cannot Find $ANDROID_HOME\nPlease set $ANDROID_HOME and restart", "Error", Messages.getInformationIcon());
            System.out.println("Cannot Find ANDROID_HOME");
        }

        AndroidDebugBridge.init(true);

        File adbPath = new File(mAndroidHome, "platform-tools" + File.separator + "adb");
        try {
            mADB = AndroidDebugBridge.createBridge(adbPath.getCanonicalPath(), true);

        } catch (IOException e) {
            e.printStackTrace();
        }

        AndroidDebugBridge.addDeviceChangeListener(new AndroidDebugBridge.IDeviceChangeListener() {
            @Override
            public void deviceConnected(IDevice iDevice) {
                PluginViewFactory.getInstance().setHint("Device Connected");
                String deviceName = iDevice.getSerialNumber();
                if (!mDevices.containsKey(deviceName)) {
                    mDevices.put(deviceName, iDevice);
                }
                System.out.println("DeviceConnected : " + deviceName);
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
        return mProperties.get(name);
    }

    public void putProperty(String name, Property property) {
        if (!mProperties.containsKey(name)) {
            mPropertyNames.add(name);
            mProperties.put(name, property);
        } else {
            mProperties.put(name, property);
            // Message : Already Contain
        }
    }

    public ArrayList<String> getDeviceNameList() {
        ArrayList<String> deviceNameList = new ArrayList<String>();
        IDevice[] devices = mADB.getDevices();
        for (IDevice device : devices) {
            deviceNameList.add(device.getSerialNumber());
        }
        return deviceNameList;
    }

    private void execAdbCommand(String... args) {
        try {
            ArrayList<String> command = new ArrayList<String>();
            command.add("adb");
            command.add("-s");
            command.add(mSelectedDeviceName);
            for (String arg : args) {
                command.add(arg);
            }
            mProcessBuilder.command(command);
            Process process = mProcessBuilder.start();

            InputStream is = process.getErrorStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line;

            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void changeDevice(String deviceName) {
        mSelectedDeviceName = deviceName;
        mCurrentDevice = mDevices.get(deviceName);
        mProperties = new HashMap<String, Property>();
        mPropertyNames = new ArrayList<String>();
        execAdbCommand("root");
    }

    public PropertyManager(AndroidDebugBridge mADB) {
        this.mADB = mADB;
    }

    synchronized public static PropertyManager getInstance() {
        if (sPropertyManager == null) {
            sPropertyManager = new PropertyManager();
        }
        return sPropertyManager;
    }

    public void updatePropFromDevice() {

        try {
            mCurrentDevice.executeShellCommand("getprop", new MultiLineReceiver() {
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
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (AdbCommandRejectedException e) {
            e.printStackTrace();
        } catch (ShellCommandUnresponsiveException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

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


    public void setPropertyValue(String name, String value) throws Exception {
        Property property = getProperty(name);
        if ("".equals(value)) {
            Messages.showMessageDialog(mProject, "Cannot set property : value cannot be null.", "Error", Messages.getInformationIcon());
            throw new NullValueException();
        } else {
            mCurrentDevice.executeShellCommand("setprop " + name + " " + value, new MultiLineReceiver() {
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

            mCurrentDevice.executeShellCommand("getprop " + name, new MultiLineReceiver() {
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


    public ArrayList<String> getPropertyNames() {
        return mPropertyNames;
    }

    public void restartRuntime() throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException {

        mCurrentDevice.executeShellCommand("stop", new NullOutputReceiver());
        mCurrentDevice.executeShellCommand("start", new NullOutputReceiver());
    }

    public void savePropFile(String path) {
        Process process = null;
        SimpleDateFormat simpleDataFormat = new SimpleDateFormat("yy-MM-dd.HHmmss.SSS");
        String currentTime = simpleDataFormat.format(new Date());
        try {
            mCurrentDevice.pullFile("system/build.prop", path + File.separator + "build.prop(" + currentTime + ")");
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
        execAdbCommand("remount");
        try {
            mCurrentDevice.pushFile(path + File.separator + "createdBuild.prop", "/system/build.prop");
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
        mCurrentDevice.reboot(null);
    }
}
