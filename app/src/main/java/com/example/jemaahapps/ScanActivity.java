package com.example.jemaahapps;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.journeyapps.barcodescanner.CaptureActivity;

public class ScanActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Start the scan on activity start
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setPrompt("Scan a QR or Barcode");
        integrator.setCameraId(0);  // Use back camera
        integrator.setBeepEnabled(true);
        integrator.setOrientationLocked(false);
        integrator.setCaptureActivity(CaptureActivity.class);  // Custom capture activity (can be default)
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null) {
            if(result.getContents() == null) {
                // Scan canceled
                Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show();
                setResult(RESULT_CANCELED);
            } else {
                // Scan successful, return result
                Intent intent = new Intent();
                intent.putExtra("scan_result", result.getContents());
                setResult(RESULT_OK, intent);
            }
            finish(); // Close this activity and return to caller
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
