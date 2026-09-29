package com.example.thwlogin

import com.journeyapps.barcodescanner.CaptureActivity
import com.journeyapps.barcodescanner.DecoratedBarcodeView

class CustomCaptureActivity : CaptureActivity() {
    override fun initializeContent(): DecoratedBarcodeView {
        val decoratedBarcodeView = super.initializeContent()
        decoratedBarcodeView.barcodeView.cameraSettings.apply {
            isAutoFocusEnabled = true
            isContinuousFocusEnabled = true
        }
        return decoratedBarcodeView
    }
}
