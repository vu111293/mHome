package ai.api.sample.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.List;

import ai.api.sample.R;

/**
 * Created by ACER on 11/5/2017.
 */

public class WifiSelectionDialog extends Dialog implements View.OnClickListener {


    public interface IWifiSelection {
        void requestSend(String ssid, String pwd);
    }

    private ArrayAdapter<String> mAdapter;
    private Spinner mSpWifi;
    private EditText mEdPasswd;
    private ProgressBar mPrbar;
    private List<String> mWifis;
    private IWifiSelection mListener;


    public WifiSelectionDialog(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        setContentView(R.layout.dialog_wifi_selection);
        setupDialogSize();


        findViewById(R.id.btn_ok).setOnClickListener(this);
        findViewById(R.id.btn_cancel).setOnClickListener(this);
        mPrbar = (ProgressBar) findViewById(R.id.pr_loadwifi);
        mPrbar.setVisibility(View.VISIBLE);
        mSpWifi = (Spinner) findViewById(R.id.sp_wifi_name);
        mEdPasswd = (EditText) findViewById(R.id.ed_passwd);

        mWifis = new ArrayList<>();
        mAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_spinner_item, mWifis);
        mSpWifi.setAdapter(mAdapter);
    }

    public void setListener(IWifiSelection listener) {
        mListener = listener;
    }

    public void updateWifiList(List<String> list) {
        mWifis.clear();
        mWifis.addAll(list);
        mAdapter.notifyDataSetChanged();

        if (list.size() > 0) {
            mSpWifi.setSelection(0);
        }
        mPrbar.setVisibility(View.GONE);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_ok:
                boolean valid = true;
                String ssid = (String) mSpWifi.getSelectedItem();
                String pwd = mEdPasswd.getText().toString();

                if (ssid == null || ssid.isEmpty()) {
                    valid = false;
                }


                if (pwd == null || pwd.isEmpty()) {
                    mEdPasswd.setError("Please enter password");
                    valid = false;
                } else {
                    mEdPasswd.setError(null);
                }


                if (valid && mListener != null) {
                    mListener.requestSend(ssid, pwd);
                    dismiss();
                }
                break;

            case R.id.btn_cancel:
                dismiss();
                break;

            default:
                break;
        }
    }

    void setupDialogSize() {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        getWindow().setAttributes(lp);
    }
}
