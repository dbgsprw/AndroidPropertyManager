package core;

import com.android.ddmlib.*;
import com.android.ddmlib.log.LogReceiver;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.containers.HashMap;
import exception.NullValueException;
import view.PluginViewFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by dbgsprw on 16. 1. 18.
 */
public class Device {
    private IDevice mIDevice;
    private HashMap<String, Property> mProperties;
    private ArrayList<String> mPropertyNames;
    private java.lang.ProcessBuilder mProcessBuilder;
    private String name;
    private boolean isRootMode;



    public Device(IDevice IDevice) {
        mIDevice = IDevice;
        mProperties = new HashMap<String, Property>();
        mPropertyNames = new ArrayList<>();
        mProcessBuilder = new java.lang.ProcessBuilder();
        name = IDevice.getSerialNumber();
        executeAdbCommand("root");
        executeShellCommand("id -u", new MultiLineReceiver() {
            @Override
            public void processNewLines(String[] lines) {
                if("0".equals(lines[0])) {
                    isRootMode = true;
                }
            }

            @Override
            public boolean isCancelled() {
                return false;
            }
        });
    }

    public boolean isRootMode() {
        return isRootMode;
    }

    public boolean isUnauthorized() {
        if (mIDevice.getState() == IDevice.DeviceState.UNAUTHORIZED) {
            return true;
        }
        return false;
    }


    public HashMap<String, Property> getProperties() {
        return mProperties;
    }

    public void putProperty(String name, Property property) {
        if (!mProperties.containsKey(name)) {
            getPropertyNames().add(name);
            mProperties.put(name, property);
        } else {
            mProperties.put(name, property);
        }
    }

    public Property getProperty(String name) {
        return mProperties.get(name);
    }

    public void setPropertyValue(String name, String value) {
        Property property = getProperty(name);
        executeShellCommand("setprop " + name + " " + value, new NullOutputReceiver());
        final String finalValue = value;
        executeShellCommand("getprop " + name, new MultiLineReceiver() {
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
        property.setValue(value);
    }

    public void executeShellCommand(String command) {
        try {
            mIDevice.executeShellCommand(command, new NullOutputReceiver());
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

    public void executeShellCommand(String command, IShellOutputReceiver receiver) {
        try {
            mIDevice.executeShellCommand(command, receiver);
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

    private void executeAdbCommand(String... args) {
        try {
            ArrayList<String> command = new ArrayList<String>();
            command.add("adb");
            command.add("-s");
            command.add(name);
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

    public void remount() {
        executeAdbCommand("remount");
    }

    public String getSerialNumber() {
        return mIDevice.getSerialNumber();
    }

    public void pushFile(String local, String remote) throws IOException, AdbCommandRejectedException, TimeoutException, SyncException {
        mIDevice.pushFile(local, remote);
    }

    public void reboot(String into) throws TimeoutException, AdbCommandRejectedException, IOException {
        mIDevice.reboot(into);
    }

    public void pullFile(String remote, String local) throws IOException, AdbCommandRejectedException, TimeoutException, SyncException {
        mIDevice.pullFile(remote, local);
    }

    public ArrayList<String> getPropertyNames() {
        return mPropertyNames;
    }

}
