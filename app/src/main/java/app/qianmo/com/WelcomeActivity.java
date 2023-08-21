package app.qianmo.com;

import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;

public final class WelcomeActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

//        Intent intent = new Intent(getApplicationContext(), WatchdogActivity.class);
//        startActivity(intent);
//        finish();
    }
}