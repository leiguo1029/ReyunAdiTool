package com.fear1ess.reyunaditool.cmd;

public class WSConnectCmd {
    public class ClientCmd {
        public final static int START_OPERATE_APP = 1;
        public final static int STOP_SERVICE = 2;
        public final static int REQ_APP_ADS_STATE = 3;
    }

    public class ServerCmd {
        public final static int NEW_APP_PUSH_MSG = 101;
        public final static int NEW_ADS_PUSH_MSG = 102;
    }
}
