package lib.kalu.monitor;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public final class WatchdogActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_page);
        initApps();
    }

    private void initApps() {
        try {
            ViewGroup viewGroup = findViewById(R.id.main_apps);
            if (null == viewGroup)
                throw new Exception("viewGroup error: null");
            PackageManager packageManager = getPackageManager();
            List<ApplicationInfo> applicationInfos = packageManager.getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES);
            if (null == applicationInfos || applicationInfos.size() == 0)
                throw new Exception("appList error: " + applicationInfos);
            for (ApplicationInfo applicationInfo : applicationInfos) {
                if (null == applicationInfo)
                    continue;
                Drawable icon = applicationInfo.loadIcon(packageManager);
                String processName = applicationInfo.processName;
                String packageName = applicationInfo.packageName;
                String appName = applicationInfo.loadLabel(packageManager).toString();
                LogUtil.logE("WatchdogActivity => initApps => appName = " + appName + ", packageName = " + packageName);
                View inflate = LayoutInflater.from(this).inflate(R.layout.activity_page_item, null);
                ImageView imageView = inflate.findViewById(R.id.main_item_img);
                imageView.setImageDrawable(icon);
                TextView textView1 = inflate.findViewById(R.id.main_item_app);
                textView1.setText(appName);
                TextView textView2 = inflate.findViewById(R.id.main_item_package);
                textView2.setText(packageName);

                inflate.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Toast.makeText(view.getContext(), "appName = " + appName, Toast.LENGTH_SHORT).show();
                    }
                });

                float scale = getResources().getDisplayMetrics().density;
                int offset = (int) (140 / scale + 0.5f);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, offset);
                layoutParams.topMargin = offset / 10;
                inflate.setLayoutParams(layoutParams);
                viewGroup.addView(inflate);
            }
        } catch (Exception e) {
            LogUtil.logE("WatchdogActivity => initApps => " + e.getMessage(), e);
        }
    }
}
