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


import com.android.ddmlib.*;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkTypeId;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.containers.HashMap;
import com.intellij.util.text.StringTokenizer;
import exception.NullAndroidHomeException;
import exception.NullValueException;
import view.PluginViewFactory;

import java.io.*;
import java.lang.reflect.Array;
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
    private PropFileMaker propFileMaker;
    private String mAdbPath;

    private DeviceManager() {
        mDevices = new HashMap<>();
        mProjectManager = ProjectManager.getInstance();
        mProject = mProjectManager.getDefaultProject();
        propFileMaker = new PropFileMaker(this);
    }

    synchronized public static DeviceManager getInstance() {
        if (sDeviceManager == null) {
            sDeviceManager = new DeviceManager();
        }
        return sDeviceManager;
    }

    public void adbInit() throws NullAndroidHomeException {
        String androidHome = null;

        androidHome = findAndroidHome();

        if (androidHome == null) {
            Messages.showMessageDialog(mProject, "Cannot Find $ANDROID_HOME and Android SDK\nPlease set Android Sdk " +
                    "or $ANDROID_HOME and restart", "Android Property Manager", Messages.getInformationIcon());
            System.out.println("Cannot Find ANDROID_HOME");
            throw new NullAndroidHomeException();
        }

        AndroidDebugBridge.initIfNeeded(false);


        try {
            mAdbPath = new File(androidHome, "platform-tools" + File.separator + "adb").getCanonicalPath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mADB = AndroidDebugBridge.createBridge(mAdbPath, true);

        AndroidDebugBridge.addDeviceChangeListener(new AndroidDebugBridge.IDeviceChangeListener() {
            @Override
            public void deviceConnected(IDevice iDevice) {
                Device device = new Device(iDevice, mAdbPath);
                PluginViewFactory.getInstance().setHint("Device is Connected");
                String deviceName = device.getSerialNumber();
                if (!mDevices.containsKey(deviceName)) {
                    mDevices.put(deviceName, device);
                }
                System.out.println("Device Connected : " + deviceName);
                PluginViewFactory pluginViewFactory = PluginViewFactory.getInstance();
                if (pluginViewFactory != null) {
                    pluginViewFactory.updateDeviceListComboBox();
                }
            }

            @Override
            public void deviceDisconnected(IDevice iDevice) {
                //  mCurrentDevice = null;
                System.out.println("DeviceDisconnected");
                PluginViewFactory pluginViewFactory = PluginViewFactory.getInstance();
                mDevices.remove(iDevice.getSerialNumber());
                if (pluginViewFactory != null) {
                    pluginViewFactory.updateDeviceListComboBox();
                }
            }

            @Override
            public void deviceChanged(IDevice iDevice, int i) {
                System.out.println("DeviceChanged : " + iDevice.getName());
                PluginViewFactory pluginViewFactory = PluginViewFactory.getInstance();
                if (pluginViewFactory != null && IDevice.CHANGE_STATE == i) {
                    pluginViewFactory.SelectedDeviceChanged();
                }
            }
        });
    }

    private String findAndroidHome() {
        for (Sdk sdk : ProjectJdkTable.getInstance().getAllJdks()) {
            SdkTypeId sdkTypeId = sdk.getSdkType();
            if ("Android SDK".equals(sdkTypeId.getName())) {
                return sdk.getHomePath();
            }
        }
        return System.getenv("ANDROID_HOME");
    }

    public void putProperty(String name, Property property) {
        mDevice.putProperty(name, property);
    }

    public Property getProperty(String name) {
        return mDevice.getProperty(name);
    }

    public void setPropertyValue(String name, String value) throws NullValueException {
        if ("".equals(value)) {
            Messages.showMessageDialog(mProject, "Cannot set property : value cannot be null.", "Error", Messages.getInformationIcon());
            throw new NullValueException();
        }
        mDevice.setPropertyValue(name, value);
    }

    public ArrayList<String> getConnectedDeviceNameList() {
        ArrayList<String> deviceNameList = new ArrayList<>();
        for (Device device : mDevices.values()) {
            deviceNameList.add(device.getSerialNumber());
        }
        return deviceNameList;
    }

    public void changeDevice(String deviceName) {
        mDevice = mDevices.get(deviceName);
        updatePropFromDevice();
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

    public void setRootMode() {
        mDevice.setRootMode();
    }

    public interface DeviceState {
        public final static int PROPERTY_EDITABLE = 0;
        public final static int PROPERTY_VISIBLE = 1;
        public final static int PROPERTY_INVISIBLE = 2;
    }

    public int getCurrentDeviceState() {
        IDevice.DeviceState deviceState = mDevice.getState();
        if (deviceState == IDevice.DeviceState.ONLINE) {
            if (mDevice.isRootMode()) {
                return DeviceState.PROPERTY_EDITABLE;
            } else {
                return DeviceState.PROPERTY_VISIBLE;
            }
        } else {
            return DeviceState.PROPERTY_INVISIBLE;
        }
    }

    public ArrayList<String> getPropertyNames() {
        if (mDevice == null) {
            return new ArrayList<>();
        }
        return mDevice.getPropertyNames();
    }

    public void restartRuntime() throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException {
        mDevice.executeShellCommand("stop");
        mDevice.executeShellCommand("start");
    }

    public void savePropFile(String path, ArrayList<String> propertyNames) {
        ArrayList<Property> properties = new ArrayList<>();
        for (String propertyName : propertyNames) {
            Property property = getProperty(propertyName);
            if(property != null) {
                properties.add(property);
            }
        }
        propFileMaker.makePropFileToPath(path, properties);
    }

    public void pushPropFile(String path, String oldPropDirPath) {
        SimpleDateFormat simpleDataFormat = new SimpleDateFormat("yy-MM-dd.HHmmss.SSS");
        String currentTime = simpleDataFormat.format(new Date());
        try {
            mDevice.pullFile("system/build.prop", oldPropDirPath + File.separator + "build.prop(" + currentTime + ")");
        } catch (AdbCommandRejectedException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (SyncException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mDevice.remount();
        try {
            mDevice.pushFile(path, "/system/build.prop");
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

    public void loadPropFile(String path) {
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(path)));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String[] splitLine =line.split("=");
                if(splitLine.length != 2) {
                    continue;
                }
                Property property = new Property(splitLine[0], splitLine[1]);
                putProperty(property.getName(), property);
                setPropertyValue(property.getName(), property.getValue());
                System.out.println(property.getName());
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullValueException e) {
            e.printStackTrace();
        }
    }


    public void rebootDevice() throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException {
        mDevice.reboot(null);
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
}
