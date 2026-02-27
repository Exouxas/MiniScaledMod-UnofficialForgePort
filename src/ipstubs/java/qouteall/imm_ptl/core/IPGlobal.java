package qouteall.imm_ptl.core;

import qouteall.q_misc_util.my_util.MyTaskList;

public class IPGlobal {
    public static boolean enableDepthClampForPortalRendering = false;

    public static final MyTaskList serverTaskList = new MyTaskList();
    public static final MyTaskList clientTaskList = new MyTaskList();
}
