package com.example.egimhesabi.util

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient

object PrintHelper {
    
    fun printHtml(context: Context, htmlContent: String, jobName: String) {
        val webView = WebView(context)
        
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, urle: String) {
                createWebPrintJob(context, view, jobName)
            }
        }
        
        // Load the HTML content
        webView.loadDataWithBaseURL(null, htmlContent, "text/HTML", "UTF-8", null)
    }

    private fun createWebPrintJob(context: Context, webView: WebView, jobName: String) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            ?: return
            
        val printAdapter = webView.createPrintDocumentAdapter(jobName)
        
        printManager.print(
            jobName,
            printAdapter,
            PrintAttributes.Builder().build()
        )
    }
}

