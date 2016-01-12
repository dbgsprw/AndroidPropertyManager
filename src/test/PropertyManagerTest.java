package test;

import myToolWindow.PropertyManager;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.*;

/**
 * Created by myoo on 16. 1. 12.
 */
public class PropertyManagerTest {
    @Test
    public void test()
    {
        PropertyManager propertyManager = new PropertyManager();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(propertyManager.getProperty("vold.post_fs_data_done"));
        ArrayList arrayList = propertyManager.getPropertyNames();
        for(Object string : arrayList) {
            System.out.println(string.toString());
        }

        try {
            Thread.sleep(1000 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}