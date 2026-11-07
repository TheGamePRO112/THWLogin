package com.example.thwlogin

import android.view.View
import com.journeyapps.barcodescanner.CaptureActivity
import com.journeyapps.barcodescanner.DecoratedBarcodeView

class CustomCaptureActivity : CaptureActivity() {

    override fun initializeContent(): DecoratedBarcodeView {
        setContentView(R.layout.custom_barcode_scanner)
        val barcodeView = findViewById<DecoratedBarcodeView>(R.id.zxing_barcode_scanner)
        barcodeView.barcodeView.cameraSettings.apply {
            isAutoFocusEnabled = true
            isContinuousFocusEnabled = true
        }

        findViewById<View>(R.id.scanner_close_button)?.setOnClickListener {
            finish()
        }

        return barcodeView
    }
}
