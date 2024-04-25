package dev.bijon.cardio

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import io.card.payment.CardIOActivity
import io.card.payment.CreditCard
import io.card.payment.i18n.locales.LocalizedStringsList


class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_SCAN = 100
        private const val REQUEST_AUTOTEST = 200
    }

    private lateinit var mManualToggle: CheckBox
    private lateinit var mEnableExpiryToggle: CheckBox
    private lateinit var mScanExpiryToggle: CheckBox
    private lateinit var mCvvToggle: CheckBox
    private lateinit var mPostalCodeToggle: CheckBox
    private lateinit var mPostalCodeNumericOnlyToggle: CheckBox
    private lateinit var mCardholderNameToggle: CheckBox
    private lateinit var mSuppressManualToggle: CheckBox
    private lateinit var mSuppressConfirmationToggle: CheckBox
    private lateinit var mSuppressScanToggle: CheckBox
    private lateinit var mUseCardIOLogoToggle: CheckBox
    private lateinit var mShowPayPalActionBarIconToggle: CheckBox
    private lateinit var mKeepApplicationThemeToggle: CheckBox
    private lateinit var mLanguageSpinner: Spinner
    private lateinit var mUnblurEdit: EditText
    private lateinit var mResultLabel: TextView
    private lateinit var mResultImage: ImageView
    private lateinit var mResultCardTypeImage: ImageView

    private var autotestMode: Boolean = false
    private var numAutotestsPassed: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        mManualToggle = findViewById(R.id.force_manual)
        mEnableExpiryToggle = findViewById(R.id.gather_expiry)
        mScanExpiryToggle = findViewById(R.id.scan_expiry)
        mCvvToggle = findViewById(R.id.gather_cvv)
        mPostalCodeToggle = findViewById(R.id.gather_postal_code)
        mPostalCodeNumericOnlyToggle = findViewById(R.id.postal_code_numeric_only)
        mCardholderNameToggle = findViewById(R.id.gather_cardholder_name)
        mSuppressManualToggle = findViewById(R.id.suppress_manual)
        mSuppressConfirmationToggle = findViewById(R.id.suppress_confirmation)
        mSuppressScanToggle = findViewById(R.id.detect_only)
        mUseCardIOLogoToggle = findViewById(R.id.use_card_io_logo)
        mShowPayPalActionBarIconToggle = findViewById(R.id.show_paypal_action_bar_icon)
        mKeepApplicationThemeToggle = findViewById(R.id.keep_application_theme)
        mLanguageSpinner = findViewById(R.id.language)
        mUnblurEdit = findViewById(R.id.unblur)
        mResultLabel = findViewById(R.id.result)
        mResultImage = findViewById(R.id.result_image)
        mResultCardTypeImage = findViewById(R.id.result_card_type_image)

        // Set version info
        val version = findViewById<TextView>(R.id.version)
        version.text = "card.io library: ${CardIOActivity.sdkVersion()}\n" +
                "Build date: ${CardIOActivity.sdkBuildDate()}"

        // Setup listeners and initial configurations
        setScanExpiryEnabled()
        setupLanguageList()
    }


    private fun setScanExpiryEnabled() {
        mScanExpiryToggle.isEnabled = mEnableExpiryToggle.isChecked
    }

    fun onExpiryToggle(v: View) {
        setScanExpiryEnabled()
    }

    fun onScan(pressed: View) {
        val intent = Intent(this, CardIOActivity::class.java)
            .putExtra(CardIOActivity.EXTRA_NO_CAMERA, mManualToggle.isChecked)
            .putExtra(CardIOActivity.EXTRA_REQUIRE_EXPIRY, mEnableExpiryToggle.isChecked)
            .putExtra(CardIOActivity.EXTRA_SCAN_EXPIRY, mScanExpiryToggle.isChecked)
            .putExtra(CardIOActivity.EXTRA_REQUIRE_CVV, mCvvToggle.isChecked)
            .putExtra(CardIOActivity.EXTRA_REQUIRE_POSTAL_CODE, mPostalCodeToggle.isChecked)
            .putExtra(CardIOActivity.EXTRA_RESTRICT_POSTAL_CODE_TO_NUMERIC_ONLY, mPostalCodeNumericOnlyToggle.isChecked)
            .putExtra(CardIOActivity.EXTRA_REQUIRE_CARDHOLDER_NAME, mCardholderNameToggle.isChecked)
            .putExtra(CardIOActivity.EXTRA_SUPPRESS_MANUAL_ENTRY, mSuppressManualToggle.isChecked)
            .putExtra(CardIOActivity.EXTRA_USE_CARDIO_LOGO, mUseCardIOLogoToggle.isChecked)
            .putExtra(CardIOActivity.EXTRA_LANGUAGE_OR_LOCALE, mLanguageSpinner.selectedItem as String)
            .putExtra(CardIOActivity.EXTRA_USE_PAYPAL_ACTIONBAR_ICON, mShowPayPalActionBarIconToggle.isChecked)
            .putExtra(CardIOActivity.EXTRA_KEEP_APPLICATION_THEME, mKeepApplicationThemeToggle.isChecked)
            .putExtra(CardIOActivity.EXTRA_GUIDE_COLOR, Color.GREEN)
            .putExtra(CardIOActivity.EXTRA_SUPPRESS_CONFIRMATION, mSuppressConfirmationToggle.isChecked)
            .putExtra(CardIOActivity.EXTRA_SUPPRESS_SCAN, mSuppressScanToggle.isChecked)
            .putExtra(CardIOActivity.EXTRA_RETURN_CARD_IMAGE, true)
            intent.putExtra(CardIOActivity.EXTRA_UNBLUR_DIGITS, 16)

        try {
            val unblurDigits = mUnblurEdit.text.toString().toInt()
            intent.putExtra(CardIOActivity.EXTRA_UNBLUR_DIGITS, unblurDigits)
        } catch (ignored: NumberFormatException) {}

        startActivityForResult(intent, REQUEST_SCAN)
    }

    fun onAutotest(v: View?) {
        Log.i(TAG, "\n\n\n ============================== \n" +
                "successfully completed $numAutotestsPassed tests\n" +
                "beginning new test run\n")

        val intent = Intent(this, CardIOActivity::class.java)
            .putExtra(CardIOActivity.EXTRA_REQUIRE_EXPIRY, false)
            .putExtra(CardIOActivity.EXTRA_REQUIRE_CVV, false)
            .putExtra(CardIOActivity.EXTRA_REQUIRE_POSTAL_CODE, false)
            .putExtra(CardIOActivity.EXTRA_REQUIRE_CARDHOLDER_NAME, false)
//            .putExtra(CardIOActivity.EXTRA_UNBLUR_DIGITS, 8)
            .putExtra("debug_autoAcceptResult", true)

        startActivityForResult(intent, REQUEST_AUTOTEST)

        autotestMode = true
    }

    override fun onStop() {
        super.onStop()

        mResultLabel.text = ""
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.v(TAG, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> onActivityResult($requestCode, $resultCode, $data)")
        var outStr = ""
        var cardTypeImage: Bitmap? = null
        if ((requestCode == REQUEST_SCAN || requestCode == REQUEST_AUTOTEST) && data != null && data.hasExtra(
                CardIOActivity.EXTRA_SCAN_RESULT
            )
        ) {

            val result = data.getParcelableExtra<CreditCard>(CardIOActivity.EXTRA_SCAN_RESULT)
            if (result != null) {
                Log.d("Card", result.toString())
                outStr += "Card number: " + result.redactedCardNumber + "\n"
                outStr += "Card expiryMonth: " + result.expiryMonth + "\n"
                outStr += "Card expiryYear: " + result.expiryYear + "\n"
                val cardType = result.cardType
                cardTypeImage = cardType.imageBitmap(this)
                outStr += ("Card type: " + cardType.name + " cardType.getDisplayName(null)="
                        + cardType.getDisplayName(null) + "\n")
                if (mEnableExpiryToggle.isChecked) {
                    outStr += "Expiry: " + result.expiryMonth + "/" + result.expiryYear + "\n"
                }
                if (mCvvToggle.isChecked) {
                    outStr += "CVV: " + result.cvv + "\n"
                }
                if (mPostalCodeToggle.isChecked) {
                    outStr += "Postal Code: " + result.postalCode + "\n"
                }
                if (mCardholderNameToggle.isChecked) {
                    outStr += "Cardholder Name: " + result.cardholderName + "\n"
                }
            }
            if (autotestMode) {
                numAutotestsPassed++
                //Handler().postDelayed(Runnable { onAutotest(null) }, 500)
//                Handler().postDelayed({ onAutotest(null) }, 500)
            }
        } else if (resultCode == RESULT_CANCELED) {
            autotestMode = false
        }
        val card = CardIOActivity.getCapturedCardImage(data)
        mResultImage.setImageBitmap(card)
        mResultCardTypeImage.setImageBitmap(cardTypeImage)
        Log.i(TAG, "Set result: $outStr")
        mResultLabel.text = outStr
    }

    private fun setupLanguageList() {
        val languages: MutableList<String> = ArrayList()
        for (locale in LocalizedStringsList.ALL_LOCALES) {
            languages.add(locale.name)
        }
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line, languages
        )
        mLanguageSpinner.adapter = adapter
        mLanguageSpinner.setSelection(adapter.getPosition("en"))
    }

}