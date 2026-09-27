package tech.layon.permadeath.api;

import tech.layon.permadeath.data.DateManager;

public class PermadeathAPI {
    public static long getDay() {
        return DateManager.getInstance().getDay();
    }
}

