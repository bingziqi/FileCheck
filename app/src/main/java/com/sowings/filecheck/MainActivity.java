package com.sowings.filecheck;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import me.jahnen.libaums.core.UsbMassStorageDevice;
import me.jahnen.libaums.core.fs.FileSystem;
import me.jahnen.libaums.core.fs.UsbFile;
import me.jahnen.libaums.core.partition.Partition;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private String[] mUsbs = new String[]{"USB1", "USB2", "USB3"};
    private UsbMassStorageDevice chooseItem;

    private CustomLoadingDialog loadingDialog;

    private static final String ACTION_USB_PERMISSION = "com.sowings.USB_PERMISSION";
    private static final int REQUEST_READ_STORAGE_PERMISSION = 1;
    private UsbManager usbManager;
    private UsbMassStorageDevice checkDevice;

    private ListView showResult;
    private SimpleAdapter adapter;
    private List<Map<String, Object>> data = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //crateChooseDialog(mUsbs);
        showResult = findViewById(R.id.show_result);

        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        // 检查设备是否支持 USB Host 模式
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_USB_HOST)) {
            Toast.makeText(this, "设备不支持 USB OTG", Toast.LENGTH_LONG).show();
            return;
        }

        // 请求存储权限
        requestStoragePermission();

        // 注册广播接收器
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        registerReceiver(usbReceiver, filter);

        // 尝试访问连接的 USB 设备
        discoverUsbDevices();

        loadingDialog = new CustomLoadingDialog(this);

        adapter = new SimpleAdapter(this, data, R.layout.item_check_result, new String[]{"file_name", "result"}, new int[]{R.id.file_name, R.id.check_result});
        showResult.setAdapter(adapter);
    }

    private void startCheckFiles(List<UsbFile> files) throws InterruptedException {
        data.clear();
        SecureRandom secureRandom = new SecureRandom();
        runOnUiThread(() -> {
            loadingDialog.checkFiles();
            loadingDialog.setTile("文件检查中...");
            loadingDialog.show(files.size());
        });

        for (int i = 0; i < files.size(); i++) {
            Thread.sleep(100);  // 模拟文件检查操作
            int finalI = i;
            runOnUiThread(() -> {
                Map<String, Object> d = new HashMap<>();
                d.put("file_name", files.get(finalI).getAbsolutePath());
                d.put("result", "result" + (secureRandom.nextInt(5) + 1));
                data.add(d);
                loadingDialog.setFileName(d.get("file_name").toString());
                loadingDialog.updateProgress(finalI);
            });
        }
        runOnUiThread(() -> {
            loadingDialog.dismiss();
            adapter.notifyDataSetChanged();
        });
    }

    private void requestStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_STORAGE_PERMISSION);
        }
    }

    private void discoverUsbDevices() {
        UsbMassStorageDevice[] devices = UsbMassStorageDevice.getMassStorageDevices(this);
        if (devices.length == 0) {
            Toast.makeText(this, "未检测到 USB 设备", Toast.LENGTH_SHORT).show();
        } else {
            createChooseDialog(devices);
        }
    }

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            accessUsbDevice(device);
                        }
                    } else {
                        Toast.makeText(context, "USB 权限被拒绝", Toast.LENGTH_SHORT).show();
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                discoverUsbDevices();
            }
        }
    };

    private void accessUsbDevice(UsbDevice usbDevice) {
        UsbMassStorageDevice[] devices = UsbMassStorageDevice.getMassStorageDevices(this);
        for (UsbMassStorageDevice device : devices) {
            if (Objects.equals(device.getUsbDevice(), usbDevice)) {
                loadingDialog.loadFiles();
                checkDevice = device;
                try {
                    checkDevice.init();
                    Partition partition = checkDevice.getPartitions().get(0);
                    FileSystem fileSystem = partition.getFileSystem();
                    UsbFile root = fileSystem.getRootDirectory();

                    /*StringBuffer sb = new StringBuffer();

                    sb.append("容量: ").append(fileSystem.getCapacity()).append("\n");
                    sb.append("容量: ").append(fileSystem.getCapacity()).append("\n");*/
                    Log.d("USB_DEVICE", "容量: " + fileSystem.getCapacity());
                    Log.d("USB_DEVICE", "可用空间: " + fileSystem.getFreeSpace());

                    // 列出根目录的文件
                    /*for (UsbFile file : root.listFiles()) {
                        sb.append("文件: ").append(file.getName()).append("\n");
                        Log.d("USB_FILE", "文件: " + file.getName());
                    }*/
                    new Thread(() -> {
                        try {
                            List<UsbFile> files = FileUtils.getAllFiles(root);
                            Thread.sleep(3000);
                            startCheckFiles(files);
                        } catch (IOException | InterruptedException e) {
                            Log.d("USB_ERROR", "getAllFiles:" + e);
                            Toast.makeText(MainActivity.this, "获取文件失败！", Toast.LENGTH_SHORT).show();
                            loadingDialog.dismiss();
                            checkDevice.close();
                        }
                    }).start();
                } catch (Exception e) {
                    Log.d("USB_ERROR", "accessUsbDevice:" + e);
                    checkDevice.close();
                    Toast.makeText(this, "访问 USB 设备失败", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(usbReceiver);
    }

    // 处理权限请求的回调
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_READ_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                discoverUsbDevices();
            } else {
                Toast.makeText(this, "存储权限被拒绝", Toast.LENGTH_SHORT).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void createChooseDialog(UsbMassStorageDevice[] usbs) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String[] names = Arrays.stream(usbs).map(UsbMassStorageDevice::getUsbDevice).map(UsbDevice::getProductName).toArray(String[]::new);
        chooseItem = usbs[0];
        builder.setTitle("请选择要查验的USB设备").setSingleChoiceItems(names, 0, (dialog, which) -> {
            Log.d("----", "setSingleChoiceItems which = " + which);
            chooseItem = usbs[which];
        }).setPositiveButton("确定", (dialog, which) -> {
            // 请求权限
            PendingIntent pendingIntent;
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                flags |= PendingIntent.FLAG_MUTABLE;
            }
            Intent action = new Intent(ACTION_USB_PERMISSION);
            action.setPackage("com.sowings.filecheck");
            pendingIntent = PendingIntent.getBroadcast(MainActivity.this, 0, action, flags);
            usbManager.requestPermission(chooseItem.getUsbDevice(), pendingIntent);
//            Toast.makeText(MainActivity.this, chooseItem, Toast.LENGTH_LONG).show();
        }).setNegativeButton("取消", (dialog, which) -> {
            dialog.dismiss();
        }).setCancelable(false);
        builder.create().show();
    }
}