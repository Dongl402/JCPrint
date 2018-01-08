package com.flowerplus.jcprint;

import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.dothantech.lpapi.LPAPI;
import com.dothantech.printer.IDzPrinter;

/**
 * Created by flowerplus-sdl on 2017/12/18.
 */

public class PrinterManager {
    private static PrinterManager sInstance;

    private LPAPI mApi;
    private PrintListener mListener;
    private Handler mHandler;
    private IDzPrinter.PrinterAddress mPreLinkPrinter;  //上次连接成功的打印机
    private LPAPI.Callback mCallback = new LPAPI.Callback() {
        @Override
        public void onProgressInfo(IDzPrinter.ProgressInfo progressInfo, Object o) {

        }

        @Override
        public void onStateChange(IDzPrinter.PrinterAddress arg0, IDzPrinter.PrinterState arg1) {
            if (mListener == null) return;

            final IDzPrinter.PrinterAddress printer = arg0;
            switch (arg1) {
                case Connected:
                case Connected2:
                    mPreLinkPrinter = printer;
                    // 打印机连接成功，发送通知，刷新界面提示
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mListener.onPrinterConnected(printer);
                        }
                    });
                    break;

                case Disconnected:
                    // 打印机连接失败、断开连接，发送通知，刷新界面提示
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mListener.onPrinterDisconnected();
                        }
                    });
                    break;

                default:
                    break;
            }
        }

        @Override
        public void onPrintProgress(IDzPrinter.PrinterAddress printerAddress, Object o, IDzPrinter.PrintProgress progress, Object o1) {
            if (mListener == null) return;

            switch (progress) {
                case Success:
                    // 打印标签成功，发送通知，刷新界面提示
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mListener.onPrintSuccess();
                        }
                    });
                    break;

                case Failed:
                    // 打印标签失败，发送通知，刷新界面提示
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mListener.onPrintFailed();
                        }
                    });
                    break;

                default:
                    break;
            }
        }

        @Override
        public void onPrinterDiscovery(IDzPrinter.PrinterAddress printerAddress, IDzPrinter.PrinterInfo printerInfo) {}
    };

    public synchronized static PrinterManager getInstance() {
        if (JCPrint.mContext == null) {
            throw new RuntimeException("JCPrint's context can't be null!");
        }

        if (sInstance == null) {
            sInstance = new PrinterManager();
            sInstance.mHandler = new Handler(JCPrint.mContext.getMainLooper());
        }
        return sInstance;
    }

    public PrinterManager() {
        if (mApi == null) {
            mApi = LPAPI.Factory.createInstance(mCallback);
        }
    }

    public LPAPI getApi() {
        return mApi;
    }

    public IDzPrinter.PrinterInfo getConnectedPrinterInfo(){
        return mApi.getPrinterInfo();
    }

    public boolean isConnected() {
        boolean connected = false;
        IDzPrinter.PrinterState ps = mApi.getPrinterState();
        if (ps == IDzPrinter.PrinterState.Connected || ps == IDzPrinter.PrinterState.Connected2) {
            return true;
        }
        return connected;
    }

    public void autoConnect() {
        getPrePrinter();

        if (mPreLinkPrinter != null) {
            connect(mPreLinkPrinter);
        }
    }

    public void connect(IDzPrinter.PrinterAddress printer) {
        if (mApi.openPrinterByAddress(printer)) {
            // 连接打印机的请求提交成功，刷新界面提示
            if (mListener != null ) mListener.onPrinterConnecting(printer);
        }else {
            //请求提交失败
            if (mListener != null ) mListener.onPrinterDisconnected();
        }
    }

    private void getPrePrinter() {
        String lastPrinterMac =  SPUtil.getString("prePrinterMac");
        String lastPrinterName = SPUtil.getString("prePrinterName");
        String lastPrinterType = SPUtil.getString("prePrinterType");
        IDzPrinter.AddressType lastAddressType = TextUtils.isEmpty(lastPrinterType) ? null : Enum.valueOf(IDzPrinter.AddressType.class, lastPrinterType);

        Log.d("get_printer", lastPrinterMac+" "+lastPrinterName+" "+lastPrinterType);

        if (lastPrinterMac == null || lastPrinterName == null || lastAddressType == null) {
            mPreLinkPrinter = null;
        } else {
            mPreLinkPrinter = new IDzPrinter.PrinterAddress(lastPrinterName, lastPrinterMac, lastAddressType);
        }
    }

    // 保存上次连接的打印机
    private void savePrinterInfo() {
        if (mPreLinkPrinter == null) return;
        // 保存相关信息

        SPUtil.putString("prePrinterMac", mPreLinkPrinter.macAddress);
        SPUtil.putString("prePrinterName", mPreLinkPrinter.shownName);
        SPUtil.putString("prePrinterType", mPreLinkPrinter.addressType.toString());

        Log.d("save_printer", mPreLinkPrinter.macAddress+" "+mPreLinkPrinter.shownName+" "+mPreLinkPrinter.addressType.toString());
    }

    public void removePrintListener() {
        mListener = null;
    }

    public void setPrintListener(PrintListener printListener) {
        this.mListener = printListener;
    }

    public void disConnect() {
        savePrinterInfo();
        setPrintListener(null);
        mApi.quit();
        sInstance = null;
    }

    public interface PrintListener {
        void onPrinterConnecting(IDzPrinter.PrinterAddress printer);
        void onPrinterConnected(IDzPrinter.PrinterAddress printer);
        void onPrinterDisconnected();
        void onPrintSuccess();
        void onPrintFailed();
    }
}
