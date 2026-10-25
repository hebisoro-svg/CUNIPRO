package com.example.cunipro.util

import android.content.Context
import android.text.Html
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.example.cunipro.R

object TermsDialogHelper {

    fun showTermsDialog(context: Context) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_terms_of_use, null)
        val tvContent = view.findViewById<TextView>(R.id.tvTermsContent)
        val btnClose = view.findViewById<Button>(R.id.btnCloseTerms)

        val termsHtml = context.getString(R.string.terms_content)
        tvContent.text = Html.fromHtml(termsHtml, Html.FROM_HTML_MODE_LEGACY)

        val dialog = AlertDialog.Builder(context)
            .setView(view)
            .create()

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()

        val displayMetrics = context.resources.displayMetrics
        val width = (displayMetrics.widthPixels * 0.90).toInt()
        val height = (displayMetrics.heightPixels * 0.75).toInt()
        dialog.window?.setLayout(width, height)
    }
}
