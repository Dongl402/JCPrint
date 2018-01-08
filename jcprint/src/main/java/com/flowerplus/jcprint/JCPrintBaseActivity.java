package com.flowerplus.jcprint;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.dothantech.printer.IDzPrinter;

import java.util.List;

/**
 * Created by flowerplus-sdl on 2018/1/8.
 *
 * 精臣打印BaseActivity
 *
 * 使用方法：
 *      复制到项目中根据需求继承BaseActivity
 *
 */

public abstract class JCPrintBaseActivity extends AppCompatActivity implements PrinterManager.PrintListener {
    private List<IDzPrinter.PrinterAddress> mAllPrinter;
    private PrinterManager mPm;

    protected TextView mTvPrinterName;

    @Override
    public void onPrinterConnecting(IDzPrinter.PrinterAddress printer) {
        // 连接打印机请求成功提交，刷新界面提示
        String txt = printer.shownName;
        if (TextUtils.isEmpty(txt))
            txt = printer.macAddress;
        txt = "正在连接打印机" + '[' + txt + ']';

        showStateProgress(txt);

        mTvPrinterName.setText(txt);
    }

    // 连接打印机成功时操作
    @Override
    public void onPrinterConnected(IDzPrinter.PrinterAddress printer) {
        // 连接打印机成功时，刷新界面提示，保存相关信息
        hideStateProgress();
        successMsg("打印机连接成功");
        // 调用LPAPI对象的getPrinterInfo方法获得当前连接的打印机信息
        String txt = "";
        txt += printer.shownName + "[";
        txt += printer.macAddress + "]";
        mTvPrinterName.setText(txt+"  ▼");
    }

    // 连接打印机操作提交失败、打印机连接失败或连接断开时操作
    @Override
    public void onPrinterDisconnected() {
        // 连接打印机操作提交失败、打印机连接失败或连接断开时，刷新界面提示
        hideStateProgress();

        errorMsg("连接失败");
        mTvPrinterName.setText("未连接 ▼");
    }

    // 开始打印标签时操作
    private void onPrintStart() {
        // 开始打印标签时，刷新界面提示
        showStateProgress("打印中...");
    }

    // 标签打印成功时操作
    @Override
    public void onPrintSuccess() {
        // 标签打印成功时，刷新界面提示
        hideStateProgress();
        successMsg("打印成功");
    }

    // 打印请求失败或标签打印失败时操作
    @Override
    public void onPrintFailed() {
        // 打印请求失败或标签打印失败时，刷新界面提示
        hideStateProgress();
        successMsg("打印失败");
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        initPrinter();
    }

    protected void initPrinter() {
        mTvPrinterName = getPrinterNameTextView();

        mTvPrinterName.setText("未连接 ▼");

        mTvPrinterName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                choosePrinter();
            }
        });

        getPrintView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                print();
            }
        });

        mPm = PrinterManager.getInstance();
        mPm.setPrintListener(this);

        if (mPm.isConnected()) {
            IDzPrinter.PrinterInfo cpi = mPm.getConnectedPrinterInfo();
            mTvPrinterName.setText(cpi.deviceName+"["+cpi.deviceAddress+ "]  ▼");
        }else {
            mPm.autoConnect();
        }
    }

    protected void choosePrinter() {
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            errorMsg("当前设备不支持蓝牙");
            return;
        }
        if (!btAdapter.isEnabled()) {
            //直接打开蓝牙
            btAdapter.enable();
        }

        mAllPrinter = mPm.getApi().getAllPrinterAddresses(null);

        if (mAllPrinter == null || mAllPrinter.isEmpty()) {
            Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivity(intent);
            Toast.makeText(this, "请配对打印机,PIN码为0000", Toast.LENGTH_LONG).show();
            return;
        }

        int size = mAllPrinter.size();
        String[] allPrinter = new String[size];
        for (int i = 0; i < size; i++) {
            IDzPrinter.PrinterAddress pa = mAllPrinter.get(i);
            allPrinter[i] = pa.shownName+"["+pa.macAddress+"]";
        }
        new AlertDialog.Builder(this).setTitle("选择已绑定的设备").setItems(allPrinter, new DeviceListItemClicker()).show();
    }

    protected void print() {
        if (mPm.isConnected()) {
            if (printContent()) {
                onPrintStart();
            } else {
                onPrintFailed();
            }
        }
    }

    private class DeviceListItemClicker implements DialogInterface.OnClickListener{

        @Override
        public void onClick(DialogInterface dialog, int which) {
            IDzPrinter.PrinterAddress printer = mAllPrinter.get(which);
            if (printer != null) {
                // 连接选择的打印机
                mPm.connect(printer);
            }
        }
    }

    protected abstract TextView getPrinterNameTextView();

    protected abstract View getPrintView();

    protected abstract boolean printContent();

    protected abstract void successMsg(String msg);

    protected abstract void errorMsg(String msg);

    // 显示连接、打印的状态提示框
    protected abstract void showStateProgress(String msg);

    // 清除连接、打印的状态提示框
    protected abstract void hideStateProgress();

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPm!=null) mPm.removePrintListener();
    }
}
