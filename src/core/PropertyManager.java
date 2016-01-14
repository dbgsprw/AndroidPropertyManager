package core;

import com.android.ddmlib.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.containers.HashMap;
import com.intellij.util.text.StringTokenizer;
import exception.NullValueException;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


/**
 * Created by dbgsprw on 16. 1. 11.
 */
public class PropertyManager {
    private static PropertyManager sPropertyManager;
    private HashMap<String, Property> mProperties;
    private ArrayList<String> mPropertyNames;
    private String mAndroidHome;
    private AndroidDebugBridge mADB;
    private IDevice mCurrentDevice;
    private ProjectManager mProjectManager;
    private Project mProject;


    private PropertyManager() {


        mProperties = new HashMap<String, Property>();
        mPropertyNames = new ArrayList<String>();

        mProjectManager = ProjectManager.getInstance();
        mProject = mProjectManager.getDefaultProject();

        try {
            Process process = Runtime.getRuntime().exec("adb root");
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }

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
                System.out.println("DeviceConnected");
                mCurrentDevice = iDevice;
            }

            @Override
            public void deviceDisconnected(IDevice iDevice) {
                //  mCurrentDevice = null;
                System.out.println("DeviceDisconnected");
            }

            @Override
            public void deviceChanged(IDevice iDevice, int i) {
                System.out.println("DeviceChanged");
                //deviceConnected(iDevice);
            }
        });
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
                        // 다 받았을 때
                                /*
                                출력이 다 끝난후 string == "" 하나가 옴
                                 */
                        if ("".equals(line)) {
                            PluginViewFactory pluginViewFactory = PluginViewFactory.getInstance();
                            if (pluginViewFactory.isViewInited()) {
                                pluginViewFactory.updateTable();
                            }
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
        if (!mProperties.containsKey(name)) {
            mPropertyNames.add(name);
            mProperties.put(name, property);
        } else {
            mProperties.put(name, property);
            // Message : Already Contain
        }
    }

    public void setPropertyValue(String name, String value) throws Exception {
        Property property = getProperty(name);
        if ("".equals(value)) {
            Messages.showMessageDialog(mProject, "Cannot set property : value cannot be null.", "Error", Messages.getInformationIcon());
            throw new NullValueException();
        } else if (property == null) {
            Messages.showMessageDialog(mProject, "Cannot set property : cannot find that property.", "Error", Messages.getInformationIcon());
            throw new Exception();
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
                        Messages.showMessageDialog(mProject, "Cannot set property now. Please save prop file and reboot device.", "Error", Messages.getInformationIcon());
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
        return mProperties.get(name);
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
            process = Runtime.getRuntime().exec("adb pull system/build.prop" + " " + path + File.separator + "build.prop(" + currentTime + ")");
            process.waitFor();
            // 기존 파일 프로젝트 디렉터리에 저장했다고 메시지
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        PropFileMaker propFileMaker = new PropFileMaker(this);
        propFileMaker.makePropFileToPath(path + File.separator + "createdBuild.prop");
        try {
            process = Runtime.getRuntime().exec("adb remount");
            process.waitFor();
            process = Runtime.getRuntime().exec("adb push " + path + File.separator + "createdBuild.prop" + " " + "/system/build.prop");
            process.waitFor();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void rebootDevice() throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException {
        mCurrentDevice.executeShellCommand("reboot", new NullOutputReceiver());
    }
}
