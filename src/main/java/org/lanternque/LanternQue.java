package org.lanternque;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class LanternQue {
    public static void main(String[] args) {
        UIThread uiThread = new UIThread();
        Executor uiExe = Executors.newSingleThreadExecutor();
        uiExe.execute(uiThread);
    }
}
