package core;

import com.android.ddmlib.*;
import com.android.ddmlib.log.LogReceiver;
import com.intellij.util.containers.HashMap;

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

    public Device(IDevice IDevice) {
        mIDevice = IDevice;
        mProperties = new HashMap<String, Property>();
        mPropertyNames = new ArrayList<>();
        mProcessBuilder = new java.lang.ProcessBuilder();
        name = IDevice.getName();
        executeAdbCommand("root");
    }

    public HashMap<String, Property> getProperties() {
        return mProperties;
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

    public void executeAdbCommand(String... args) {
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

    public Property getProperty(String name) {
        return mProperties.get(name);
    }

    public ArrayList<String> getPropertyNames() {
        return mPropertyNames;
    }
}
