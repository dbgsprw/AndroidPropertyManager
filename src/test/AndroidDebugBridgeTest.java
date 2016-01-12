package test;

import com.android.ddmlib.*;
import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

public class AndroidDebugBridgeTest extends TestCase {
    private String mAndroidHome;

    @Override
    protected void setUp() throws Exception {
        mAndroidHome = System.getenv("ANDROID_HOME");
        assertNotNull(
                "This test requires ANDROID_HOME environment variable to point to a valid SDK",
                mAndroidHome);

        AndroidDebugBridge.init(true);
    }

    // https://code.google.com/p/android/issues/detail?id=63170
    public void testCanRecreateAdb() throws IOException {
        File adbPath = new File(mAndroidHome, "platform-tools" + File.separator + "adb");
      //  AndroidDebugBridge.init(true);;
        AndroidDebugBridge adb = AndroidDebugBridge.createBridge(adbPath.getCanonicalPath(), true);
        adb.addDeviceChangeListener(new AndroidDebugBridge.IDeviceChangeListener() {
            @Override

            public void deviceConnected(IDevice iDevice) {
                System.out.println("device Connected ================================");
                try {
                    /*
                    System.out.print("Begin");
                    Map<String, String> map = iDevice.getProperties();
                    System.out.print(map.size());
                    for(Map.Entry<String, String> entry:map.entrySet())
                    {
                        System.out.println("key = : " + entry.getKey());
                        System.out.print("     value = : " + entry.getValue());


                    }
                    System.out.println("End");
                    */
                    iDevice.executeShellCommand("getprop", new MultiLineReceiver() {
                        int cnt = 0 ;
                        @Override
                        public void processNewLines(String[] strings) {
                            // 다 받았을 때
                            if(strings.length == 0)
                            {

                            }
                            System.out.println(cnt++);
                            for(String line : strings)
                                System.out.println(line);
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
                System.out.println("device Changed ================================");
                try {
                    iDevice.executeShellCommand("getprop", new MultiLineReceiver() {
                        @Override
                        public void processNewLines(String[] strings) {
                            for(String line : strings)
                                System.out.println(line);
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
        });

        try {
            Thread.sleep(1000 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertNotNull(adb);

        AndroidDebugBridge.terminate();

        adb = AndroidDebugBridge.createBridge(adbPath.getCanonicalPath(), true);

        AndroidDebugBridge.terminate();

    }
}

