package com.gathin.tvhome;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.provider.Settings;

/** The standard settings intent, or, when nothing usable answers it - or when the standard SETTINGS
 *  action would open the AOSP TV settings on firmware that also ships a vendor settings app - the
 *  first vendor settings entry from settings_fallback_components that is installed (Xiaomi China,
 *  TCL / FFALCON, Fire OS). */
final class SystemSettings {
    private SystemSettings() {}

    static Intent intent(Context context, String action) {
        PackageManager pm = context.getPackageManager();
        Intent standard = new Intent(action);
        ResolveInfo resolved = pm.resolveActivity(standard, PackageManager.MATCH_DEFAULT_ONLY);
        // Fire OS resolves the standard actions to a CTS compliance stub that opens nothing;
        // treat it as missing so the vendor fallback runs.
        //
        // China-market firmware (Xiaomi HyperOS on the MiTV-MTEQ0 is one) ships the AOSP TV
        // settings next to the vendor's own, and the standard SETTINGS action resolves to the
        // AOSP one - which has no Account & Security page, where the ADB switch lives on Xiaomi.
        // For that one action with that one answer, the vendor entry is the better target.
        boolean standardUsable = resolved != null && !resolved.activityInfo.name.contains("CTSDummy");
        if (standardUsable && !(Settings.ACTION_SETTINGS.equals(action)
                && "com.android.tv.settings".equals(resolved.activityInfo.packageName))) {
            return standard;
        }
        Intent vendor = vendorIntent(context);
        return vendor != null ? vendor : standard;
    }

    /** The first vendor settings entry from settings_fallback_components that is installed, or null
     *  when none is. China-market firmware keeps pages the AOSP TV settings has no row for -
     *  Xiaomi's Account & Security, where the ADB switch lives - in the vendor's own settings app;
     *  MainActivity's Settings menu offers it as "Native settings" while one is installed. */
    static Intent vendorIntent(Context context) {
        PackageManager pm = context.getPackageManager();
        for (String value : context.getResources().getStringArray(R.array.settings_fallback_components)) {
            ComponentName component = ComponentName.unflattenFromString(value);
            if (component == null) continue;
            // Explicit resolveActivity() always succeeds, so check the package exists instead.
            try {
                pm.getPackageInfo(component.getPackageName(), 0);
            } catch (PackageManager.NameNotFoundException e) {
                continue;
            }
            return new Intent().setComponent(component);
        }
        return null;
    }
}
